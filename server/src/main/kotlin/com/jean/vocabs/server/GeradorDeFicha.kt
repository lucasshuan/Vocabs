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
    /**
     * Enquanto o app não escolher idiomas, toda ficha sai deste par. O parâmetro
     * existe para que a escolha, quando vier, seja uma linha no `gerar` e não
     * uma reescrita do prompt.
     */
    private val idiomas: ParDeIdiomas = ParDeIdiomas.PADRAO,
) {
    private val client: AnthropicClient by lazy(clientFactory)
    private val json = Json { ignoreUnknownKeys = true }
    private val promptSistema: String = promptDe(idiomas)

    fun gerar(pedido: GerarFichaRequest): FichaResponse {
        val params = MessageCreateParams.builder()
            .model(modelo)
            .maxTokens(2048L)
            .system(promptSistema)
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
                Snippet: ${pedido.trecho}
                Target: ${pedido.alvo}
                Type selected by the app: ${pedido.tipo.name}
                """.trimIndent()
            )
            .build()

        val resposta = client.messages().create(params)

        val texto = resposta.content()
            .firstNotNullOfOrNull { bloco -> bloco.text().orElse(null)?.text() }
            ?: error("A Claude API não devolveu nenhum bloco de texto.")

        val ficha = json.decodeFromString<FichaResponse>(texto)
        return aplicarDecisoesLocais(pedido, ficha)
    }

    // Configurável para dar para comparar modelos sem recompilar. O padrão é o
    // mais capaz para produzir definições contextualizadas e termos relacionados.
    private val modelo: String = Config["MODELO"] ?: MODELO_PADRAO

    /** Haiku 4.5 rejeita `effort` com 400; Opus e Sonnet aceitam. */
    private val suportaEffort: Boolean = !modelo.startsWith("claude-haiku")

    private companion object {
        const val MODELO_PADRAO = "claude-opus-5"

        /**
         * O prompt é a única coisa deste projeto escrita em inglês — todo o resto,
         * inclusive estes comentários, continua em português.
         *
         * Dois motivos. O idioma da instrução e o idioma da saída são
         * independentes: o modelo lê inglês e escreve a tradução em português
         * porque a instrução manda, não porque a instrução esteja em português.
         * E manter uma cópia traduzida do prompt por idioma nativo seria manter N
         * versões de uma prosa calibrada — o teste de PALAVRA vs EXPRESSAO é a
         * parte mais sutil da ficha, e traduzir é recalibrar sem querer.
         *
         * Os nomes dos campos e os valores do enum ficam como estão: são o
         * contrato com [FichaResponse], não texto para o modelo traduzir.
         */
        fun promptDe(idiomas: ParDeIdiomas): String {
            val nativo = idiomas.nativo
            val alvo = idiomas.alvo

            return """
                You help someone whose native language is $nativo learn ${alvo.nome}
                by living it — games, shows, books. They capture a snippet of
                ${alvo.nome} and mark the part of it that caught their attention.
                Your job is to build the study card for that target.

                `tipo` is supplied by the app. Copy it exactly; never classify or
                change it. It is PALAVRA for one selected token and EXPRESSAO for
                two or more selected tokens.

                The other fields:
                - `traducao`: in $nativo, the sense the target carries IN THIS
                  snippet — not the term's most common translation out of context.
                - `definicoes`: 1 or 2 definitions in $nativo, short and direct.
                - `exemplo`: ONE new sentence in ${alvo.nome} using the target in
                  the same sense. Do not repeat the original snippet.
                - `ipa`: the target's pronunciation written as
                  ${alvo.notacaoDePronuncia}. For expressions, transcribe the whole
                  expression. (The field is named `ipa` from when English was the
                  only target; the notation it should carry is the one above.)
                - `relacionadas`: 3 to 6 useful terms in ${alvo.nome} that are
                  semantically related to the target. Return only concise terms,
                  without definitions or numbering.
            """.trimIndent()
        }

        /**
         * Espelha [FichaResponse]. `additionalProperties: false` e todos os campos
         * em `required` são exigidos pelo structured outputs da API.
         */
        val SCHEMA: JsonValue = JsonValue.from(
            mapOf(
                "type" to "object",
                "additionalProperties" to false,
                "required" to listOf(
                    "tipo", "traducao", "definicoes", "exemplo", "ipa", "relacionadas"
                ),
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
                    "relacionadas" to mapOf(
                        "type" to "array",
                        "minItems" to 3,
                        "maxItems" to 6,
                        "items" to mapOf("type" to "string"),
                    ),
                ),
            )
        )
    }
}

/** A seleção no aparelho é a autoridade; a saída do modelo nunca a sobrescreve. */
internal fun aplicarDecisoesLocais(
    pedido: GerarFichaRequest,
    ficha: FichaResponse,
): FichaResponse = ficha.copy(
    tipo = pedido.tipo,
    relacionadas = ficha.relacionadas
        .map(String::trim)
        .filter(String::isNotEmpty)
        .distinct()
        .take(6),
)
