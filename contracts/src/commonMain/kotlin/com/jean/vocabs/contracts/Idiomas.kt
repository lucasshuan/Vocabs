package com.jean.vocabs.contracts

/**
 * Que idiomas existem, com um nome só para os dois lados da conversa.
 *
 * Mora no contrato porque app e servidor precisam concordar sobre a mesma coisa
 * por motivos diferentes: a interface mostra a bandeira e um nome que vem dos
 * recursos do app, o prompt cita [Language.englishName], e o banco guarda
 * [Language.code]. Se cada lado tivesse a sua lista, o dia em que uma delas
 * ganhasse um idioma a mais seria o dia em que fichas passariam a nascer com o
 * idioma errado, sem ninguém ver.
 *
 * O nome de exibição não mora aqui: ele muda com o idioma da interface, e este
 * módulo é compilado também pelo servidor, que não tem recursos do Android.
 *
 * [Language.code] é a chave estável: está gravada em cada entrada do banco, e
 * renomeá-la transformaria fichas antigas em órfãs.
 */
data class Language(
    /** Chave estável, gravada no banco. Curta quando basta ("en"), com região quando não ("pt-BR"). */
    val code: String,
    /** Que bandeira representa o language — código do país, como o file do desenho. */
    val country: String,
    /** Como o prompt o cita — o prompt é escrito em inglês. */
    val englishName: String,
    /** BCP-47 completo, para voz e formatação. É o que o TTS entende. */
    val tag: String,
)

object Languages {

    /**
     * Em que ordem a lista de "Novo idioma" aparece.
     *
     * Não é alfabética de propósito: quem abre a tela quase sempre quer um dos
     * primeiros, e uma lista alfabética faria a pessoa rolar até o M para achar
     * mandarim. A busca resolve o resto.
     */
    val CATALOGO: List<Language> = listOf(
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

    private val porCodigo: Map<String, Language> = CATALOGO.associateBy { it.code }

    /** O par com que o app nasceu, e o que entries antigas recebem na migração. */
    const val NATIVO_PADRAO: String = "pt-BR"
    const val ALVO_PADRAO: String = "en"

    val PORTUGUES: Language = porCodigo.getValue(NATIVO_PADRAO)
    val INGLES: Language = porCodigo.getValue(ALVO_PADRAO)

    /**
     * Nulo para código desconhecido — e quem chama decide o que fazer.
     *
     * Um idioma pode sumir do catálogo enquanto ainda existe gravado em entradas
     * antigas. Devolver um substituto silencioso faria essas fichas mudarem de
     * idioma sozinhas; quem mostra a lista prefere pular a linha, e quem gera a
     * ficha prefere recusar.
     */
    fun de(codigo: String?): Language? = codigo?.let(porCodigo::get)
}
