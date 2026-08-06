package io.github.lucasshuan.vocabu.shared.domain

import io.github.lucasshuan.vocabu.contracts.Languages

/** Codes, not [Languages] objects: a code is what is stored, and survives a rename. */
data class LanguagePair(
    val native: String,
    val target: String,
) {
    companion object {
        val DEFAULT = LanguagePair(native = Languages.DEFAULT_NATIVE, target = Languages.DEFAULT_TARGET)
    }
}

/**
 * [mastered] counts steps, not points: points decay, so the number would change
 * every hour.
 */
data class CourseSummary(
    /**
     * The target alone, never the pair. Keying this by the pair made switching
     * native language stop matching every stored card, and the totals read zero.
     */
    val target: String,
    val total: Int,
    val mastered: Int,
    val inQueue: Int = 0,
    /** Millis until this course's next card is due. Null when none is. */
    val nextInMillis: Long? = null,
) {
    val badge: CourseBadge
        get() = when {
            inQueue > 0 -> CourseBadge.Review(inQueue)
            nextInMillis != null -> CourseBadge.UpToDate
            else -> CourseBadge.Empty
        }
}

/**
 * [Empty] is not [UpToDate]: a zero where the tick goes turns the strip's only
 * good news into a scoreboard of nothing done.
 */
sealed interface CourseBadge {
    data class Review(val count: Int) : CourseBadge
    data object UpToDate : CourseBadge
    data object Empty : CourseBadge
}

/**
 * Not [MemoryLevel]: points decay, so a bar built on them walks backwards while
 * someone sleeps. A step moves only on an answer.
 *
 * Derived from the decay rate — already the hit history in one number, so
 * nothing extra is stored.
 */
object Steps {
    const val TOTAL = 5

    /** 40, 26.7, 17.8, 11.9, 7.9 — one hit apart, so a step is one right answer. */
    private val LIMITS: List<Double> = generateSequence(Retention.INITIAL_RATE) { it / Retention.HIT_DIVISOR }
        .take(TOTAL)
        .toList()

    /** 1 to [TOTAL]. A never-reviewed word sits on the first. */
    fun of(retention: Retention?): Int {
        if (retention == null || retention.reviews == 0) return 1
        val reached = LIMITS.count { retention.decayRate <= it + TOLERANCE }
        return reached.coerceIn(1, TOTAL)
    }

    /** Shares [MemoryLevel]'s names on purpose: two scales is already the limit. */
    fun level(step: Int): MemoryLevel = when {
        step >= TOTAL -> MemoryLevel.MASTERED
        step == TOTAL - 1 -> MemoryLevel.FAMILIAR
        else -> MemoryLevel.LEARNING
    }

    /** Hits left before the step changes name. Zero at the top. */
    fun hitsToLevelUp(step: Int): Int {
        val current = level(step)
        val next = (step..TOTAL).firstOrNull { level(it) != current } ?: return 0
        return next - step
    }

    /** A double comparison that survived five consecutive divisions. */
    private const val TOLERANCE = 1e-9
}

/**
 * [total] is what decay asked for today, not a goal. A fixed goal lies both
 * ways: unreachable when thirty words come due together, already met on an
 * empty day.
 */
data class DailyQuota(
    val done: Int,
    val inQueue: Int,
) {
    val total: Int get() = done + inQueue
    val met: Boolean get() = inQueue == 0
    val fraction: Float get() = if (total == 0) 1f else (done.toFloat() / total).coerceIn(0f, 1f)
}

/** Retention holds only now; neither it nor the timeline reconstructs the other. */
data class Event(
    val id: Long,
    val entryId: Long,
    val day: Long,
    val instant: Long,
    val type: EventType,
    val target: String,
    val languagePair: LanguagePair,
    /** Review number on CORRECT/INCORRECT, new level on LEVELED_UP. */
    val detail: String?,
)

enum class EventType {
    CAPTURED,
    CARD_READY,
    CORRECT,
    INCORRECT,
    LEVELED_UP;

    companion object {
        fun of(value: String): EventType = entries.firstOrNull { it.name == value } ?: CAPTURED
    }
}

/** [days] arrives descending and without repeats, as it comes out of the database. */
fun bestStreakOf(days: List<Long>): Int {
    if (days.isEmpty()) return 0
    var best = 1
    var current = 1
    for (index in 1 until days.size) {
        if (days[index] == days[index - 1] - 1) current++ else current = 1
        if (current > best) best = current
    }
    return best
}
