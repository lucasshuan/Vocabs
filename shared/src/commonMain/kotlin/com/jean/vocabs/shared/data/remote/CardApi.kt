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
 * A falha como código, para a tela escolher o texto no idioma de quem lê.
 *
 * [detail] é o que não dá para traduzir: a frase crua do provedor de IA, ou o
 * status HTTP. Existe para diagnóstico e nunca substitui o código.
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
     * [par] vem da entrada, e não de uma configuração do app: regerar uma ficha
     * antiga depois de trocar de curso tem que devolvê-la no idioma em que ela
     * nasceu.
     */
    suspend fun gerar(snippet: String, target: String, type: TargetType, languagePair: LanguagePair): CardResponse {
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
        } catch (cancelamento: CancellationException) {
            throw cancelamento
        } catch (falha: Exception) {
            // Servidor desligado, rede fora, DNS: para quem lê é tudo a mesma
            // coisa, e a frase do sistema operacional vem em inglês de qualquer
            // jeito. O texto original fica no detalhe, para diagnóstico.
            throw CardException(ErrorCode.UNREACHABLE, falha.message)
        }

        if (!answer.status.isSuccess()) {
            val body = runCatching { answer.body<ErrorResponse>() }.getOrNull()
                ?: throw CardException(ErrorCode.HTTP_ERROR, answer.status.value.toString())
            throw CardException(ErrorCode.of(body.code), body.detail)
        }

        return answer.body()
    }
}
