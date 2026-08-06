package io.github.lucasshuan.vocabu.contracts

/**
 * No display name here: it changes with the interface language, and the server
 * compiles this module without Android resources.
 */
data class Language(
    /** Stored in the database. Renaming one orphans its cards. */
    val code: String,
    /** Matches the flag drawable's name. */
    val country: String,
    /** What the prompt cites. */
    val englishName: String,
    /** BCP-47, for TTS and formatting. */
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
     * Null rather than a substitute: a language can leave the catalog while old
     * entries still store it, and substituting would change their language.
     */
    fun of(code: String?): Language? = code?.let(byCode::get)
}
