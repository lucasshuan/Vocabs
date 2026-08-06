package io.github.lucasshuan.vocabu.server

import com.anthropic.client.AnthropicClient
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.core.JsonValue
import com.anthropic.models.messages.JsonOutputFormat
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.OutputConfig
import io.github.lucasshuan.vocabu.contracts.CardResponse
import io.github.lucasshuan.vocabu.contracts.GenerateCardRequest
import java.util.concurrent.ConcurrentHashMap
import kotlinx.serialization.json.Json

/** A 400, not a 503: retrying will never work, and the app retries on 503. */
class UnknownLanguagePair(native: String, target: String) :
    IllegalArgumentException("Par de languages desconhecido: $native → $target.")

/**
 * Snippet plus target into a card, via Claude.
 *
 * [SCHEMA] is what makes it parseable: unstructured, the model sometimes wraps
 * the JSON in markdown, and extracting it needs a regex that breaks on the
 * first odd case.
 */
class CardGenerator(
    // Lazy: otherwise a missing ANTHROPIC_API_KEY takes the server down at boot,
    // leaving /health and authentication untestable.
    clientFactory: () -> AnthropicClient = {
        AnthropicOkHttpClient.builder()
            .apiKey(Config.required("ANTHROPIC_API_KEY"))
            .build()
    },
) {
    private val client: AnthropicClient by lazy(clientFactory)
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Built once per pair: rebuilding the system text per request risks a
     * byte-different string, which loses the prompt cache on the other side.
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
                        // Haiku 4.5 rejects `effort` with a 400, failing the card.
                        // LOW where it is accepted: a short extraction task.
                        if (supportsEffort) effort(OutputConfig.Effort.LOW)
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
            ?: error("The Claude API returned no text block.")

        val card = json.decodeFromString<CardResponse>(text)
        return applyLocalDecisions(request, card)
    }

    private val model: String = Config["MODEL"] ?: DEFAULT_MODEL

    /** Haiku 4.5 rejects `effort` with a 400; Opus and Sonnet accept it. */
    private val supportsEffort: Boolean = !model.startsWith("claude-haiku")

    /** Internal, not private, so a test can check the prompt names both languages. */
    internal companion object {
        // Raise through MODEL if definitions or related terms come out thin.
        const val DEFAULT_MODEL = "claude-haiku-4-5"

        /**
         * English whatever the pair: instruction language and output language are
         * independent, and one calibrated prompt beats N translated copies — the
         * WORD/PHRASE test is the subtlest part, and translating recalibrates it
         * by accident. Field names are the contract with [CardResponse], not text
         * to translate.
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
         * `additionalProperties: false` and a full `required` list are demanded by
         * structured outputs.
         *
         * A map rather than inline JSON so a test can compare its keys against
         * [CardResponse]'s serial names. Nothing else connects the two: a field
         * renamed on one side still compiles, and fails to decode on every card.
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
                    // No `minItems`/`maxItems`: structured outputs accepts only
                    // `minItems` 0 or 1 and 400s on the rest. The range lives in
                    // the prompt, the ceiling in [applyLocalDecisions].
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
