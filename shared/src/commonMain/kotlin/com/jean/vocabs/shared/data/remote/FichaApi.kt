package com.jean.vocabs.shared.data.remote

import com.jean.vocabs.contracts.ErroResponse
import com.jean.vocabs.contracts.FichaResponse
import com.jean.vocabs.contracts.GerarFichaRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess

/** Erro já traduzido para algo que dá para mostrar na tela. */
class FichaException(mensagem: String) : Exception(mensagem)

class FichaApi(
    private val baseUrl: String,
    private val token: String,
    private val client: HttpClient,
) {
    suspend fun gerar(trecho: String, alvo: String): FichaResponse {
        val resposta = client.post("$baseUrl/v1/ficha") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody(GerarFichaRequest(trecho = trecho, alvo = alvo))
        }

        if (!resposta.status.isSuccess()) {
            val mensagem = runCatching { resposta.body<ErroResponse>().mensagem }.getOrNull()
            throw FichaException(mensagem ?: "Servidor respondeu ${resposta.status.value}.")
        }

        return resposta.body()
    }
}
