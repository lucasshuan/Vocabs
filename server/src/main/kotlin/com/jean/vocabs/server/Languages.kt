package com.jean.vocabs.server

import com.jean.vocabs.contracts.Language
import com.jean.vocabs.contracts.Languages

/**
 * What changes in the prompt when the target language changes.
 *
 * Only one thing, and always the same: **which notation the pronunciation is
 * written in**. The contract's catalog already gives the language name, and the
 * word-versus-phrase classification was never the AI's — it arrives decided from
 * the device. The notation genuinely varies: IPA is not what someone learning
 * Mandarin wants to read, where the answer is pinyin.
 *
 * The **native** language needs none of this: it only says which language to
 * write the translation and definitions in, and the name is enough for that.
 */
data class TargetLanguageSpec(
    val language: Language,
    /** How to fill the card's `pronunciation` field. */
    val pronunciationNotation: String,
) {
    val name: String get() = language.englishName
}

/**
 * The pair that defines a card: which language it is read in, and which it
 * teaches.
 *
 * It comes from the request rather than from server configuration, because the
 * pair belongs to the entry: a German card regenerated after switching courses
 * has to come back in German.
 */
data class LanguagePairSpec(
    val native: Language,
    val target: TargetLanguageSpec,
) {
    companion object {
        val DEFAULT = LanguagePairSpec(
            native = Languages.PORTUGUESE,
            target = targetOf(Languages.ENGLISH),
        )

        /**
         * Null when one of the two codes does not exist, and the request is then
         * refused.
         *
         * Falling back to the default would be worse than an error: the person
         * would get an English card for a German word and only find out by
         * reading it. A 400 says what happened.
         */
        fun of(nativeCode: String, targetCode: String): LanguagePairSpec? {
            val native = Languages.of(nativeCode) ?: return null
            val target = Languages.of(targetCode) ?: return null
            return LanguagePairSpec(native = native, target = targetOf(target))
        }
    }
}

/** IPA by default; the exceptions are the languages nobody writes in IPA. */
private fun targetOf(language: Language) = TargetLanguageSpec(language, pronunciationNotation = NOTATIONS[language.code] ?: IPA)

private const val IPA = "IPA, without slashes"

private val NOTATIONS: Map<String, String> = mapOf(
    "zh" to "Hanyu Pinyin with tone marks",
    "ja" to "the kana reading, followed by Hepburn romaji in parentheses",
    "ko" to "Revised Romanization of Korean",
    "ar" to "the fully vowelled Arabic script, followed by a Latin transliteration in parentheses",
    "fa" to "the Persian script with vowel diacritics, followed by a Latin transliteration in parentheses",
    "he" to "Hebrew with niqqud, followed by a Latin transliteration in parentheses",
    "th" to "the Royal Thai General System of Transcription",
    "hi" to "IAST transliteration",
    "bn" to "IAST transliteration",
)
