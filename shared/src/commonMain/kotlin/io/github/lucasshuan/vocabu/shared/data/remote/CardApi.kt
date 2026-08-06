package io.github.lucasshuan.vocabu.shared.data.remote

import io.github.lucasshuan.vocabu.contracts.CardResponse
import io.github.lucasshuan.vocabu.contracts.ErrorCode
import io.github.lucasshuan.vocabu.contracts.ErrorResponse
import io.github.lucasshuan.vocabu.contracts.GenerateCardRequest
import io.github.lucasshuan.vocabu.contracts.TargetType
import io.github.lucasshuan.vocabu.shared.domain.LanguagePair
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException

/**
 * A code, so the screen picks the wording. [detail] is the provider's raw
 * sentence or the HTTP status — for diagnosis, never a replacement for the code.
 */
class CardException(
    val code: ErrorCode,
    val detail: String? = null,
) : Exception(detail ?: code.name)

class CardApi(
    private val baseUrl: String,
    private val token: String,
    private val client: HttpClient,
) {
    /**
     * The pair comes from the entry, not from app configuration: an old card
     * regenerated after a switch must come back in the language it was born in.
     */
    suspend fun generate(snippet: String, target: String, type: TargetType, languagePair: LanguagePair): CardResponse {
        val answer = try {
            client.post("$baseUrl/v1/card") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $token")
                setBody(
                    GenerateCardRequest(
                        snippet = snippet,
                        target = target,
                        type = type,
                        nativeLanguage = languagePair.native,
                        targetLanguage = languagePair.target,
                    ),
                )
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            // Server down, network out, DNS: one thing to a reader, and the OS
            // sentence arrives in English anyway. It survives in the detail.
            throw CardException(ErrorCode.UNREACHABLE, failure.message)
        }

        if (!answer.status.isSuccess()) {
            val body = runCatching { answer.body<ErrorResponse>() }.getOrNull()
                ?: throw CardException(ErrorCode.HTTP_ERROR, answer.status.value.toString())
            throw CardException(ErrorCode.of(body.code), body.detail)
        }

        return answer.body()
    }
}
