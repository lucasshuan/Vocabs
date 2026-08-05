package io.github.lucasshuan.vocabu.shared.domain

/**
 * Memory strength: points from 0 to 100 that rise on a correct answer and decay
 * on their own.
 *
 * Two numbers, not one. If every word decayed at the same rate, one mastered
 * months ago would fall as fast as a new one — the exact advantage spaced
 * repetition exists to give. So [decayRate] shrinks on a hit and grows on a miss.
 *
 * Nothing here reads the clock; every function takes `now`. That stops the UI
 * inventing its own, which would let the card's bar and the review queue
 * disagree with no visible cause.
 */
data class Retention(
    val points: Double,
    val decayRate: Double,
    val lastInteraction: Long,
    val reviews: Int,
    val hits: Int = 0,
    val misses: Int = 0,
) {

    /** Not always [reviews]: those answered before the score existed have no outcome. */
    val answered: Int get() = hits + misses

    /** Null rather than zero: "never answered" and "got them all wrong" differ. */
    val hitRate: Double? get() = if (answered == 0) null else hits.toDouble() / answered

    fun pointsAt(now: Long): Double {
        // coerceAtLeast: a clock moving backwards (NTP, timezone change) would
        // otherwise produce a negative elapsed time and *award* points.
        val days = (now - lastInteraction).coerceAtLeast(0L) / MILLIS_PER_DAY
        return (points - decayRate * days).coerceIn(0.0, MAX_POINTS)
    }

    fun levelAt(now: Long): MemoryLevel = when {
        reviews == 0 -> MemoryLevel.NEW
        else -> when {
            pointsAt(now) < 30.0 -> MemoryLevel.LEARNING
            pointsAt(now) < 70.0 -> MemoryLevel.FAMILIAR
            else -> MemoryLevel.MASTERED
        }
    }

    fun needsReview(now: Long): Boolean = pointsAt(now) < REVIEW_THRESHOLD

    /** Millis until the word crosses the threshold; 0 if it already has. */
    fun nextReviewIn(now: Long): Long {
        val current = pointsAt(now)
        if (current < REVIEW_THRESHOLD) return 0L
        if (decayRate <= 0.0) return Long.MAX_VALUE
        return ((current - REVIEW_THRESHOLD) / decayRate * MILLIS_PER_DAY).toLong()
    }

    /**
     * A miss zeroes the points rather than lowering them to 30 or 50, because
     * `0 < 60` is always true: the word stays in the queue continuously until
     * it is answered right. No relearning flag, no extra state, no new column.
     */
    fun after(correct: Boolean, now: Long): Retention = Retention(
        points = if (correct) MAX_POINTS else 0.0,
        decayRate = if (correct) {
            (decayRate / HIT_DIVISOR).coerceIn(MIN_RATE, MAX_RATE)
        } else {
            (decayRate * MISS_MULTIPLIER).coerceIn(MIN_RATE, MAX_RATE)
        },
        lastInteraction = now,
        reviews = reviews + 1,
        hits = if (correct) hits + 1 else hits,
        misses = if (correct) misses else misses + 1,
    )

    companion object {
        const val MAX_POINTS = 100.0

        /** Every interval is `40 / decayRate`. */
        const val REVIEW_THRESHOLD = 60.0

        /** `40 / 40` = one day to the first review. */
        const val INITIAL_RATE = 40.0

        const val HIT_DIVISOR = 1.5

        /**
         * Halves the interval on a miss. The symmetric-looking 2.0 would give
         * 0.75x, so a miss would barely register; 3.0 makes one miss undo 2.7
         * hits, balancing at a 73% hit rate.
         */
        const val MISS_MULTIPLIER = 3.0

        /**
         * Caps the interval at ~67 days. This floor sets the steady-state daily
         * load (`N * 0.6 / 40`): 500 words is 7.5 cards a day. A lower floor
         * sounds better and is how a deck dies — the queue dries up.
         */
        const val MIN_RATE = 0.6

        /** Floors the interval at 16h, so the app never says "review now". */
        const val MAX_RATE = 60.0

        /**
         * Fractional days, never whole ones. With `floor`, a word at rate 40
         * reaches exactly 60 after 24h, and `60 < 60` is false — it would wait
         * another day, silently turning a 1-day interval into 2.
         */
        private const val MILLIS_PER_DAY = 86_400_000.0

        fun initial(now: Long) = Retention(
            points = MAX_POINTS,
            decayRate = INITIAL_RATE,
            lastInteraction = now,
            reviews = 0,
        )
    }
}

/** 0-30 learning, 30-70 familiar, 70-100 mastered, plus never-reviewed. */
enum class MemoryLevel {
    NEW,
    LEARNING,
    FAMILIAR,
    MASTERED,
}

data class Streak(
    val dayStreak: Int,
    val reviewedToday: Boolean,
)

/**
 * [days] arrives descending and without repeats (it is the table's primary key).
 *
 * A streak may anchor on **yesterday**: not having reviewed yet today does not
 * break it, since the day is not over. Only skipping a whole day does.
 */
fun streakOf(days: List<Long>, today: Long): Streak {
    val reviewedToday = days.firstOrNull() == today
    val start = when {
        reviewedToday -> today
        days.firstOrNull() == today - 1 -> today - 1
        else -> return Streak(dayStreak = 0, reviewedToday = false)
    }

    var expected = start
    var total = 0
    for (day in days) {
        if (day != expected) break
        total++
        expected--
    }
    return Streak(dayStreak = total, reviewedToday = reviewedToday)
}
