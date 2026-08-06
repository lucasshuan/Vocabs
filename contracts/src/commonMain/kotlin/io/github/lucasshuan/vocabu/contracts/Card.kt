package io.github.lucasshuan.vocabu.contracts

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The pair travels with the request instead of being server configuration: a
 * server holding its own pair would rewrite a German card in English the day
 * someone switched language.
 *
 * [nativeLanguage] and [targetLanguage] are [Languages] codes, not names.
 */
@Serializable
data class GenerateCardRequest(
    val snippet: String,
    val target: String,
    val type: TargetType,
    val nativeLanguage: String,
    val targetLanguage: String,
)

@Serializable
data class CardResponse(
    val type: TargetType,
    val translation: String,
    val definitions: List<String>,
    val example: String,
    /** Not `ipa`: Mandarin wants pinyin. Notation per language: `TargetLanguageSpec`. */
    val pronunciation: String,
    val related: List<String> = emptyList(),
)

/** Decided on the device, never by the AI: one token is a WORD, several a PHRASE. */
@Serializable
enum class TargetType {
    @SerialName("WORD")
    WORD,

    @SerialName("PHRASE")
    PHRASE,
}

/**
 * A code, not a sentence: the app owns the wording. [detail] is the provider's
 * own untranslated text, the only diagnostic there is.
 */
@Serializable
data class ErrorResponse(
    val code: String,
    val detail: String? = null,
)

/**
 * Sent as a `String`, not as this enum: kotlinx.serialization throws on an
 * unknown enum value, so a newer server's code would break older clients.
 * [of] falls back instead.
 */
enum class ErrorCode {
    UNKNOWN_LANGUAGE_PAIR,
    MISSING_FIELDS,
    INVALID_TOKEN,
    GENERATION_FAILED,

    /** Client-side only: the server never answered. */
    UNREACHABLE,

    /** Client-side only: it answered, but not in a readable shape. */
    HTTP_ERROR,
    ;

    companion object {
        fun of(value: String?): ErrorCode =
            entries.firstOrNull { it.name == value } ?: GENERATION_FAILED
    }
}
