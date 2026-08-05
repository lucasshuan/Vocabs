package com.jean.vocabs.shared.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.jean.vocabs.shared.data.remote.FichaApi
import com.jean.vocabs.shared.db.VocabsDatabase
import com.jean.vocabs.shared.domain.Escopo
import com.jean.vocabs.shared.domain.CaptureFormat
import com.jean.vocabs.shared.domain.MemoryLevel
import com.jean.vocabs.shared.domain.ParIdiomas
import com.jean.vocabs.shared.domain.QuotaDoDia
import com.jean.vocabs.shared.domain.SeloDeCurso
import com.jean.vocabs.shared.domain.EntryStatus
import com.jean.vocabs.shared.domain.EventType
import com.jean.vocabs.shared.domain.selecionarTokens
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
    private var agora = Instant.parse("2026-01-10T12:00:00Z").toEpochMilli()

    @Test
    fun `lote sobreposto limita concorrencia preserva sucesso parcial e contabiliza so sucesso`() = runBlocking {
        val ativas = AtomicInteger(0)
        val maximo = AtomicInteger(0)
        val chamadas = AtomicInteger(0)
        val repo = repositorio { 
            val emCurso = ativas.incrementAndGet()
            maximo.updateAndGet { maxOf(it, emCurso) }
            try {
                delay(60)
                if (chamadas.getAndIncrement() == 1) HttpStatusCode.InternalServerError to "{\"mensagem\":\"falhou\"}"
                else HttpStatusCode.OK to FICHA
            } finally {
                ativas.decrementAndGet()
            }
        }
        val trecho = "He is on the fence today"
        val ids = repo.capturarTexto(
            trecho,
            listOf(
                selecionarTokens(trecho, 4)!!,
                selecionarTokens(trecho, 2, 4)!!,
                selecionarTokens(trecho, 0)!!,
            ),
        )

        val resultados = repo.gerarFichas(ids, concorrencia = 2)
        assertEquals(3, resultados.size)
        assertEquals(2, resultados.count { it })
        assertTrue(maximo.get() <= 2)
        assertTrue(maximo.get() >= 2)
        assertEquals(2, repo.observarUsoIa().first().usadas)
        assertEquals(2, repo.observarProntas().first().size)
        assertEquals(EntryStatus.ERROR, repo.observarInbox().first().single().status)
    }

    @Test
    fun `excluir ficha preserva midia ate a ultima irma`() = runBlocking {
        val removidos = mutableListOf<String>()
        val repo = repositorio(remover = removidos::add)
        val captura = repo.capturarMidia(CaptureFormat.PHOTO, "foto.jpg")
        val trecho = "green fence"
        repo.registrarTranscricao(captura, trecho)
        val ids = repo.confirmarCaptura(
            captura,
            trecho,
            listOf(selecionarTokens(trecho, 0)!!, selecionarTokens(trecho, 1)!!),
        )

        repo.excluir(ids.first())
        assertTrue(removidos.isEmpty())
        repo.excluir(ids.last())
        assertEquals(listOf("foto.jpg"), removidos)
    }

    @Test
    fun `atividade e uso de ia viram com o mes e dia reais`() = runBlocking {
        val repo = repositorio()
        val trecho = "verdant field"
        val id = repo.capturarTexto(trecho, listOf(selecionarTokens(trecho, 0)!!)).single()
        assertTrue(repo.gerarFicha(id))
        repo.registrarResposta(id, acertou = true)
        assertEquals(1, repo.observarAtividade(84).first().single().revisoes)
        assertEquals(1, repo.observarUsoIa().first().usadas)

        agora = Instant.parse("2026-02-01T12:00:00Z").toEpochMilli()
        assertEquals(0, repo.observarUsoIa().first().usadas)
    }

    @Test
    fun `cada curso ve so as proprias palavras e trocar de curso troca a lista`() = runBlocking {
        val curso = MutableStateFlow(ParIdiomas("pt-BR", "en"))
        val repo = repositorio(curso = curso)

        val ingles = "on the fence"
        repo.capturarTexto(ingles, listOf(selecionarTokens(ingles, 2)!!))

        curso.value = ParIdiomas("pt-BR", "de")
        val alemao = "der Zaun"
        repo.capturarTexto(alemao, listOf(selecionarTokens(alemao, 1)!!))

        assertEquals(listOf("Zaun"), repo.observarInbox().first().map { it.alvo })
        curso.value = ParIdiomas("pt-BR", "en")
        assertEquals(listOf("fence"), repo.observarInbox().first().map { it.alvo })

        // A captura guarda o par em que nasceu, e não o par de agora.
        assertEquals(ParIdiomas("pt-BR", "en"), repo.observarInbox().first().single().par)
    }

    @Test
    fun `escopo Todos ve os tres idiomas e escopo Curso ve so o nomeado`() = runBlocking {
        val curso = MutableStateFlow(ParIdiomas("pt-BR", "en"))
        val repo = repositorio(curso = curso)

        val ingles = "on the fence"
        repo.capturarTexto(ingles, listOf(selecionarTokens(ingles, 2)!!))
        curso.value = ParIdiomas("pt-BR", "de")
        val alemao = "der Zaun"
        repo.capturarTexto(alemao, listOf(selecionarTokens(alemao, 1)!!))

        // Vocabulários, Pendentes e Você leem assim: tudo, sempre.
        assertEquals(
            setOf("fence", "Zaun"),
            repo.observarInbox(Escopo.Todos).first().mapNotNull { it.alvo }.toSet(),
        )
        // "Seu progresso · inglês" lê assim, sem trocar o curso aberto.
        assertEquals(listOf("fence"), repo.observarInbox(Escopo.Curso("en")).first().map { it.alvo })
        assertEquals(ParIdiomas("pt-BR", "de"), curso.value)
    }

    @Test
    fun `o idioma escolhido na folha vence o curso aberto`() = runBlocking {
        val curso = MutableStateFlow(ParIdiomas("pt-BR", "en"))
        val repo = repositorio(curso = curso)

        val espanhol = "se puso las botas"
        val id = repo.capturarTrecho(espanhol, ParIdiomas("pt-BR", "es"))

        val pendente = repo.observarCapturasPendentes(Escopo.Todos).first().single()
        assertEquals(id, pendente.id)
        assertEquals("es", pendente.par.alvo)
        // E some do curso aberto, que continua sendo o inglês.
        assertTrue(repo.observarCapturasPendentes().first().isEmpty())
    }

    @Test
    fun `trocar o idioma da captura so vale antes de virar fichas`() = runBlocking {
        val repo = repositorio()
        val trecho = "tant pis"
        val id = repo.capturarTrecho(trecho)

        repo.alterarIdiomaDaCaptura(id, "fr")
        assertEquals("fr", repo.observarCapturaPorId(id).first()?.par?.alvo)

        repo.confirmarCaptura(id, trecho, listOf(selecionarTokens(trecho, 0)!!))
        repo.alterarIdiomaDaCaptura(id, "de")
        assertEquals("fr", repo.observarCapturaPorId(id).first()?.par?.alvo)
    }

    @Test
    fun `o resumo de cada curso traz o selo da faixa`() = runBlocking {
        val repo = repositorio()
        val trecho = "verdant field"
        val id = repo.capturarTexto(trecho, listOf(selecionarTokens(trecho, 0)!!)).single()
        assertTrue(repo.gerarFicha(id))

        // Ficha recém-pronta: em dia, com uma revisão agendada para depois.
        val emDia = repo.observarCursos().first().single()
        assertEquals(0, emDia.naFila)
        assertTrue(emDia.proximaEmMillis!! > 0)
        assertEquals(SeloDeCurso.EmDia, emDia.selo)

        agora += 2 * 86_400_000L
        assertEquals(SeloDeCurso.Revisar(1), repo.observarCursos().first().single().selo)
    }

    @Test
    fun `a tela de confirmacao acompanha so os ids que acabaram de nascer`() = runBlocking {
        val repo = repositorio()
        val trecho = "The plan went haywire"
        val id = repo.capturarTrecho(trecho)
        val ids = repo.confirmarCaptura(
            id,
            trecho,
            listOf(selecionarTokens(trecho, 3)!!, selecionarTokens(trecho, 1)!!),
        )

        assertEquals(ids.sorted(), repo.observarEntradas(ids).first().map { it.id })
        // Lista vazia é o estado normal antes do argumento de rota chegar.
        assertTrue(repo.observarEntradas(emptyList()).first().isEmpty())
    }

    @Test
    fun `a ficha e regerada no idioma em que nasceu, mesmo apos trocar de curso`() = runBlocking {
        val curso = MutableStateFlow(ParIdiomas("pt-BR", "de"))
        val pedidos = mutableListOf<String>()
        val repo = repositorio(curso = curso, aoPedir = pedidos::add)

        val trecho = "der Zaun"
        val id = repo.capturarTexto(trecho, listOf(selecionarTokens(trecho, 1)!!)).single()
        curso.value = ParIdiomas("pt-BR", "en")
        assertTrue(repo.gerarFicha(id))

        assertTrue(pedidos.single().contains("\"targetLanguage\":\"de\""), pedidos.single())
    }

    @Test
    fun `quota conta o que ja saiu hoje e a fila do curso aberto`() = runBlocking {
        val repo = repositorio()
        val trecho = "verdant field"
        val id = repo.capturarTexto(trecho, listOf(selecionarTokens(trecho, 0)!!)).single()
        assertTrue(repo.gerarFicha(id))

        // Ficha recém-pronta está com 100 pontos: nada na fila, nada feito.
        assertEquals(QuotaDoDia(feita = 0, naFila = 0), repo.observarResumoDeRevisao().first().quota)

        // Passado o intervalo, ela pede revisão e a quota do dia passa a ter 1.
        agora += 2 * 86_400_000L
        assertEquals(QuotaDoDia(feita = 0, naFila = 1), repo.observarResumoDeRevisao().first().quota)

        repo.registrarResposta(id, acertou = true)
        assertEquals(QuotaDoDia(feita = 1, naFila = 0), repo.observarResumoDeRevisao().first().quota)
        assertTrue(repo.observarResumoDeRevisao().first().quota.batida)
    }

    @Test
    fun `a linha do tempo registra captura, ficha, resposta e mudanca de nivel`() = runBlocking {
        val repo = repositorio()
        val trecho = "verdant field"
        val id = repo.capturarTexto(trecho, listOf(selecionarTokens(trecho, 0)!!)).single()
        assertTrue(repo.gerarFicha(id))
        repo.registrarResposta(id, acertou = true)

        val tipos = repo.observarEventos(84).first().map { it.tipo }
        assertTrue(EventType.CAPTURED in tipos)
        assertTrue(EventType.CARD_READY in tipos)
        assertTrue(EventType.CORRECT in tipos)

        val acerto = repo.observarEventos(84).first().first { it.tipo == EventType.CORRECT }
        assertEquals("1", acerto.detalhe)
        assertEquals("verdant", acerto.alvo)

        // Três acertos depois ela cruza para familiar, e só aí um SUBIU_NIVEL sai.
        repeat(3) { repo.registrarResposta(id, acertou = true) }
        val subidas = repo.observarEventos(84).first().filter { it.tipo == EventType.LEVELED_UP }
        assertEquals(listOf(MemoryLevel.MASTERED.name, MemoryLevel.FAMILIAR.name), subidas.map { it.detalhe })
    }

    private fun repositorio(
        remover: (String) -> Unit = {},
        curso: Flow<ParIdiomas> = flowOf(ParIdiomas.PADRAO),
        aoPedir: (String) -> Unit = {},
        resposta: suspend () -> Pair<HttpStatusCode, String> = { HttpStatusCode.OK to FICHA },
    ): VocabRepositoryImpl {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        VocabsDatabase.Schema.create(driver)
        val engine = MockEngine { requisicao ->
            aoPedir((requisicao.body as OutgoingContent.ByteArrayContent).bytes().decodeToString())
            val (status, corpo) = resposta()
            respond(
                corpo,
                status,
                headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val cliente = HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        return VocabRepositoryImpl(
            db = VocabsDatabase(driver),
            api = FichaApi("http://teste", "token", cliente),
            io = Dispatchers.IO,
            agora = { agora },
            cursoAtivo = curso,
            removerArquivo = remover,
        )
    }

    private companion object {
        const val FICHA = """{
            "type":"WORD",
            "translation":"verdejante",
            "definitions":["Muito verde"],
            "example":"A verdant field appeared.",
            "pronunciation":"vɜːdənt",
            "related":["lush","green","leafy"]
        }"""
    }
}
