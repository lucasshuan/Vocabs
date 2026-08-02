package com.jean.vocabs.server

import com.anthropic.client.AnthropicClient
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.core.JsonValue
import com.anthropic.models.messages.JsonOutputFormat
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.OutputConfig
import com.jean.vocabs.contracts.FichaResponse
import com.jean.vocabs.contracts.GerarFichaRequest
import kotlinx.serialization.json.Json

/**
 * Traduz uma captura crua (trecho + alvo) numa ficha completa, via Claude.
 *
 * O ponto crítico aqui é o structured output: o schema abaixo obriga a resposta
 * a vir no formato exato de [FichaResponse]. Sem isso, o modelo às vezes devolve
 * o JSON embrulhado em markdown ou com um comentário antes, e você acaba
 * escrevendo regex para extrair — que quebra no primeiro caso estranho.
 */
class GeradorDeFicha(
    // Preguiçoso de propósito: sem isto, a falta de ANTHROPIC_API_KEY derruba o
    // servidor no boot e você não consegue nem testar /health ou a autenticação.
    clientFactory: () -> AnthropicClient = {
        AnthropicOkHttpClient.builder()
            .apiKey(Config.obrigatorio("ANTHROPIC_API_KEY"))
            .build()
    },
) {
    private val client: AnthropicClient by lazy(clientFactory)
    private val json = Json { ignoreUnknownKeys = true }

    fun gerar(pedido: GerarFichaRequest): FichaResponse {
        val params = MessageCreateParams.builder()
            .model(modelo)
            .maxTokens(2048L)
            .system(PROMPT_SISTEMA)
            .outputConfig(
                OutputConfig.builder()
                    .apply {
                        // Nem todo modelo aceita `effort` — o Haiku 4.5 responde
                        // 400 "does not support the effort parameter". Mandar
                        // mesmo assim faz a ficha falhar inteira, então só envia
                        // para quem suporta.
                        if (suportaEffort) {
                            // Tarefa de extração curta e bem definida: não precisa
                            // de raciocínio profundo. É ajuste de custo/latência.
                            effort(OutputConfig.Effort.LOW)
                        }
                    }
                    .format(JsonOutputFormat.builder().schema(SCHEMA).build())
                    .build()
            )
            .addUserMessage(
                """
                Trecho: ${pedido.trecho}
                Alvo: ${pedido.alvo}
                """.trimIndent()
            )
            .build()

        val resposta = client.messages().create(params)

        val texto = resposta.content()
            .firstNotNullOfOrNull { bloco -> bloco.text().orElse(null)?.text() }
            ?: error("A Claude API não devolveu nenhum bloco de texto.")

        return json.decodeFromString<FichaResponse>(texto)
    }

    // Configurável para dar para comparar modelos sem recompilar. O padrão é o
    // mais capaz: a classificação PALAVRA vs EXPRESSAO é o julgamento mais sutil
    // da ficha, e errar isso contamina todos os outros campos.
    private val modelo: String = Config["MODELO"] ?: MODELO_PADRAO

    /** Haiku 4.5 rejeita `effort` com 400; Opus e Sonnet aceitam. */
    private val suportaEffort: Boolean = !modelo.startsWith("claude-haiku")

    private companion object {
        const val MODELO_PADRAO = "claude-opus-5"

        val PROMPT_SISTEMA = """
            Você ajuda um brasileiro que está aprendendo inglês "vivendo" o idioma
            (jogos, séries, livros). Ele captura um trecho em inglês e marca o que
            chamou atenção dentro dele. Sua tarefa é montar a ficha desse alvo.

            Classificação do campo `tipo` — use este teste prático:
            procurando SÓ o alvo isolado num dicionário, o sentido que ele tem
            DENTRO deste trecho aparece?
              - Sim  -> PALAVRA    (ex.: "ubiquitous", "meticulously")
              - Não  -> EXPRESSAO  (phrasal verb, idioma, collocation:
                                    "kick the bucket", "on the fence", "pull off")
            O que decide não é quantas palavras o alvo tem, e sim se o significado
            nasce da soma das partes. Analise sempre o alvo DENTRO do trecho, nunca
            isolado: a mesma palavra muda de sentido conforme o contexto.

            Regras dos demais campos:
            - `traducao`: em português do Brasil, o sentido que o alvo tem NESTE
              trecho — não a tradução mais comum da palavra fora de contexto.
            - `definicoes`: 1 ou 2 definições em português, curtas e diretas.
            - `exemplo`: UMA frase nova em inglês usando o alvo no mesmo sentido.
              Não repita o trecho original.
            - `ipa`: pronúncia do alvo em IPA, sem barras. Para expressões, a
              transcrição da expressão inteira.
        """.trimIndent()

        /**
         * Espelha [FichaResponse]. `additionalProperties: false` e todos os campos
         * em `required` são exigidos pelo structured outputs da API.
         */
        val SCHEMA: JsonValue = JsonValue.from(
            mapOf(
                "type" to "object",
                "additionalProperties" to false,
                "required" to listOf("tipo", "traducao", "definicoes", "exemplo", "ipa"),
                "properties" to mapOf(
                    "tipo" to mapOf(
                        "type" to "string",
                        "enum" to listOf("PALAVRA", "EXPRESSAO"),
                    ),
                    "traducao" to mapOf("type" to "string"),
                    "definicoes" to mapOf(
                        "type" to "array",
                        "items" to mapOf("type" to "string"),
                    ),
                    "exemplo" to mapOf("type" to "string"),
                    "ipa" to mapOf("type" to "string"),
                ),
            )
        )
    }
}
