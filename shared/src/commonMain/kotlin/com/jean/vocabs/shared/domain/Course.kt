package com.jean.vocabs.shared.domain

import com.jean.vocabs.contracts.Languages

/**
 * Catalog codes rather than [Languages] objects: the pair is stored and compared
 * constantly, and a stable code survives the catalog renaming something.
 */
data class LanguagePair(
    val native: String,
    val target: String,
) {
    companion object {
        val DEFAULT = LanguagePair(native = Languages.DEFAULT_NATIVE, target = Languages.DEFAULT_TARGET)
    }
}

/**
 * [mastered] counts steps, not points: memory strength decays on its own, so
 * counting it would change the number every hour. What the strip should show is
 * how far someone climbed, and that does not drop while they sleep.
 */
data class CourseSummary(
    val languagePair: LanguagePair,
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
 * Three states, not four. [Empty] is a new course and differs from [UpToDate],
 * which is an achievement — writing a zero where the tick goes would turn the
 * strip's only good news into a scoreboard of nothing done.
 */
sealed interface CourseBadge {
    data class Review(val count: Int) : CourseBadge
    data object UpToDate : CourseBadge
    data object Empty : CourseBadge
}

/**
 * The five-step ladder behind the "What's left" screen.
 *
 * Deliberately not [MemoryLevel]: memory strength answers "how much do you
 * remember **now**" and so decays, while a step answers "how far did you get"
 * and only moves when a card is answered. A progress bar that walks backwards
 * while someone sleeps does not say what is left to do.
 *
 * The step is derived from the decay rate, which is already the hit history
 * compressed into one number — nothing extra is stored.
 */
object Steps {
    const val TOTAL = 5

    /**
     * Rate boundaries: 40, 26.7, 17.8, 11.9, 7.9 — each the previous divided by
     * one hit, so climbing a step is literally answering right once more.
     */
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
 * [total] is not a chosen goal — it is what decay asked for today, done plus
 * still queued. A fixed goal would lie both ways: unreachable on the day thirty
 * words come due together, already met on a day with nothing to review.
 */
data class DailyQuota(
    val done: Int,
    val inQueue: Int,
) {
    val total: Int get() = done + inQueue
    val met: Boolean get() = inQueue == 0
    val fraction: Float get() = if (total == 0) 1f else (done.toFloat() / total).coerceIn(0f, 1f)
}

/**
 * Retention holds only the state of now; the timeline needs what happened.
 * Neither reconstructs the other.
 */
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
