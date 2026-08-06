package io.github.lucasshuan.vocabu.server

import io.github.lucasshuan.vocabu.contracts.ErrorCode
import io.github.lucasshuan.vocabu.contracts.ErrorResponse
import io.github.lucasshuan.vocabu.contracts.GenerateCardRequest
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
    val port = Config["PORT"]?.toIntOrNull() ?: 8080
    embeddedServer(Netty, port = port, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    val log = LoggerFactory.getLogger("vocabs")

    // Fails at boot: without a token every request 401s, discovered with the
    // phone already in hand.
    val expectedToken = Config.required("APP_TOKEN")

    val generator = CardGenerator()

    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
    install(CallLogging)
    install(StatusPages) {
        // Before the general handler, which would answer 503 and make the app retry.
        exception<UnknownLanguagePair> { call, causa ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse(ErrorCode.UNKNOWN_LANGUAGE_PAIR.name, causa.message))
        }
        exception<Throwable> { call, causa ->
            log.error("Falha ao gerar card", causa)
            // 503, not 500: the app reads it as "try again". Only the first line,
            // capped — an API error is otherwise a JSON dump on the phone.
            val detail = causa.message
                ?.lineSequence()
                ?.firstOrNull()
                ?.take(140)
                ?: causa::class.simpleName.orEmpty()
            call.respond(
                HttpStatusCode.ServiceUnavailable,
                ErrorResponse(ErrorCode.GENERATION_FAILED.name, detail),
            )
        }
    }

    routing {
        get("/health") { call.respondText("ok") }

        post("/v1/card") {
            if (call.request.headers[HttpHeaders.Authorization] != "Bearer $expectedToken") {
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse(ErrorCode.INVALID_TOKEN.name))
                return@post
            }

            val request = call.receive<GenerateCardRequest>()
            if (request.snippet.isBlank() || request.target.isBlank()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse(ErrorCode.MISSING_FIELDS.name),
                )
                return@post
            }

            // The Anthropic SDK is blocking; without this it holds a Netty thread.
            val card = withContext(Dispatchers.IO) { generator.gerar(request) }
            call.respond(card)
        }
    }
}
