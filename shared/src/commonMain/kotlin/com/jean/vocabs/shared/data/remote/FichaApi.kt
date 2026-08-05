package com.jean.vocabs.shared.data.remote

import com.jean.vocabs.contracts.CardResponse
import com.jean.vocabs.contracts.ErrorCode
import com.jean.vocabs.contracts.ErrorResponse
import com.jean.vocabs.contracts.GenerateCardRequest
import com.jean.vocabs.contracts.TargetType
import com.jean.vocabs.shared.domain.ParIdiomas
import kotlinx.coroutines.CancellationException
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess

/**
 * A falha como código, para a tela escolher o texto no idioma de quem lê.
 *
 * [detail] é o que não dá para traduzir: a frase crua do provedor de IA, ou o
 * status HTTP. Existe para diagnóstico e nunca substitui o código.
 */
class FichaException(
    val code: ErrorCode,
    val detail: String? = null,
) : Exception(detail ?: code.name)

class FichaApi(
    private val baseUrl: String,
    private val token: String,
    private val client: HttpClient,
) {
    /**
     * [par] vem da entrada, e não de uma configuração do app: regerar uma ficha
     * antiga depois de trocar de curso tem que devolvê-la no idioma em que ela
     * nasceu.
     */
    suspend fun gerar(trecho: String, alvo: String, tipo: TargetType, par: ParIdiomas): CardResponse {
        val resposta = try {
            client.post("$baseUrl/v1/ficha") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $token")
                setBody(
                    GenerateCardRequest(
                        snippet = trecho,
                        target = alvo,
                        type = tipo,
                        nativeLanguage = par.nativo,
                        targetLanguage = par.alvo,
                    ),
                )
            }
        } catch (cancelamento: CancellationException) {
            throw cancelamento
        } catch (falha: Exception) {
            // Servidor desligado, rede fora, DNS: para quem lê é tudo a mesma
            // coisa, e a frase do sistema operacional vem em inglês de qualquer
            // jeito. O texto original fica no detalhe, para diagnóstico.
            throw FichaException(ErrorCode.UNREACHABLE, falha.message)
        }

        if (!resposta.status.isSuccess()) {
            val corpo = runCatching { resposta.body<ErrorResponse>() }.getOrNull()
                ?: throw FichaException(ErrorCode.HTTP_ERROR, resposta.status.value.toString())
            throw FichaException(ErrorCode.of(corpo.code), corpo.detail)
        }

        return resposta.body()
    }
}
