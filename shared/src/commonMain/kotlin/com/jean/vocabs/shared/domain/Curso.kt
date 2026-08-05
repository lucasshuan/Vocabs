package com.jean.vocabs.shared.domain

import com.jean.vocabs.contracts.Languages

/**
 * Em que língua se lê e que língua se aprende — o par em que uma captura nasceu.
 *
 * São códigos do catálogo de [Languages], e não os objetos: o par é gravado no
 * banco e comparado o tempo todo, e um código estável sobrevive ao dia em que o
 * catálogo mudar um nome.
 */
data class LanguagePair(
    val native: String,
    val target: String,
) {
    companion object {
        val PADRAO = LanguagePair(native = Languages.NATIVO_PADRAO, target = Languages.ALVO_PADRAO)
    }
}

/**
 * Quanto vocabulário existe num curso — o "9 de 24" da faixa de idiomas.
 *
 * [dominadas] conta degraus, não pontos: a faixa mostraria um número diferente a
 * cada hora se contasse força de memória, porque ela decai sozinha. O que a
 * pessoa quer ver ali é o quanto já subiu, e isso não desce enquanto ela dorme.
 *
 * [inQueue] e [nextInMillis] existem para o selo: são a mesma pergunta que o
 * cartão da Início faz, respondida por curso, e sem elas a faixa teria que ler a
 * fila de cada idioma por conta própria.
 */
data class CourseSummary(
    val languagePair: LanguagePair,
    val total: Int,
    val mastered: Int,
    val inQueue: Int = 0,
    /** Millis até a próxima card deste course pedir revisão. Nulo quando não há nenhuma. */
    val nextInMillis: Long? = null,
) {
    val badge: CourseBadge
        get() = when {
            inQueue > 0 -> CourseBadge.Revisar(inQueue)
            nextInMillis != null -> CourseBadge.EmDia
            else -> CourseBadge.Vazio
        }
}

/**
 * O estado que toda bandeira da faixa carrega — nunca vazia, nunca um "0" escrito.
 *
 * São três e não quatro: "sem nada agendado" ([Vazio]) é curso novo, e é
 * diferente de "em dia", que é uma conquista. Escrever zero no lugar do tique
 * transformaria a única boa notícia da faixa num placar de nada feito.
 */
sealed interface CourseBadge {
    data class Revisar(val quantas: Int) : CourseBadge
    data object EmDia : CourseBadge
    data object Vazio : CourseBadge
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
 * comprimido num número: cada acerto a divide por [Retention.DIVISOR_ACERTO] e
 * cada erro a multiplica por [Retention.MULTIPLICADOR_ERRO]. Não é preciso guardar
 * nada de novo para saber em que degrau alguém está.
 */
object Steps {
    const val TOTAL = 5

    /**
     * Os limites de taxa de cada degrau: 40, 26.7, 17.8, 11.9, 7.9.
     *
     * Cada um é o anterior dividido pelo ganho de um acerto — subir um degrau é
     * literalmente acertar mais uma vez.
     */
    private val LIMITES: List<Double> = generateSequence(Retention.TAXA_INICIAL) { it / Retention.DIVISOR_ACERTO }
        .take(TOTAL)
        .toList()

    /** De 1 a [TOTAL]. Palavra nunca revisada está no primeiro. */
    fun de(retention: Retention?): Int {
        if (retention == null || retention.reviews == 0) return 1
        val alcancados = LIMITES.count { retention.taxa <= it + TOLERANCIA }
        return alcancados.coerceIn(1, TOTAL)
    }

    /**
     * O nome do degrau. Coincide de propósito com os nomes de [MemoryLevel]:
     * duas escalas já são o limite do que uma pessoa aceita aprender; dois
     * vocabulários seriam demais.
     */
    fun level(degrau: Int): MemoryLevel = when {
        degrau >= TOTAL -> MemoryLevel.MASTERED
        degrau == TOTAL - 1 -> MemoryLevel.FAMILIAR
        else -> MemoryLevel.LEARNING
    }

    /** Quantos hits faltam para o degrau mudar de name. Zero quando já é o topo. */
    fun hitsToLevelUp(degrau: Int): Int {
        val current = level(degrau)
        val proximo = (degrau..TOTAL).firstOrNull { level(it) != current } ?: return 0
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
data class DailyQuota(
    val done: Int,
    val inQueue: Int,
) {
    val total: Int get() = done + inQueue
    val batida: Boolean get() = inQueue == 0
    val fracao: Float get() = if (total == 0) 1f else (done.toFloat() / total).coerceIn(0f, 1f)
}

/**
 * Uma coisa que aconteceu com uma palavra, num dia.
 *
 * A retenção guarda só o estado de agora; a linha do tempo precisa do que
 * aconteceu, e nenhum dos dois se reconstrói a partir do outro.
 */
data class Event(
    val id: Long,
    val entryId: Long,
    val day: Long,
    val instant: Long,
    val type: EventType,
    val target: String,
    val languagePair: LanguagePair,
    /** Número da revisão em [EventType.CORRECT]/[EventType.INCORRECT], nível novo em [EventType.LEVELED_UP]. */
    val detail: String?,
)

enum class EventType {
    CAPTURED,
    CARD_READY,
    CORRECT,
    INCORRECT,
    LEVELED_UP;

    companion object {
        fun de(value: String): EventType = entries.firstOrNull { it.name == value } ?: CAPTURED
    }
}

/**
 * A maior sequência de dias seguidos que já houve, para o "11 melhor sequência".
 *
 * [dias] vem em ordem decrescente e sem repetição, como sai do banco.
 */
fun bestStreakOf(days: List<Long>): Int {
    if (days.isEmpty()) return 0
    var melhor = 1
    var current = 1
    for (indice in 1 until days.size) {
        if (days[indice] == days[indice - 1] - 1) current++ else current = 1
        if (current > melhor) melhor = current
    }
    return melhor
}
