package com.jean.vocabs.contracts

/**
 * Que idiomas existem, com um nome só para os dois lados da conversa.
 *
 * Mora no contrato porque app e servidor precisam concordar sobre a mesma coisa
 * por motivos diferentes: a interface mostra [nome] e a bandeira, o prompt cita
 * [nomeEmIngles], e o banco guarda [codigo]. Se cada lado tivesse a sua lista,
 * o dia em que uma delas ganhasse um idioma a mais seria o dia em que fichas
 * passariam a nascer com o idioma errado, sem ninguém ver.
 *
 * [codigo] é a chave estável e nunca muda: ele está gravado em cada entrada do
 * banco, e renomeá-lo transformaria fichas antigas em órfãs.
 */
data class Idioma(
    /** Chave estável, gravada no banco. Curta quando basta ("en"), com região quando não ("pt-BR"). */
    val codigo: String,
    /** Que bandeira representa o idioma — código do país, como o arquivo do desenho. */
    val pais: String,
    /** Como a interface o chama, em português. */
    val nome: String,
    /** Como o prompt o cita — o prompt é escrito em inglês. */
    val nomeEmIngles: String,
    /** BCP-47 completo, para voz e formatação. É o que o TTS entende. */
    val etiqueta: String,
)

object Idiomas {

    /**
     * Em que ordem a lista de "Novo idioma" aparece.
     *
     * Não é alfabética de propósito: quem abre a tela quase sempre quer um dos
     * primeiros, e uma lista alfabética faria a pessoa rolar até o M para achar
     * mandarim. A busca resolve o resto.
     */
    val CATALOGO: List<Idioma> = listOf(
        Idioma("en", "us", "Inglês", "English", "en-US"),
        Idioma("es", "es", "Espanhol", "Spanish", "es-ES"),
        Idioma("fr", "fr", "Francês", "French", "fr-FR"),
        Idioma("de", "de", "Alemão", "German", "de-DE"),
        Idioma("it", "it", "Italiano", "Italian", "it-IT"),
        Idioma("ja", "jp", "Japonês", "Japanese", "ja-JP"),
        Idioma("ru", "ru", "Russo", "Russian", "ru-RU"),
        Idioma("zh", "cn", "Mandarim", "Mandarin Chinese", "zh-CN"),
        Idioma("ko", "kr", "Coreano", "Korean", "ko-KR"),
        Idioma("nl", "nl", "Holandês", "Dutch", "nl-NL"),
        Idioma("sv", "se", "Sueco", "Swedish", "sv-SE"),
        Idioma("ar", "sa", "Árabe", "Modern Standard Arabic", "ar-SA"),
        Idioma("pt-BR", "br", "Português (Brasil)", "Brazilian Portuguese", "pt-BR"),
        Idioma("pt-PT", "pt", "Português (Portugal)", "European Portuguese", "pt-PT"),
        Idioma("hi", "in", "Hindi", "Hindi", "hi-IN"),
        Idioma("tr", "tr", "Turco", "Turkish", "tr-TR"),
        Idioma("pl", "pl", "Polonês", "Polish", "pl-PL"),
        Idioma("el", "gr", "Grego", "Greek", "el-GR"),
        Idioma("he", "il", "Hebraico", "Hebrew", "he-IL"),
        Idioma("nb", "no", "Norueguês", "Norwegian Bokmål", "nb-NO"),
        Idioma("da", "dk", "Dinamarquês", "Danish", "da-DK"),
        Idioma("fi", "fi", "Finlandês", "Finnish", "fi-FI"),
        Idioma("cs", "cz", "Tcheco", "Czech", "cs-CZ"),
        Idioma("hu", "hu", "Húngaro", "Hungarian", "hu-HU"),
        Idioma("ro", "ro", "Romeno", "Romanian", "ro-RO"),
        Idioma("uk", "ua", "Ucraniano", "Ukrainian", "uk-UA"),
        Idioma("th", "th", "Tailandês", "Thai", "th-TH"),
        Idioma("vi", "vn", "Vietnamita", "Vietnamese", "vi-VN"),
        Idioma("id", "id", "Indonésio", "Indonesian", "id-ID"),
        Idioma("ms", "my", "Malaio", "Malay", "ms-MY"),
        Idioma("fa", "ir", "Persa", "Persian", "fa-IR"),
        Idioma("sw", "ke", "Suaíli", "Swahili", "sw-KE"),
        Idioma("ca", "es-ct", "Catalão", "Catalan", "ca-ES"),
        Idioma("is", "is", "Islandês", "Icelandic", "is-IS"),
        Idioma("bg", "bg", "Búlgaro", "Bulgarian", "bg-BG"),
        Idioma("hr", "hr", "Croata", "Croatian", "hr-HR"),
        Idioma("sr", "rs", "Sérvio", "Serbian", "sr-RS"),
        Idioma("sk", "sk", "Eslovaco", "Slovak", "sk-SK"),
        Idioma("et", "ee", "Estoniano", "Estonian", "et-EE"),
        Idioma("lv", "lv", "Letão", "Latvian", "lv-LV"),
        Idioma("lt", "lt", "Lituano", "Lithuanian", "lt-LT"),
        Idioma("tl", "ph", "Filipino", "Filipino", "fil-PH"),
        Idioma("bn", "bd", "Bengali", "Bengali", "bn-BD"),
    )

    private val porCodigo: Map<String, Idioma> = CATALOGO.associateBy { it.codigo }

    /** O par com que o app nasceu, e o que entradas antigas recebem na migração. */
    const val NATIVO_PADRAO: String = "pt-BR"
    const val ALVO_PADRAO: String = "en"

    val PORTUGUES: Idioma = porCodigo.getValue(NATIVO_PADRAO)
    val INGLES: Idioma = porCodigo.getValue(ALVO_PADRAO)

    /**
     * Nulo para código desconhecido — e quem chama decide o que fazer.
     *
     * Um idioma pode sumir do catálogo enquanto ainda existe gravado em entradas
     * antigas. Devolver um substituto silencioso faria essas fichas mudarem de
     * idioma sozinhas; quem mostra a lista prefere pular a linha, e quem gera a
     * ficha prefere recusar.
     */
    fun de(codigo: String?): Idioma? = codigo?.let(porCodigo::get)

    /** O nome que sobra quando o código não está no catálogo: o próprio código. */
    fun nomeDe(codigo: String?): String = de(codigo)?.nome ?: codigo.orEmpty()
}
