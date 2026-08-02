package com.jean.vocabs.shared.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.jean.vocabs.shared.data.remote.FichaApi
import com.jean.vocabs.shared.db.VocabsDatabase
import com.jean.vocabs.shared.domain.FormatoCaptura
import com.jean.vocabs.shared.domain.StatusEntrada
import com.jean.vocabs.shared.domain.selecionarTokens
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
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
import kotlinx.coroutines.flow.first
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
        assertEquals(StatusEntrada.ERRO, repo.observarInbox().first().single().status)
    }

    @Test
    fun `excluir ficha preserva midia ate a ultima irma`() = runBlocking {
        val removidos = mutableListOf<String>()
        val repo = repositorio(remover = removidos::add)
        val captura = repo.capturarMidia(FormatoCaptura.FOTO, "foto.jpg")
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

    private fun repositorio(
        remover: (String) -> Unit = {},
        resposta: suspend () -> Pair<HttpStatusCode, String> = { HttpStatusCode.OK to FICHA },
    ): VocabRepositoryImpl {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        VocabsDatabase.Schema.create(driver)
        val engine = MockEngine {
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
            removerArquivo = remover,
        )
    }

    private companion object {
        const val FICHA = """{
            "tipo":"PALAVRA",
            "traducao":"verdejante",
            "definicoes":["Muito verde"],
            "exemplo":"A verdant field appeared.",
            "ipa":"/vɜːdənt/",
            "relacionadas":["lush","green","leafy"]
        }"""
    }
}
