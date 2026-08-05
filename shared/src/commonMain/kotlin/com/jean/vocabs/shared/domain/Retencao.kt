package com.jean.vocabs.shared.domain

/**
 * A força de memória de uma palavra: quanto você lembra dela agora.
 *
 * O modelo é o do documento de produto — pontos de 0 a 100 que sobem com acertos
 * e caem sozinhos com o tempo, em vez de SM-2/Anki com suas muitas variáveis
 * interdependentes. O que sustenta tudo são **dois** números, não um: se todas
 * as palavras decaíssem no mesmo ritmo, uma dominada há meses cairia tão rápido
 * quanto uma nova — exatamente a vantagem que a repetição espaçada existe para
 * dar. Por isso [taxa] diminui a cada acerto e sobe a cada erro.
 *
 * Nenhuma função aqui lê o relógio: todas recebem `agora`. É isso que deixa o
 * repositório reusar a costura de tempo que ele já tem injetada e impede a UI de
 * inventar o seu próprio "agora" — se a barra da ficha e a fila da home
 * consultassem relógios diferentes, elas discordariam sem ninguém entender por quê.
 */
data class Retencao(
    val pontos: Double,
    val taxa: Double,
    val ultimaInteracao: Long,
    val revisoes: Int,
    val acertos: Int = 0,
    val erros: Int = 0,
) {

    /**
     * Quantas respostas têm desfecho guardado.
     *
     * Nem sempre igual a [revisoes]: as revisões feitas antes de o placar existir
     * contam no total, mas ninguém sabe se foram acerto ou erro.
     */
    val respondidas: Int get() = acertos + erros

    /**
     * De 0 a 1, ou nulo quando ainda não há resposta com desfecho conhecido.
     *
     * Nulo em vez de zero de propósito: "nunca respondida" e "errou todas" são
     * coisas diferentes, e um 0% dito na cara de quem acabou de capturar a palavra
     * seria só desanimador.
     */
    val taxaDeAcerto: Double? get() = if (respondidas == 0) null else acertos.toDouble() / respondidas

    /** Quanto resta da memória neste instante. */
    fun pontosEm(agora: Long): Double {
        // coerceAtLeast porque um acerto de relógio para trás (NTP, troca de
        // fuso) daria uma diferença negativa e a palavra *ganharia* pontos.
        val dias = (agora - ultimaInteracao).coerceAtLeast(0L) / MILLIS_POR_DIA
        return (pontos - taxa * dias).coerceIn(0.0, PONTOS_MAX)
    }

    fun nivelEm(agora: Long): MemoryLevel = when {
        revisoes == 0 -> MemoryLevel.NEW
        else -> when {
            pontosEm(agora) < 30.0 -> MemoryLevel.LEARNING
            pontosEm(agora) < 70.0 -> MemoryLevel.FAMILIAR
            else -> MemoryLevel.MASTERED
        }
    }

    fun precisaRevisar(agora: Long): Boolean = pontosEm(agora) < LIMIAR_REVISAO

    /** Quanto falta, em millis, para a palavra cruzar o limiar. 0 se já cruzou. */
    fun proximaRevisaoEm(agora: Long): Long {
        val atuais = pontosEm(agora)
        if (atuais < LIMIAR_REVISAO) return 0L
        if (taxa <= 0.0) return Long.MAX_VALUE
        return ((atuais - LIMIAR_REVISAO) / taxa * MILLIS_POR_DIA).toLong()
    }

    /**
     * O resultado de um cartão respondido.
     *
     * Acerto devolve os pontos ao topo e alarga o intervalo; erro zera os pontos
     * e encurta. Zerar (em vez de baixar para 30 ou 50) não é rigor: `0 < 60` é
     * sempre verdade, então a palavra **fica na fila continuamente até você
     * acertar** — sem flag de "reaprendizado", sem estado extra, sem coluna nova.
     */
    fun apos(acertou: Boolean, agora: Long): Retencao = Retencao(
        pontos = if (acertou) PONTOS_MAX else 0.0,
        taxa = if (acertou) {
            (taxa / DIVISOR_ACERTO).coerceIn(TAXA_MINIMA, TAXA_MAXIMA)
        } else {
            (taxa * MULTIPLICADOR_ERRO).coerceIn(TAXA_MINIMA, TAXA_MAXIMA)
        },
        ultimaInteracao = agora,
        revisoes = revisoes + 1,
        acertos = if (acertou) acertos + 1 else acertos,
        erros = if (acertou) erros else erros + 1,
    )

    companion object {
        const val PONTOS_MAX = 100.0

        /** Do documento de produto. Todo intervalo é `40 / taxa`. */
        const val LIMIAR_REVISAO = 60.0

        /**
         * `40 / 40 = 1 dia` até a primeira revisão. Nem "revise agora" — que o
         * princípio 2 proíbe — nem daqui a uma semana, quando você já esqueceu.
         */
        const val TAXA_INICIAL = 40.0

        const val DIVISOR_ACERTO = 1.5

        /**
         * É o número que faz o erro **cortar o intervalo pela metade**.
         *
         * O intervalo depois de errar-e-depois-acertar é `1.5/m` do anterior.
         * Com 2.0 daria 0,75× — o erro quase não faria nada, que é a armadilha
         * da escolha "simétrica". Com 3.0 a regra cabe numa frase, e um erro
         * desfaz 2,7 acertos. O equilíbrio fica em 73% de acerto.
         */
        const val MULTIPLICADOR_ERRO = 3.0

        /**
         * Intervalo máximo de ~67 dias. O piso é o que define a carga diária em
         * regime (`N × 0.6 / 40`): 500 palavras dão 7,5 cartões por dia. Um piso
         * menor soaria melhor e é exatamente como um baralho morre — a fila
         * secaria e não haveria mais o que revisar.
         */
        const val TAXA_MINIMA = 0.6

        /**
         * Intervalo mínimo de 16h. Sem teto, uma palavra nova errada duas vezes
         * voltaria em 4 horas — o app dizendo "revise agora", a única coisa que
         * o princípio 2 proíbe.
         */
        const val TAXA_MAXIMA = 60.0

        /**
         * Dias fracionários, nunca inteiros. Com `floor`, uma palavra a 40 de
         * taxa chegaria a `100 - 40×1 = 60` em 24h, e `60 < 60` é falso: ela só
         * entraria na fila no dia seguinte. O intervalo de 1 dia viraria 2 sem
         * ninguém perceber.
         */
        private const val MILLIS_POR_DIA = 86_400_000.0

        /** O estado em que uma palavra entra no sistema, quando a ficha fica pronta. */
        fun inicial(agora: Long) = Retencao(
            pontos = PONTOS_MAX,
            taxa = TAXA_INICIAL,
            ultimaInteracao = agora,
            revisoes = 0,
        )
    }
}

/** 0-30 aprendendo · 30-70 familiar · 70-100 dominada, mais o caso de nunca revisada. */
enum class MemoryLevel {
    NEW,
    LEARNING,
    FAMILIAR,
    MASTERED,
}

data class Sequencia(
    val diasSeguidos: Int,
    val revisouHoje: Boolean,
)

/**
 * Quantos dias seguidos você revisou, a partir dos dias registrados.
 *
 * [dias] vem em ordem decrescente e sem repetição (é a chave primária da tabela).
 *
 * A sequência pode ancorar em **ontem**: não ter revisado ainda hoje não quebra
 * nada — o dia não acabou. Só pular um dia inteiro quebra. Por isso [Sequencia]
 * separa `revisouHoje`, que é o que a tela usa para dizer se ainda falta.
 */
fun sequenciaDe(dias: List<Long>, hoje: Long): Sequencia {
    val revisouHoje = dias.firstOrNull() == hoje
    val inicio = when {
        revisouHoje -> hoje
        dias.firstOrNull() == hoje - 1 -> hoje - 1
        else -> return Sequencia(diasSeguidos = 0, revisouHoje = false)
    }

    var esperado = inicio
    var total = 0
    for (dia in dias) {
        if (dia != esperado) break
        total++
        esperado--
    }
    return Sequencia(diasSeguidos = total, revisouHoje = revisouHoje)
}
