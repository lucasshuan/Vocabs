package com.jean.vocabs.server

import com.jean.vocabs.contracts.Idioma
import com.jean.vocabs.contracts.Idiomas

/**
 * O que muda no prompt quando o idioma alvo muda.
 *
 * É só uma coisa, e é sempre a mesma: **em que notação a pronúncia se escreve**.
 * Nome do idioma o catálogo do contrato já dá, e a classificação palavra vs
 * expressão nunca foi da IA — ela vem decidida do aparelho. Sobra a notação, e
 * ela varia de verdade: IPA não é o que alguém aprendendo mandarim quer ler,
 * ali a resposta é pinyin; em japonês, o kana com o romaji ao lado.
 *
 * O idioma **nativo** não precisa de nada disso: ele só diz em que língua
 * escrever a tradução e as definições, e para isso o nome basta.
 */
data class IdiomaAlvo(
    val idioma: Idioma,
    /** Como preencher o campo `pronuncia` da ficha. */
    val notacaoDePronuncia: String,
) {
    val nome: String get() = idioma.nomeEmIngles
}

/**
 * O par que define uma ficha: em que língua ela é lida e que língua ela ensina.
 *
 * Vem do pedido, e não de uma configuração do servidor, porque o par pertence à
 * entrada: uma ficha de alemão regerada depois de a pessoa trocar de curso
 * precisa voltar em alemão.
 */
data class ParDeIdiomas(
    val nativo: Idioma,
    val alvo: IdiomaAlvo,
) {
    companion object {
        val PADRAO = ParDeIdiomas(
            nativo = Idiomas.PORTUGUES,
            alvo = alvoDe(Idiomas.INGLES),
        )

        /**
         * Nulo quando um dos dois códigos não existe — e aí o pedido é recusado.
         *
         * Cair no padrão em vez de recusar seria pior que um erro: a pessoa
         * receberia uma ficha em inglês para uma palavra alemã e só descobriria
         * lendo. Um 400 diz o que aconteceu.
         */
        fun de(codigoNativo: String, codigoAlvo: String): ParDeIdiomas? {
            val nativo = Idiomas.de(codigoNativo) ?: return null
            val alvo = Idiomas.de(codigoAlvo) ?: return null
            return ParDeIdiomas(nativo = nativo, alvo = alvoDe(alvo))
        }
    }
}

/** IPA por padrão; a exceção é o idioma cuja pronúncia ninguém escreve em IPA. */
private fun alvoDe(idioma: Idioma) = IdiomaAlvo(idioma, notacaoDePronuncia = NOTACOES[idioma.codigo] ?: IPA)

private const val IPA = "IPA, without slashes"

private val NOTACOES: Map<String, String> = mapOf(
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
