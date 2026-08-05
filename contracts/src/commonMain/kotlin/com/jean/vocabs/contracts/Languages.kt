package com.jean.vocabs.contracts

/**
 * One catalog for both sides. If each kept its own, the day one of them gained
 * a language would be the day cards started being born in the wrong one.
 *
 * No display name here: that changes with the interface language, and this
 * module is compiled by the server too, which has no Android resources.
 */
data class Language(
    /** Stable key, stored in the database. Renaming one orphans its cards. */
    val code: String,
    /** Country code, matching the flag drawable. */
    val country: String,
    /** How the prompt refers to it; the prompt is written in English. */
    val englishName: String,
    /** Full BCP-47, for TTS and formatting. */
    val tag: String,
)

object Languages {

    /** Ordered by expected popularity, not alphabetically. Search covers the rest. */
    val CATALOG: List<Language> = listOf(
        Language("en", "us", "English", "en-US"),
        Language("es", "es", "Spanish", "es-ES"),
        Language("fr", "fr", "French", "fr-FR"),
        Language("de", "de", "German", "de-DE"),
        Language("it", "it", "Italian", "it-IT"),
        Language("ja", "jp", "Japanese", "ja-JP"),
        Language("ru", "ru", "Russian", "ru-RU"),
        Language("zh", "cn", "Mandarin Chinese", "zh-CN"),
        Language("ko", "kr", "Korean", "ko-KR"),
        Language("nl", "nl", "Dutch", "nl-NL"),
        Language("sv", "se", "Swedish", "sv-SE"),
        Language("ar", "sa", "Modern Standard Arabic", "ar-SA"),
        Language("pt-BR", "br", "Brazilian Portuguese", "pt-BR"),
        Language("pt-PT", "pt", "European Portuguese", "pt-PT"),
        Language("hi", "in", "Hindi", "hi-IN"),
        Language("tr", "tr", "Turkish", "tr-TR"),
        Language("pl", "pl", "Polish", "pl-PL"),
        Language("el", "gr", "Greek", "el-GR"),
        Language("he", "il", "Hebrew", "he-IL"),
        Language("nb", "no", "Norwegian Bokmål", "nb-NO"),
        Language("da", "dk", "Danish", "da-DK"),
        Language("fi", "fi", "Finnish", "fi-FI"),
        Language("cs", "cz", "Czech", "cs-CZ"),
        Language("hu", "hu", "Hungarian", "hu-HU"),
        Language("ro", "ro", "Romanian", "ro-RO"),
        Language("uk", "ua", "Ukrainian", "uk-UA"),
        Language("th", "th", "Thai", "th-TH"),
        Language("vi", "vn", "Vietnamese", "vi-VN"),
        Language("id", "id", "Indonesian", "id-ID"),
        Language("ms", "my", "Malay", "ms-MY"),
        Language("fa", "ir", "Persian", "fa-IR"),
        Language("sw", "ke", "Swahili", "sw-KE"),
        Language("ca", "es-ct", "Catalan", "ca-ES"),
        Language("is", "is", "Icelandic", "is-IS"),
        Language("bg", "bg", "Bulgarian", "bg-BG"),
        Language("hr", "hr", "Croatian", "hr-HR"),
        Language("sr", "rs", "Serbian", "sr-RS"),
        Language("sk", "sk", "Slovak", "sk-SK"),
        Language("et", "ee", "Estonian", "et-EE"),
        Language("lv", "lv", "Latvian", "lv-LV"),
        Language("lt", "lt", "Lithuanian", "lt-LT"),
        Language("tl", "ph", "Filipino", "fil-PH"),
        Language("bn", "bd", "Bengali", "bn-BD"),
    )

    private val byCode: Map<String, Language> = CATALOG.associateBy { it.code }

    const val DEFAULT_NATIVE: String = "pt-BR"
    const val DEFAULT_TARGET: String = "en"

    val PORTUGUESE: Language = byCode.getValue(DEFAULT_NATIVE)
    val ENGLISH: Language = byCode.getValue(DEFAULT_TARGET)

    /**
     * Null for an unknown code, so the caller decides. A language can leave the
     * catalog while still stored on old entries; substituting silently would
     * make those cards change language on their own.
     */
    fun of(code: String?): Language? = code?.let(byCode::get)
}
