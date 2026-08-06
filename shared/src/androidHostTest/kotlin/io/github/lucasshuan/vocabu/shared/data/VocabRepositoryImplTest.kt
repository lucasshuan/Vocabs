package io.github.lucasshuan.vocabu.shared.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.github.lucasshuan.vocabu.shared.data.remote.CardApi
import io.github.lucasshuan.vocabu.shared.db.VocabsDatabase
import io.github.lucasshuan.vocabu.shared.domain.CaptureFormat
import io.github.lucasshuan.vocabu.shared.domain.CourseBadge
import io.github.lucasshuan.vocabu.shared.domain.DailyQuota
import io.github.lucasshuan.vocabu.shared.domain.EntryStatus
import io.github.lucasshuan.vocabu.shared.domain.EventType
import io.github.lucasshuan.vocabu.shared.domain.LanguagePair
import io.github.lucasshuan.vocabu.shared.domain.MemoryLevel
import io.github.lucasshuan.vocabu.shared.domain.Scope
import io.github.lucasshuan.vocabu.shared.domain.selectTokens
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
    fun `a batch caps concurrency, keeps partial success and counts only what succeeded`() = runBlocking {
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
    fun `deleting a card keeps the media until the last sibling goes`() = runBlocking {
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
    fun `activity and AI usage turn over on the real day and month`() = runBlocking {
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
    fun `each course sees only its own words, and switching swaps the list`() = runBlocking {
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

    /**
     * Matching on the whole pair meant that the moment the native side changed,
     * no stored card matched: totals read zero and lists came back empty, which
     * reads as deletion.
     */
    @Test
    fun `switching the native language keeps the course, its cards and its counts`() = runBlocking {
        val course = MutableStateFlow(LanguagePair("pt-BR", "en"))
        val repo = repository(course = course)

        val snippet = "on the fence"
        repo.captureText(snippet, listOf(selectTokens(snippet, 2)!!))
        repo.generateCard(repo.observeInbox().first().single().id)

        val before = repo.observeCourses().first().single()
        assertEquals("en", before.target)
        assertEquals(1, before.total)

        course.value = LanguagePair(native = "es", target = "en")

        val after = repo.observeCourses().first().single()
        assertEquals("en", after.target)
        assertEquals(1, after.total)
        // Scope.ActiveCourse reads through the same rule.
        assertEquals(listOf("fence"), repo.observeReady().first().map { it.target })
        // The card still records the pair it was born in.
        assertEquals("pt-BR", repo.observeReady().first().single().languagePair.native)
    }

    @Test
    fun `Scope All sees all three languages, Scope Course only the named one`() = runBlocking {
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
    fun `the language chosen on the sheet beats the active course`() = runBlocking {
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
    fun `a capture language change only applies before it becomes cards`() = runBlocking {
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
    fun `each course summary carries the strip badge`() = runBlocking {
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
    fun `the confirmation screen follows only the ids just created`() = runBlocking {
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
    fun `a card regenerates in the language it was born in, even after switching course`() = runBlocking {
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
    fun `the quota counts what left today plus the active course queue`() = runBlocking {
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
    fun `the timeline records capture, card, answer and level change`() = runBlocking {
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
