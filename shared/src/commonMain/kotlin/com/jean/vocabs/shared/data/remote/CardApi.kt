package com.jean.vocabs.shared.data.remote

import com.jean.vocabs.contracts.CardResponse
import com.jean.vocabs.contracts.ErrorCode
import com.jean.vocabs.contracts.ErrorResponse
import com.jean.vocabs.contracts.GenerateCardRequest
import com.jean.vocabs.contracts.TargetType
import com.jean.vocabs.shared.domain.LanguagePair
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
 * The failure as a code, so the screen picks the wording in the reader's
 * language. [detail] is what cannot be translated — the AI provider's raw
 * sentence, or the HTTP status. It is for diagnosis and never replaces the code.
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
     * [pair] comes from the entry, not from app configuration: regenerating an old
     * card after switching course has to return it in the language it was born in.
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
            // Server down, network out, DNS: to a reader these are the same thing,
            // and the operating system's sentence arrives in English anyway. The
            // original text stays in the detail, for diagnosis.
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
