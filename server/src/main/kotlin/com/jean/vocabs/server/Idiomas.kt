package com.jean.vocabs.server

/**
 * O idioma que está sendo aprendido.
 *
 * É mais que um nome porque três coisas do prompt mudam de língua para língua:
 * a notação em que a pronúncia se escreve e os exemplos do teste palavra vs
 * expressão. Exemplos ingleses numa ficha de japonês seriam ruído, e IPA não é
 * o que alguém aprendendo mandarim quer ler — ali a resposta é pinyin.
 *
 * O idioma **nativo**, ao contrário, não precisa de nada disso: ele só diz em
 * que língua escrever a tradução e as definições. É por isso que o eixo que
 * realmente varia aqui é o alvo, e não o nativo.
 */
data class IdiomaAlvo(
    /** Em inglês, porque é a língua em que o prompt está escrito. */
    val nome: String,
    /** Como preencher o campo de pronúncia. */
    val notacaoDePronuncia: String,
    /** Dois ou três alvos que o teste classificaria como PALAVRA, nesta língua. */
    val exemplosDePalavra: String,
    /** O mesmo para EXPRESSAO, com o nome que a categoria tem nesta língua. */
    val exemplosDeExpressao: String,
) {
    companion object {
        val INGLES = IdiomaAlvo(
            nome = "English",
            notacaoDePronuncia = "IPA, without slashes",
            exemplosDePalavra = "\"ubiquitous\", \"meticulously\"",
            exemplosDeExpressao = "phrasal verbs, idioms and collocations: " +
                "\"kick the bucket\", \"on the fence\", \"pull off\"",
        )
    }
}

/**
 * O par que define uma ficha: em que língua ela é lida e que língua ela ensina.
 *
 * Hoje só existe [PADRAO], e de propósito. Deixar o usuário escolher mexe no
 * contrato, no banco — cada entrada precisa lembrar em que par nasceu, senão
 * regerar uma ficha antiga depois de trocar de idioma produz lixo — e no TTS,
 * que está fixo em inglês no app. Este tipo existe só para que o prompt já não
 * tenha nenhum idioma escrito por dentro quando essa hora chegar.
 */
data class ParDeIdiomas(
    /** Em inglês, porque é assim que o prompt vai citá-lo. */
    val nativo: String,
    val alvo: IdiomaAlvo,
) {
    companion object {
        val PADRAO = ParDeIdiomas(nativo = "Brazilian Portuguese", alvo = IdiomaAlvo.INGLES)
    }
}
