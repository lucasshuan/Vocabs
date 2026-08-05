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
    fun `lote sobreposto limita concorrencia preserva sucesso parcial e contabiliza so sucesso`() = runBlocking {
        val ativas = AtomicInteger(0)
        val maximo = AtomicInteger(0)
        val chamadas = AtomicInteger(0)
        val repo = repository { 
            val emCurso = ativas.incrementAndGet()
            maximo.updateAndGet { maxOf(it, emCurso) }
            try {
                delay(60)
                if (chamadas.getAndIncrement() == 1) HttpStatusCode.InternalServerError to "{\"mensagem\":\"falhou\"}"
                else HttpStatusCode.OK to CARD_JSON
            } finally {
                ativas.decrementAndGet()
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

        val resultados = repo.generateCards(ids, concorrencia = 2)
        assertEquals(3, resultados.size)
        assertEquals(2, resultados.count { it })
        assertTrue(maximo.get() <= 2)
        assertTrue(maximo.get() >= 2)
        assertEquals(2, repo.observeAiUsage().first().used)
        assertEquals(2, repo.observeReady().first().size)
        assertEquals(EntryStatus.ERROR, repo.observeInbox().first().single().status)
    }

    @Test
    fun `excluir card preserva midia ate a ultima irma`() = runBlocking {
        val removidos = mutableListOf<String>()
        val repo = repository(remover = removidos::add)
        val capture = repo.captureMedia(CaptureFormat.PHOTO, "foto.jpg")
        val snippet = "green fence"
        repo.recordTranscription(capture, snippet)
        val ids = repo.confirmCapture(
            capture,
            snippet,
            listOf(selectTokens(snippet, 0)!!, selectTokens(snippet, 1)!!),
        )

        repo.excluir(ids.first())
        assertTrue(removidos.isEmpty())
        repo.excluir(ids.last())
        assertEquals(listOf("foto.jpg"), removidos)
    }

    @Test
    fun `activity e uso de ia viram com o month e day reais`() = runBlocking {
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
    fun `cada course ve so as proprias palavras e trocar de course troca a lista`() = runBlocking {
        val course = MutableStateFlow(LanguagePair("pt-BR", "en"))
        val repo = repository(course = course)

        val ingles = "on the fence"
        repo.captureText(ingles, listOf(selectTokens(ingles, 2)!!))

        course.value = LanguagePair("pt-BR", "de")
        val alemao = "der Zaun"
        repo.captureText(alemao, listOf(selectTokens(alemao, 1)!!))

        assertEquals(listOf("Zaun"), repo.observeInbox().first().map { it.target })
        course.value = LanguagePair("pt-BR", "en")
        assertEquals(listOf("fence"), repo.observeInbox().first().map { it.target })

        // A captura guarda o par em que nasceu, e não o par de agora.
        assertEquals(LanguagePair("pt-BR", "en"), repo.observeInbox().first().single().languagePair)
    }

    @Test
    fun `scope All ve os tres languages e scope Curso ve so o nomeado`() = runBlocking {
        val course = MutableStateFlow(LanguagePair("pt-BR", "en"))
        val repo = repository(course = course)

        val ingles = "on the fence"
        repo.captureText(ingles, listOf(selectTokens(ingles, 2)!!))
        course.value = LanguagePair("pt-BR", "de")
        val alemao = "der Zaun"
        repo.captureText(alemao, listOf(selectTokens(alemao, 1)!!))

        // Vocabulários, Pendentes e Você leem assim: tudo, sempre.
        assertEquals(
            setOf("fence", "Zaun"),
            repo.observeInbox(Scope.All).first().mapNotNull { it.target }.toSet(),
        )
        // "Seu progresso · inglês" lê assim, sem trocar o curso aberto.
        assertEquals(listOf("fence"), repo.observeInbox(Scope.Course("en")).first().map { it.target })
        assertEquals(LanguagePair("pt-BR", "de"), course.value)
    }

    @Test
    fun `o language escolhido na folha vence o course aberto`() = runBlocking {
        val course = MutableStateFlow(LanguagePair("pt-BR", "en"))
        val repo = repository(course = course)

        val espanhol = "se puso las botas"
        val id = repo.captureSnippet(espanhol, LanguagePair("pt-BR", "es"))

        val pendente = repo.observePendingCaptures(Scope.All).first().single()
        assertEquals(id, pendente.id)
        assertEquals("es", pendente.languagePair.target)
        // E some do curso aberto, que continua sendo o inglês.
        assertTrue(repo.observePendingCaptures().first().isEmpty())
    }

    @Test
    fun `trocar o language da capture so vale antes de virar fichas`() = runBlocking {
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
    fun `o resumo de cada course traz o badge da faixa`() = runBlocking {
        val repo = repository()
        val snippet = "verdant field"
        val id = repo.captureText(snippet, listOf(selectTokens(snippet, 0)!!)).single()
        assertTrue(repo.generateCard(id))

        // Ficha recém-pronta: em dia, com uma revisão agendada para depois.
        val emDia = repo.observeCourses().first().single()
        assertEquals(0, emDia.inQueue)
        assertTrue(emDia.nextInMillis!! > 0)
        assertEquals(CourseBadge.UpToDate, emDia.badge)

        now += 2 * 86_400_000L
        assertEquals(CourseBadge.Review(1), repo.observeCourses().first().single().badge)
    }

    @Test
    fun `a tela de confirmacao acompanha so os ids que acabaram de nascer`() = runBlocking {
        val repo = repository()
        val snippet = "The plan went haywire"
        val id = repo.captureSnippet(snippet)
        val ids = repo.confirmCapture(
            id,
            snippet,
            listOf(selectTokens(snippet, 3)!!, selectTokens(snippet, 1)!!),
        )

        assertEquals(ids.sorted(), repo.observeEntries(ids).first().map { it.id })
        // Lista vazia é o estado normal antes do argumento de rota chegar.
        assertTrue(repo.observeEntries(emptyList()).first().isEmpty())
    }

    @Test
    fun `a card e regerada no language em que nasceu, mesmo after trocar de course`() = runBlocking {
        val course = MutableStateFlow(LanguagePair("pt-BR", "de"))
        val pedidos = mutableListOf<String>()
        val repo = repository(course = course, aoPedir = pedidos::add)

        val snippet = "der Zaun"
        val id = repo.captureText(snippet, listOf(selectTokens(snippet, 1)!!)).single()
        course.value = LanguagePair("pt-BR", "en")
        assertTrue(repo.generateCard(id))

        assertTrue(pedidos.single().contains("\"targetLanguage\":\"de\""), pedidos.single())
    }

    @Test
    fun `quota conta o que ja saiu today e a fila do course aberto`() = runBlocking {
        val repo = repository()
        val snippet = "verdant field"
        val id = repo.captureText(snippet, listOf(selectTokens(snippet, 0)!!)).single()
        assertTrue(repo.generateCard(id))

        // Ficha recém-pronta está com 100 pontos: nada na fila, nada feito.
        assertEquals(DailyQuota(done = 0, inQueue = 0), repo.observeReviewSummary().first().quota)

        // Passado o intervalo, ela pede revisão e a quota do dia passa a ter 1.
        now += 2 * 86_400_000L
        assertEquals(DailyQuota(done = 0, inQueue = 1), repo.observeReviewSummary().first().quota)

        repo.recordAnswer(id, correct = true)
        assertEquals(DailyQuota(done = 1, inQueue = 0), repo.observeReviewSummary().first().quota)
        assertTrue(repo.observeReviewSummary().first().quota.met)
    }

    @Test
    fun `a row do tempo registra capture, card, answer e mudanca de level`() = runBlocking {
        val repo = repository()
        val snippet = "verdant field"
        val id = repo.captureText(snippet, listOf(selectTokens(snippet, 0)!!)).single()
        assertTrue(repo.generateCard(id))
        repo.recordAnswer(id, correct = true)

        val tipos = repo.observeEvents(84).first().map { it.type }
        assertTrue(EventType.CAPTURED in tipos)
        assertTrue(EventType.CARD_READY in tipos)
        assertTrue(EventType.CORRECT in tipos)

        val hit = repo.observeEvents(84).first().first { it.type == EventType.CORRECT }
        assertEquals("1", hit.detail)
        assertEquals("verdant", hit.target)

        // Três acertos depois ela cruza para familiar, e só aí um SUBIU_NIVEL sai.
        repeat(3) { repo.recordAnswer(id, correct = true) }
        val subidas = repo.observeEvents(84).first().filter { it.type == EventType.LEVELED_UP }
        assertEquals(listOf(MemoryLevel.MASTERED.name, MemoryLevel.FAMILIAR.name), subidas.map { it.detail })
    }

    private fun repository(
        remover: (String) -> Unit = {},
        course: Flow<LanguagePair> = flowOf(LanguagePair.DEFAULT),
        aoPedir: (String) -> Unit = {},
        answer: suspend () -> Pair<HttpStatusCode, String> = { HttpStatusCode.OK to CARD_JSON },
    ): VocabRepositoryImpl {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        VocabsDatabase.Schema.create(driver)
        val engine = MockEngine { requisicao ->
            aoPedir((requisicao.body as OutgoingContent.ByteArrayContent).bytes().decodeToString())
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
            removeFile = remover,
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
