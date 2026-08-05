package com.jean.vocabs.shared.domain

/**
 * Que fatia do banco uma leitura enxerga.
 *
 * Nasceu quando o idioma deixou de recortar o app inteiro e passou a recortar só
 * a Início. Antes disso o par de idiomas era contexto implícito de tudo, e a
 * regra cabia numa frase; agora convivem três recortes ao mesmo tempo — a Início
 * mostra um curso por página, Vocabulários e Pendentes mostram os três juntos, e
 * "Seu progresso" mostra um curso que não é necessariamente o aberto.
 *
 * Ser um parâmetro com padrão, e não três famílias de métodos, é o que evita a
 * segunda API paralela: quem esquecer de escolher continua no curso aberto, que
 * é a resposta certa na maioria das telas.
 */
sealed interface Scope {

    /** O course que a pessoa está usando now. O padrão de quase toda leitura. */
    data object CursoAberto : Scope

    /**
     * Um curso nomeado, aberto ou não — o que "Seu progresso · francês" precisa
     * para existir sem trocar o curso ativo por baixo de quem só queria olhar.
     */
    data class Curso(val target: String) : Scope

    /** Todos os courses juntos: Vocabulários, Pendentes e Você. */
    data object Todos : Scope
}
