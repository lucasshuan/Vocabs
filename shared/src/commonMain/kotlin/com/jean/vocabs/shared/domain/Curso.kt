package com.jean.vocabs.shared.domain

import com.jean.vocabs.contracts.Idiomas

/**
 * Em que língua se lê e que língua se aprende — o par em que uma captura nasceu.
 *
 * São códigos do catálogo de [Idiomas], e não os objetos: o par é gravado no
 * banco e comparado o tempo todo, e um código estável sobrevive ao dia em que o
 * catálogo mudar um nome.
 */
data class ParIdiomas(
    val nativo: String,
    val alvo: String,
) {
    companion object {
        val PADRAO = ParIdiomas(nativo = Idiomas.NATIVO_PADRAO, alvo = Idiomas.ALVO_PADRAO)
    }
}

/**
 * Quanto vocabulário existe num curso — o "9 de 24" da faixa de idiomas.
 *
 * [dominadas] conta degraus, não pontos: a faixa mostraria um número diferente a
 * cada hora se contasse força de memória, porque ela decai sozinha. O que a
 * pessoa quer ver ali é o quanto já subiu, e isso não desce enquanto ela dorme.
 *
 * [naFila] e [proximaEmMillis] existem para o selo: são a mesma pergunta que o
 * cartão da Início faz, respondida por curso, e sem elas a faixa teria que ler a
 * fila de cada idioma por conta própria.
 */
data class ResumoCurso(
    val par: ParIdiomas,
    val total: Int,
    val dominadas: Int,
    val naFila: Int = 0,
    /** Millis até a próxima ficha deste curso pedir revisão. Nulo quando não há nenhuma. */
    val proximaEmMillis: Long? = null,
) {
    val selo: SeloDeCurso
        get() = when {
            naFila > 0 -> SeloDeCurso.Revisar(naFila)
            proximaEmMillis != null -> SeloDeCurso.EmDia
            else -> SeloDeCurso.Vazio
        }
}

/**
 * O estado que toda bandeira da faixa carrega — nunca vazia, nunca um "0" escrito.
 *
 * São três e não quatro: "sem nada agendado" ([Vazio]) é curso novo, e é
 * diferente de "em dia", que é uma conquista. Escrever zero no lugar do tique
 * transformaria a única boa notícia da faixa num placar de nada feito.
 */
sealed interface SeloDeCurso {
    data class Revisar(val quantas: Int) : SeloDeCurso
    data object EmDia : SeloDeCurso
    data object Vazio : SeloDeCurso
}

/**
 * A escada de cinco degraus da tela "O que falta".
 *
 * Não é a mesma coisa que [MemoryLevel], e a diferença é o ponto: força de
 * memória responde "quanto você lembra **agora**" e por isso decai sozinha;
 * degrau responde "quão longe você chegou" e só se mexe quando você responde um
 * cartão. Uma tela chamada "O que falta" precisa da segunda pergunta — uma barra
 * que anda para trás enquanto a pessoa dorme não diz o que falta fazer.
 *
 * O degrau sai da **taxa de decaimento**, que já é o histórico de acertos
 * comprimido num número: cada acerto a divide por [Retencao.DIVISOR_ACERTO] e
 * cada erro a multiplica por [Retencao.MULTIPLICADOR_ERRO]. Não é preciso guardar
 * nada de novo para saber em que degrau alguém está.
 */
object Degraus {
    const val TOTAL = 5

    /**
     * Os limites de taxa de cada degrau: 40, 26.7, 17.8, 11.9, 7.9.
     *
     * Cada um é o anterior dividido pelo ganho de um acerto — subir um degrau é
     * literalmente acertar mais uma vez.
     */
    private val LIMITES: List<Double> = generateSequence(Retencao.TAXA_INICIAL) { it / Retencao.DIVISOR_ACERTO }
        .take(TOTAL)
        .toList()

    /** De 1 a [TOTAL]. Palavra nunca revisada está no primeiro. */
    fun de(retencao: Retencao?): Int {
        if (retencao == null || retencao.revisoes == 0) return 1
        val alcancados = LIMITES.count { retencao.taxa <= it + TOLERANCIA }
        return alcancados.coerceIn(1, TOTAL)
    }

    /**
     * O nome do degrau. Coincide de propósito com os nomes de [MemoryLevel]:
     * duas escalas já são o limite do que uma pessoa aceita aprender; dois
     * vocabulários seriam demais.
     */
    fun nivel(degrau: Int): MemoryLevel = when {
        degrau >= TOTAL -> MemoryLevel.MASTERED
        degrau == TOTAL - 1 -> MemoryLevel.FAMILIAR
        else -> MemoryLevel.LEARNING
    }

    /** Quantos acertos faltam para o degrau mudar de nome. Zero quando já é o topo. */
    fun acertosParaSubirDeNivel(degrau: Int): Int {
        val atual = nivel(degrau)
        val proximo = (degrau..TOTAL).firstOrNull { nivel(it) != atual } ?: return 0
        return proximo - degrau
    }

    /** Uma comparação de doubles que sobreviveu a cinco divisões seguidas. */
    private const val TOLERANCIA = 1e-9
}

/**
 * A quota do dia: quantas revisões já saíram e quantas o dia ainda tem.
 *
 * [total] não é uma meta escolhida no dedo — é o que o próprio decaimento pediu
 * hoje, já feito mais o que ainda está na fila. Uma meta fixa mentiria nos dois
 * sentidos: seria inalcançável no dia em que 30 palavras vencem juntas e já
 * estaria batida num dia sem nada para revisar.
 */
data class QuotaDoDia(
    val feita: Int,
    val naFila: Int,
) {
    val total: Int get() = feita + naFila
    val batida: Boolean get() = naFila == 0
    val fracao: Float get() = if (total == 0) 1f else (feita.toFloat() / total).coerceIn(0f, 1f)
}

/**
 * Uma coisa que aconteceu com uma palavra, num dia.
 *
 * A retenção guarda só o estado de agora; a linha do tempo precisa do que
 * aconteceu, e nenhum dos dois se reconstrói a partir do outro.
 */
data class Evento(
    val id: Long,
    val entradaId: Long,
    val dia: Long,
    val instante: Long,
    val tipo: EventType,
    val alvo: String,
    val par: ParIdiomas,
    /** Número da revisão em [EventType.CORRECT]/[EventType.INCORRECT], nível novo em [EventType.LEVELED_UP]. */
    val detalhe: String?,
)

enum class EventType {
    CAPTURED,
    CARD_READY,
    CORRECT,
    INCORRECT,
    LEVELED_UP;

    companion object {
        fun de(valor: String): EventType = entries.firstOrNull { it.name == valor } ?: CAPTURED
    }
}

/**
 * A maior sequência de dias seguidos que já houve, para o "11 melhor sequência".
 *
 * [dias] vem em ordem decrescente e sem repetição, como sai do banco.
 */
fun melhorSequenciaDe(dias: List<Long>): Int {
    if (dias.isEmpty()) return 0
    var melhor = 1
    var atual = 1
    for (indice in 1 until dias.size) {
        if (dias[indice] == dias[indice - 1] - 1) atual++ else atual = 1
        if (atual > melhor) melhor = atual
    }
    return melhor
}
