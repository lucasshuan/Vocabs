package io.github.lucasshuan.vocabu.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.lucasshuan.vocabu.shared.AppContainer
import io.github.lucasshuan.vocabu.shared.domain.CourseSummary
import io.github.lucasshuan.vocabu.shared.domain.Entry
import io.github.lucasshuan.vocabu.shared.domain.LanguagePair
import io.github.lucasshuan.vocabu.shared.domain.MemoryLevel
import io.github.lucasshuan.vocabu.shared.domain.Scope
import io.github.lucasshuan.vocabu.shared.domain.Steps
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * One carousel page — everything Home shows of a course.
 *
 * [summary] is the same object that feeds the strip's badge, not a second count:
 * the number on the English chip and the English card's "3 cooled off today" are
 * one sentence said twice, and disagreeing would be worse than either missing.
 */
data class HomePage(
    val summary: CourseSummary,
    val averageStrength: Int,
    /** How many fall due within 24h — the "Next 5 in 19h". */
    val nextIn24h: Int,
    val capturedToday: List<Entry>,
) {
    val target: String get() = summary.target
}

data class HomeState(
    val pages: List<HomePage> = emptyList(),
    val activeTarget: String = "",
    val native: String = "",
    val loaded: Boolean = false,
) {
    val courses: List<CourseSummary> get() = pages.map { it.summary }

    val activeIndex: Int get() = pages.indexOfFirst { it.target == activeTarget }.coerceAtLeast(0)

    /** With one course there is no strip and no carousel: nowhere to swipe. */
    val hasCarousel: Boolean get() = pages.size > 1
}

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = AppContainer.repository(app)
    private val preferences = AppContainer.preferences(app)

    /**
     * One read, grouped in memory: swiping the carousel must not fire a query,
     * and one read is also what makes the pages describe the same instant.
     */
    val state: StateFlow<HomeState> = combine(
        preferences.observeLanguagePair(),
        preferences.observeCourses(),
        repository.observeReady(Scope.All),
    ) { languagePair, enrolled, readyEntries ->
        val now = System.currentTimeMillis()
        val today = LocalDate.now()
        val byCourse = readyEntries.groupBy { it.languagePair.target }

        HomeState(
            pages = enrolled.map { target ->
                page(
                    languagePair = LanguagePair(native = languagePair.native, target = target),
                    entries = byCourse[target].orEmpty(),
                    now = now,
                    today = today,
                )
            },
            activeTarget = languagePair.target,
            native = languagePair.native,
            loaded = true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())

    private fun page(
        languagePair: LanguagePair,
        entries: List<Entry>,
        now: Long,
        today: LocalDate,
    ): HomePage {
        val misses = entries.mapNotNull { it.retention?.nextReviewIn(now) }
        return HomePage(
            summary = CourseSummary(
                target = languagePair.target,
                total = entries.size,
                // By step, as everywhere: memory strength decays between reads,
                // so the same total would differ between screens.
                mastered = entries.count { Steps.level(it.step) == MemoryLevel.MASTERED },
                inQueue = entries.count { it.needsReview(now) },
                nextInMillis = misses.filter { it > 0L }.minOrNull(),
            ),
            averageStrength = entries.mapNotNull { it.retention?.pointsAt(now) }.averageOrZero().toInt(),
            nextIn24h = misses.count { it in 1..ONE_DAY_IN_MILLIS },
            capturedToday = entries
                .filter { Instant.ofEpochMilli(it.createdAt).atZone(ZoneId.systemDefault()).toLocalDate() == today }
                .take(3),
        )
    }

    /** Swiping the carousel is switching course: review and the `+` follow. */
    fun openCourse(code: String) = preferences.openCourse(code)

    private companion object {
        const val ONE_DAY_IN_MILLIS = 86_400_000L
    }
}

private fun List<Double>.averageOrZero(): Double = if (isEmpty()) 0.0 else average()
