package com.jean.vocabs.ui.idiomas

/**
 * Os idiomas como a interface precisa mostrá-los: bandeira e nome.
 *
 * Só existe um de cada por enquanto, e isso é intencional — a escolha de verdade
 * ainda não foi construída. O que está aqui são as vitrines: o seletor de idioma
 * nativo e a fila de idiomas que se está aprendendo já ocupam o lugar certo na
 * tela, com o único valor real selecionado, para que ligar o resto depois seja
 * acrescentar itens a estas listas em vez de redesenhar duas telas.
 *
 * O servidor tem o seu próprio `ParDeIdiomas`, usado para montar o prompt. Os
 * dois vão virar um só quando o contrato passar a carregar os idiomas — está
 * anotado no NOTAS.md.
 */
data class IdiomaUi(
    val codigo: String,
    /** Emoji de bandeira: dois indicadores regionais, renderizados pelo sistema. */
    val bandeira: String,
    val nome: String,
)

object Idiomas {

    val PORTUGUES = IdiomaUi(codigo = "pt-BR", bandeira = "🇧🇷", nome = "Português")
    val INGLES = IdiomaUi(codigo = "en", bandeira = "🇺🇸", nome = "Inglês")

    /** Em que língua as fichas são escritas. Hoje, a base do app inteiro. */
    val nativoAtual: IdiomaUi = PORTUGUES

    /** As opções de idioma nativo já disponíveis. */
    val nativos: List<IdiomaUi> = listOf(PORTUGUES)

    /** O que se está aprendendo — a fila de bandeiras da tela de Início. */
    val alvos: List<IdiomaUi> = listOf(INGLES)

    val alvoAtual: IdiomaUi = INGLES
}
