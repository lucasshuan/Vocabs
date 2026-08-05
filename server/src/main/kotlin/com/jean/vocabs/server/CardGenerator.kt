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
 * A language pair the catalog does not know.
 *
 * Becomes a 400, not a 503: repeating the same request will never work, and the
 * app needs to tell "try again" apart from "this does not exist".
 */
class UnknownLanguagePair(native: String, target: String) :
    IllegalArgumentException("Par de languages desconhecido: $native → $target.")

/**
 * Turns a raw capture (snippet plus target) into a full card, via Claude.
 *
 * The critical part is structured output: the schema below forces the response
 * into [CardResponse]'s exact shape. Without it the model sometimes returns the
 * JSON wrapped in markdown or with a comment first, and you end up writing a
 * regex to extract it — which breaks on the first odd case.
 */
class CardGenerator(
    // Lazy on purpose: without it, a missing ANTHROPIC_API_KEY brings the server
    // down at boot and you cannot even test /health or authentication.
    clientFactory: () -> AnthropicClient = {
        AnthropicOkHttpClient.builder()
            .apiKey(Config.required("ANTHROPIC_API_KEY"))
            .build()
    },
) {
    private val client: AnthropicClient by lazy(clientFactory)
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Each pair's prompt, built once.
     *
     * The system text is the longest and most repeated piece of every call, and
     * rebuilding it per request would produce byte-different strings through
     * formatting luck alone — enough to lose the prompt cache on the other side.
     * There are few pairs per install; keeping them all costs nothing.
     */
    private val prompts = ConcurrentHashMap<LanguagePairSpec, String>()

    fun gerar(request: GenerateCardRequest): CardResponse {
        val languages = LanguagePairSpec.of(request.nativeLanguage, request.targetLanguage)
            ?: throw UnknownLanguagePair(request.nativeLanguage, request.targetLanguage)

        val params = MessageCreateParams.builder()
            .model(model)
            .maxTokens(2048L)
            .system(prompts.computeIfAbsent(languages, ::promptFor))
            .outputConfig(
                OutputConfig.builder()
                    .apply {
                        // Not every model accepts `effort` — Haiku 4.5 answers
                        // 400 "does not support the effort parameter". Sending it
                        // anyway fails the whole card, so it only goes to models
                        // that support it.
                        if (supportsEffort) {
                            // A short, well-defined extraction task: no deep
                            // reasoning needed. This is a cost/latency choice.
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

    // Configurable so models can be compared without recompiling. The default is
    // the most capable, for contextual definitions and related terms.
    private val model: String = Config["MODEL"] ?: DEFAULT_MODEL

    /** Haiku 4.5 rejects `effort` with a 400; Opus and Sonnet accept it. */
    private val supportsEffort: Boolean = !model.startsWith("claude-haiku")

    /** Internal, not private, so a test can check the prompt names both languages. */
    internal companion object {
        const val DEFAULT_MODEL = "claude-opus-5"

        /**
         * The instruction's language and the output's are independent: the model
         * reads this and writes the translation in the native language because
         * the instruction says so, not because the instruction is in that
         * language. Keeping a translated copy per native language would mean N
         * versions of calibrated prose — the WORD versus PHRASE test is the
         * subtlest part of a card, and translating is recalibrating by accident.
         *
         * Field names and enum values stay as they are: they are the contract
         * with [CardResponse], not text for the model to translate.
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
         * Mirrors [CardResponse]. `additionalProperties: false` and every field in
         * `required` are demanded by the API's structured outputs.
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
                    // No `minItems`/`maxItems`: the API's structured outputs only
                    // accepts `minItems` 0 or 1 and rejects the rest with a 400,
                    // which would fail every card. The 3-to-6 range lives in the
                    // prompt, and the ceiling is really enforced in
                    // [applyLocalDecisions].
                    "related" to mapOf(
                        "type" to "array",
                        "items" to mapOf("type" to "string"),
                    ),
                ),
        )

        val SCHEMA: JsonValue = JsonValue.from(SCHEMA_MAP)
    }
}

/** The device's selection is the authority; the model's output never overrides it. */
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
