package com.jean.vocabs.server

import com.jean.vocabs.contracts.ErroResponse
import com.jean.vocabs.contracts.GerarFichaRequest
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

fun main() {
    val porta = Config["PORT"]?.toIntOrNull() ?: 8080
    embeddedServer(Netty, port = porta, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    val log = LoggerFactory.getLogger("vocabs")

    // Falhar no boot é melhor que falhar na primeira captura: sem token, todo
    // request seria 401 e você descobriria isso com o celular na mão.
    val tokenEsperado = Config.obrigatorio("APP_TOKEN")

    val gerador = GeradorDeFicha()

    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
    install(CallLogging)
    install(StatusPages) {
        // Antes do handler geral: um par de idiomas que não existe não melhora
        // se o app tentar de novo, e 503 faria o app tentar.
        exception<IdiomaDesconhecido> { call, causa ->
            call.respond(HttpStatusCode.BadRequest, ErroResponse(causa.message.orEmpty()))
        }
        exception<Throwable> { call, causa ->
            log.error("Falha ao gerar ficha", causa)
            // 503 e não 500: para o app isso é "tente de novo", não "desista".
            // A causa completa fica no log; para a tela vai só a primeira linha,
            // limitada — senão um erro da API vira um dump de JSON no celular.
            val detalhe = causa.message
                ?.lineSequence()
                ?.firstOrNull()
                ?.take(140)
                ?: causa::class.simpleName.orEmpty()
            call.respond(
                HttpStatusCode.ServiceUnavailable,
                ErroResponse("Falha ao gerar a ficha. $detalhe"),
            )
        }
    }

    routing {
        get("/health") { call.respondText("ok") }

        post("/v1/ficha") {
            if (call.request.headers[HttpHeaders.Authorization] != "Bearer $tokenEsperado") {
                call.respond(HttpStatusCode.Unauthorized, ErroResponse("Token inválido."))
                return@post
            }

            val pedido = call.receive<GerarFichaRequest>()
            if (pedido.trecho.isBlank() || pedido.alvo.isBlank()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErroResponse("trecho e alvo são obrigatórios."),
                )
                return@post
            }

            // O SDK da Anthropic é bloqueante; sem isto ele trava uma thread do Netty.
            val ficha = withContext(Dispatchers.IO) { gerador.gerar(pedido) }
            call.respond(ficha)
        }
    }
}
