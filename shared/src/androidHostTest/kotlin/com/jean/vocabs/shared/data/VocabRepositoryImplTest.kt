package com.jean.vocabs.shared.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.jean.vocabs.shared.data.remote.CardApi
import com.jean.vocabs.shared.db.VocabsDatabase
import com.jean.vocabs.shared.domain.CaptureFormat
import com.jean.vocabs.shared.domain.CourseBadge
import com.jean.vocabs.shared.domain.DailyQuota
import com.jean.vocabs.shared.domain.EntryStatus
import com.jean.vocabs.shared.domain.EventType
import com.jean.vocabs.shared.domain.LanguagePair
import com.jean.vocabs.shared.domain.MemoryLevel
import com.jean.vocabs.shared.domain.Scope
import com.jean.vocabs.shared.domain.selectTokens
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

class VocabRepositoryImplTest {
    private var now = Instant.parse("2026-01-10T12:00:00Z").toEpochMilli()

    @Test
    fun `batch overlaid limita concurrency preserva success partial e contabiliza so success`() = runBlocking {
        val running = AtomicInteger(0)
        val maximum = AtomicInteger(0)
        val calls = AtomicInteger(0)
        val repo = repository { 
            val inProgress = running.incrementAndGet()
            maximum.updateAndGet { maxOf(it, inProgress) }
            try {
                delay(60)
                if (calls.getAndIncrement() == 1) HttpStatusCode.InternalServerError to "{\"message\":\"failed\"}"
                else HttpStatusCode.OK to CARD_JSON
            } finally {
                running.decrementAndGet()
            }
        }
        val snippet = "He is on the fence today"
        val ids = repo.captureText(
            snippet,
            listOf(
                selectTokens(snippet, 4)!!,
                selectTokens(snippet, 2, 4)!!,
                selectTokens(snippet, 0)!!,
            ),
        )

        val results = repo.generateCards(ids, concurrency = 2)
        assertEquals(3, results.size)
        assertEquals(2, results.count { it })
        assertTrue(maximum.get() <= 2)
        assertTrue(maximum.get() >= 2)
        assertEquals(2, repo.observeAiUsage().first().used)
        assertEquals(2, repo.observeReady().first().size)
        assertEquals(EntryStatus.ERROR, repo.observeInbox().first().single().status)
    }

    @Test
    fun `delete card preserva media ate a last sibling`() = runBlocking {
        val removed = mutableListOf<String>()
        val repo = repository(remove = removed::add)
        val capture = repo.captureMedia(CaptureFormat.PHOTO, "foto.jpg")
        val snippet = "green fence"
        repo.recordTranscription(capture, snippet)
        val ids = repo.confirmCapture(
            capture,
            snippet,
            listOf(selectTokens(snippet, 0)!!, selectTokens(snippet, 1)!!),
        )

        repo.delete(ids.first())
        assertTrue(removed.isEmpty())
        repo.delete(ids.last())
        assertEquals(listOf("foto.jpg"), removed)
    }

    @Test
    fun `activity e usage de ia viram com o month e day real`() = runBlocking {
        val repo = repository()
        val snippet = "verdant field"
        val id = repo.captureText(snippet, listOf(selectTokens(snippet, 0)!!)).single()
        assertTrue(repo.generateCard(id))
        repo.recordAnswer(id, correct = true)
        assertEquals(1, repo.observeActivity(84).first().single().reviews)
        assertEquals(1, repo.observeAiUsage().first().used)

        now = Instant.parse("2026-02-01T12:00:00Z").toEpochMilli()
        assertEquals(0, repo.observeAiUsage().first().used)
    }

    @Test
    fun `cada course ve so as own words e switch de course swap a list`() = runBlocking {
        val course = MutableStateFlow(LanguagePair("pt-BR", "en"))
        val repo = repository(course = course)

        val english = "on the fence"
        repo.captureText(english, listOf(selectTokens(english, 2)!!))

        course.value = LanguagePair("pt-BR", "de")
        val german = "der Zaun"
        repo.captureText(german, listOf(selectTokens(german, 1)!!))

        assertEquals(listOf("Zaun"), repo.observeInbox().first().map { it.target })
        course.value = LanguagePair("pt-BR", "en")
        assertEquals(listOf("fence"), repo.observeInbox().first().map { it.target })

        // The capture keeps the pair it was born in, not the current one.
        assertEquals(LanguagePair("pt-BR", "en"), repo.observeInbox().first().single().languagePair)
    }

    @Test
    fun `scope All ve os three languages e scope Curso ve so o named`() = runBlocking {
        val course = MutableStateFlow(LanguagePair("pt-BR", "en"))
        val repo = repository(course = course)

        val english = "on the fence"
        repo.captureText(english, listOf(selectTokens(english, 2)!!))
        course.value = LanguagePair("pt-BR", "de")
        val german = "der Zaun"
        repo.captureText(german, listOf(selectTokens(german, 1)!!))

        // Words, Pending and Profile read like this: everything, always.
        assertEquals(
            setOf("fence", "Zaun"),
            repo.observeInbox(Scope.All).first().mapNotNull { it.target }.toSet(),
        )
        // "Your progress" reads like this, without switching the active course.
        assertEquals(listOf("fence"), repo.observeInbox(Scope.Course("en")).first().map { it.target })
        assertEquals(LanguagePair("pt-BR", "de"), course.value)
    }

    @Test
    fun `o language chosen na sheet expires o course activePair`() = runBlocking {
        val course = MutableStateFlow(LanguagePair("pt-BR", "en"))
        val repo = repository(course = course)

        val spanish = "se puso las botas"
        val id = repo.captureSnippet(spanish, LanguagePair("pt-BR", "es"))

        val pending = repo.observePendingCaptures(Scope.All).first().single()
        assertEquals(id, pending.id)
        assertEquals("es", pending.languagePair.target)
        // And leaves the active course, which is still English.
        assertTrue(repo.observePendingCaptures().first().isEmpty())
    }

    @Test
    fun `switch o language da capture so vale antes de turn cards`() = runBlocking {
        val repo = repository()
        val snippet = "tant pis"
        val id = repo.captureSnippet(snippet)

        repo.changeCaptureLanguage(id, "fr")
        assertEquals("fr", repo.observeCaptureById(id).first()?.languagePair?.target)

        repo.confirmCapture(id, snippet, listOf(selectTokens(snippet, 0)!!))
        repo.changeCaptureLanguage(id, "de")
        assertEquals("fr", repo.observeCaptureById(id).first()?.languagePair?.target)
    }

    @Test
    fun `o summary de cada course traz o badge da strip`() = runBlocking {
        val repo = repository()
        val snippet = "verdant field"
        val id = repo.captureText(snippet, listOf(selectTokens(snippet, 0)!!)).single()
        assertTrue(repo.generateCard(id))

        // A just-ready card: up to date, with a review scheduled for later.
        val upToDate = repo.observeCourses().first().single()
        assertEquals(0, upToDate.inQueue)
        assertTrue(upToDate.nextInMillis!! > 0)
        assertEquals(CourseBadge.UpToDate, upToDate.badge)

        now += 2 * 86_400_000L
        assertEquals(CourseBadge.Review(1), repo.observeCourses().first().single().badge)
    }

    @Test
    fun `a screen de confirmation acompanha so os ids que ranOut de nascer`() = runBlocking {
        val repo = repository()
        val snippet = "The plan went haywire"
        val id = repo.captureSnippet(snippet)
        val ids = repo.confirmCapture(
            id,
            snippet,
            listOf(selectTokens(snippet, 3)!!, selectTokens(snippet, 1)!!),
        )

        assertEquals(ids.sorted(), repo.observeEntries(ids).first().map { it.id })
        // An empty list is the normal state before the route argument arrives.
        assertTrue(repo.observeEntries(emptyList()).first().isEmpty())
    }

    @Test
    fun `a card e regenerated no language em que wasBorn, mesmo after switch de course`() = runBlocking {
        val course = MutableStateFlow(LanguagePair("pt-BR", "de"))
        val requests = mutableListOf<String>()
        val repo = repository(course = course, onRequest = requests::add)

        val snippet = "der Zaun"
        val id = repo.captureText(snippet, listOf(selectTokens(snippet, 1)!!)).single()
        course.value = LanguagePair("pt-BR", "en")
        assertTrue(repo.generateCard(id))

        assertTrue(requests.single().contains("\"targetLanguage\":\"de\""), requests.single())
    }

    @Test
    fun `quota conta o que ja left today e a queue do course activePair`() = runBlocking {
        val repo = repository()
        val snippet = "verdant field"
        val id = repo.captureText(snippet, listOf(selectTokens(snippet, 0)!!)).single()
        assertTrue(repo.generateCard(id))

        // A just-ready card sits at 100 points: nothing queued, nothing done.
        assertEquals(DailyQuota(done = 0, inQueue = 0), repo.observeReviewSummary().first().quota)

        // Past the interval it becomes due and the day's quota goes to 1.
        now += 2 * 86_400_000L
        assertEquals(DailyQuota(done = 0, inQueue = 1), repo.observeReviewSummary().first().quota)

        repo.recordAnswer(id, correct = true)
        assertEquals(DailyQuota(done = 1, inQueue = 0), repo.observeReviewSummary().first().quota)
        assertTrue(repo.observeReviewSummary().first().quota.met)
    }

    @Test
    fun `a row do time record capture, card, answer e change de level`() = runBlocking {
        val repo = repository()
        val snippet = "verdant field"
        val id = repo.captureText(snippet, listOf(selectTokens(snippet, 0)!!)).single()
        assertTrue(repo.generateCard(id))
        repo.recordAnswer(id, correct = true)

        val types = repo.observeEvents(84).first().map { it.type }
        assertTrue(EventType.CAPTURED in types)
        assertTrue(EventType.CARD_READY in types)
        assertTrue(EventType.CORRECT in types)

        val hit = repo.observeEvents(84).first().first { it.type == EventType.CORRECT }
        assertEquals("1", hit.detail)
        assertEquals("verdant", hit.target)

        // Three hits later it crosses into familiar, and only then is a
        // LEVELED_UP recorded.
        repeat(3) { repo.recordAnswer(id, correct = true) }
        val rises = repo.observeEvents(84).first().filter { it.type == EventType.LEVELED_UP }
        assertEquals(listOf(MemoryLevel.MASTERED.name, MemoryLevel.FAMILIAR.name), rises.map { it.detail })
    }

    private fun repository(
        remove: (String) -> Unit = {},
        course: Flow<LanguagePair> = flowOf(LanguagePair.DEFAULT),
        onRequest: (String) -> Unit = {},
        answer: suspend () -> Pair<HttpStatusCode, String> = { HttpStatusCode.OK to CARD_JSON },
    ): VocabRepositoryImpl {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        VocabsDatabase.Schema.create(driver)
        val engine = MockEngine { request ->
            onRequest((request.body as OutgoingContent.ByteArrayContent).bytes().decodeToString())
            val (status, body) = answer()
            respond(
                body,
                status,
                headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val httpClient = HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        return VocabRepositoryImpl(
            db = VocabsDatabase(driver),
            api = CardApi("http://teste", "token", httpClient),
            io = Dispatchers.IO,
            now = { now },
            activeCourse = course,
            removeFile = remove,
        )
    }

    private companion object {
        const val CARD_JSON = """{
            "type":"WORD",
            "translation":"verdejante",
            "definitions":["Muito verde"],
            "example":"A verdant field appeared.",
            "pronunciation":"vɜːdənt",
            "related":["lush","green","leafy"]
        }"""
    }
}
