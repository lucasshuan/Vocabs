package io.github.lucasshuan.vocabu.server

import io.github.lucasshuan.vocabu.contracts.Language
import io.github.lucasshuan.vocabu.contracts.Languages

/**
 * The only thing a target language changes in the prompt: the notation the
 * pronunciation is written in. The name comes from the catalog, and the
 * WORD/PHRASE call arrives decided from the device.
 *
 * The native language needs no spec — it only names the language translations
 * are written in.
 */
data class TargetLanguageSpec(
    val language: Language,
    val pronunciationNotation: String,
) {
    val name: String get() = language.englishName
}

/**
 * From the request, never from server configuration: a German card regenerated
 * after a language switch has to come back in German.
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
         * Null on an unknown code, refused upstream. Falling back to the default
         * would answer a German word with an English card, discovered by reading it.
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
