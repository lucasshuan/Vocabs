package com.jean.vocabs.contracts

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The language pair travels with the request rather than being server
 * configuration: it belongs to the entry. A server that knew the pair on its
 * own would rewrite a German card in English the day someone switched courses.
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
    /**
     * Not named `ipa`: IPA is not what someone learning Mandarin wants to read,
     * where the answer is pinyin. The notation each language asks for lives in
     * `TargetLanguageSpec`, on the server.
     */
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
 * A code rather than a sentence, so the failure is worded in the reader's
 * language and not the server's. [detail] is what cannot be translated — the
 * AI provider's own text, for diagnosis.
 */
@Serializable
data class ErrorResponse(
    val code: String,
    val detail: String? = null,
)

/**
 * Sent as a `String`, not as this enum: kotlinx.serialization rejects an enum
 * value it does not know, so a newer server adding a code would break decoding
 * on an older client. [of] falls back instead.
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
