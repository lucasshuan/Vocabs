package com.jean.vocabs.server

import com.anthropic.client.AnthropicClient
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.core.JsonValue
import com.anthropic.models.messages.JsonOutputFormat
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.OutputConfig
import com.jean.vocabs.contracts.CardResponse
import com.jean.vocabs.contracts.GenerateCardRequest
import java.util.concurrent.ConcurrentHashMap
import kotlinx.serialization.json.Json

/**
 * Par de idiomas que o catálogo não conhece.
 *
 * Vira 400, e não 503: repetir o mesmo pedido nunca vai dar certo, e o app
 * precisa saber a diferença entre "tente de novo" e "isto não existe".
 */
class UnknownLanguagePair(native: String, target: String) :
    IllegalArgumentException("Par de languages desconhecido: $native → $target.")

/**
 * Traduz uma captura crua (trecho + alvo) numa ficha completa, via Claude.
 *
 * O ponto crítico aqui é o structured output: o schema abaixo obriga a resposta
 * a vir no formato exato de [CardResponse]. Sem isso, o modelo às vezes devolve
 * o JSON embrulhado em markdown ou com um comentário antes, e você acaba
 * escrevendo regex para extrair — que quebra no primeiro caso estranho.
 */
class CardGenerator(
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

    /**
     * O prompt de cada par, montado uma vez só.
     *
     * O texto do sistema é o pedaço mais longo e mais repetido de toda chamada, e
     * remontá-lo por requisição produziria strings diferentes byte a byte só por
     * azar de formatação — o que basta para perder o cache de prompt do outro
     * lado. São poucos pares por instalação; guardar todos custa nada.
     */
    private val prompts = ConcurrentHashMap<LanguagePairSpec, String>()

    fun gerar(request: GenerateCardRequest): CardResponse {
        val languages = LanguagePairSpec.de(request.nativeLanguage, request.targetLanguage)
            ?: throw UnknownLanguagePair(request.nativeLanguage, request.targetLanguage)

        val params = MessageCreateParams.builder()
            .model(model)
            .maxTokens(2048L)
            .system(prompts.computeIfAbsent(languages, ::promptFor))
            .outputConfig(
                OutputConfig.builder()
                    .apply {
                        // Nem todo modelo aceita `effort` — o Haiku 4.5 responde
                        // 400 "does not support the effort parameter". Mandar
                        // mesmo assim faz a ficha falhar inteira, então só envia
                        // para quem suporta.
                        if (supportsEffort) {
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
                Snippet: ${request.snippet}
                Target: ${request.target}
                Type selected by the app: ${request.type.name}
                """.trimIndent()
            )
            .build()

        val answer = client.messages().create(params)

        val text = answer.content()
            .firstNotNullOfOrNull { bloco -> bloco.text().orElse(null)?.text() }
            ?: error("A Claude API não devolveu nenhum bloco de text.")

        val card = json.decodeFromString<CardResponse>(text)
        return applyLocalDecisions(request, card)
    }

    // Configurável para dar para comparar modelos sem recompilar. O padrão é o
    // mais capaz para produzir definições contextualizadas e termos relacionados.
    private val model: String = Config["MODELO"] ?: MODELO_PADRAO

    /** Haiku 4.5 rejeita `effort` com 400; Opus e Sonnet aceitam. */
    private val supportsEffort: Boolean = !model.startsWith("claude-haiku")

    /** Interno, e não privado, para o teste conferir que o prompt cita os dois languages. */
    internal companion object {
        const val MODELO_PADRAO = "claude-opus-5"

        /**
         * O prompt é a única coisa deste projeto escrita em inglês — todo o resto,
         * inclusive estes comentários, continua em português.
         *
         * Dois motivos. O idioma da instrução e o idioma da saída são
         * independentes: o modelo lê inglês e escreve a tradução em português
         * porque a instrução manda, não porque a instrução esteja em português.
         * E manter uma cópia traduzida do prompt por idioma nativo seria manter N
         * versões de uma prosa calibrada — o teste de WORD vs PHRASE é a
         * parte mais sutil da ficha, e traduzir é recalibrar sem querer.
         *
         * Os nomes dos campos e os valores do enum ficam como estão: são o
         * contrato com [CardResponse], não texto para o modelo traduzir.
         */
        fun promptFor(languages: LanguagePairSpec): String {
            val native = languages.native.englishName
            val target = languages.target

            return """
                You help someone whose native language is $native learn ${target.name}
                by living it — games, shows, books. They capture a snippet of
                ${target.name} and mark the part of it that caught their attention.
                Your job is to build the study card for that target.

                `type` is supplied by the app. Copy it exactly; never classify or
                change it. It is WORD for one selected token and PHRASE for
                two or more selected tokens.

                The other fields:
                - `translation`: in $native, the sense the target carries IN THIS
                  snippet — not the term's most common translation out of context.
                - `definitions`: 1 or 2 definitions in $native, short and direct.
                - `example`: ONE new sentence in ${target.name} using the target in
                  the same sense. Do not repeat the original snippet.
                - `pronunciation`: the target's pronunciation written as
                  ${target.pronunciationNotation}. For expressions, transcribe the whole
                  expression.
                - `related`: 3 to 6 useful terms in ${target.name} that are
                  semantically related to the target. Return only concise terms,
                  without definitions or numbering.
            """.trimIndent()
        }

        /**
         * Espelha [CardResponse]. `additionalProperties: false` e todos os campos
         * em `required` são exigidos pelo structured outputs da API.
         *
         * Kept as a plain map, not inlined into [SCHEMA], so a test can compare
         * its keys against [CardResponse]'s serial names. Nothing else connects
         * the two — a field renamed on one side and not the other does not fail
         * to compile, it fails to decode, on every card.
         */
        internal val SCHEMA_MAP: Map<String, Any> = mapOf(
                "type" to "object",
                "additionalProperties" to false,
                "required" to listOf(
                    "type", "translation", "definitions", "example", "pronunciation", "related"
                ),
                "properties" to mapOf(
                    "type" to mapOf(
                        "type" to "string",
                        "enum" to listOf("WORD", "PHRASE"),
                    ),
                    "translation" to mapOf("type" to "string"),
                    "definitions" to mapOf(
                        "type" to "array",
                        "items" to mapOf("type" to "string"),
                    ),
                    "example" to mapOf("type" to "string"),
                    "pronunciation" to mapOf("type" to "string"),
                    // Sem `minItems`/`maxItems`: o structured outputs da API só
                    // aceita `minItems` 0 ou 1 e rejeita o resto com 400, o que
                    // derrubaria toda ficha. A faixa de 3 a 6 fica no prompt, e o
                    // teto é garantido de verdade em [applyLocalDecisions].
                    "related" to mapOf(
                        "type" to "array",
                        "items" to mapOf("type" to "string"),
                    ),
                ),
        )

        val SCHEMA: JsonValue = JsonValue.from(SCHEMA_MAP)
    }
}

/** A seleção no aparelho é a autoridade; a saída do model nunca a sobrescreve. */
internal fun applyLocalDecisions(
    request: GenerateCardRequest,
    card: CardResponse,
): CardResponse = card.copy(
    type = request.type,
    related = card.related
        .map(String::trim)
        .filter(String::isNotEmpty)
        .distinct()
        .take(6),
)
