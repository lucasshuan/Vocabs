package io.github.lucasshuan.vocabu.shared.domain

/**
 * Points 0-100, decaying with time. Two numbers rather than one: at a shared
 * decay rate a word mastered months ago falls as fast as a new one, which is
 * the advantage spaced repetition exists to give.
 *
 * Nothing here reads the clock — every function takes `now`, so the card's bar
 * and the review queue cannot disagree.
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
        // coerceAtLeast: a clock moving backwards (NTP, timezone) would otherwise
        // give negative elapsed time and *award* points.
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
     * A miss zeroes the points rather than halving them: `0 < 60` holds, so the
     * word stays queued until answered right. No relearning flag, no new column.
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
         * Halves the interval. The symmetric-looking 2.0 gives 0.75x, barely
         * registering; 3.0 makes one miss undo 2.7 hits, balancing at 73% hits.
         */
        const val MISS_MULTIPLIER = 3.0

        /**
         * Caps the interval at ~67 days, which sets the steady-state daily load
         * (`N * 0.6 / 40`): 500 words is 7.5 cards a day. Lower dries the queue up.
         */
        const val MIN_RATE = 0.6

        /** Floors the interval at 16h, so the app never says "review now". */
        const val MAX_RATE = 60.0

        /**
         * A Double, so days stay fractional. Floored, a word at rate 40 hits
         * exactly 60 after 24h and `60 < 60` is false — a 1-day interval becomes 2.
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
 * [days] arrives descending and without repeats (it is the primary key).
 *
 * A streak may anchor on yesterday: the day is not over, so nothing reviewed yet
 * today does not break it. Only a whole day skipped does.
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
