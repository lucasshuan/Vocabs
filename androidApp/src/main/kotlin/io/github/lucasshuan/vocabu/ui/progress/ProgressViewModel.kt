package io.github.lucasshuan.vocabu.ui.progress

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.lucasshuan.vocabu.shared.AppContainer
import io.github.lucasshuan.vocabu.shared.domain.CourseSummary
import io.github.lucasshuan.vocabu.shared.domain.DailyActivity
import io.github.lucasshuan.vocabu.shared.domain.DailyQuota
import io.github.lucasshuan.vocabu.shared.domain.Entry
import io.github.lucasshuan.vocabu.shared.domain.Event
import io.github.lucasshuan.vocabu.shared.domain.LanguagePair
import io.github.lucasshuan.vocabu.shared.domain.MemoryLevel
import io.github.lucasshuan.vocabu.shared.domain.Scope
import io.github.lucasshuan.vocabu.shared.domain.Steps
import io.github.lucasshuan.vocabu.shared.enrolledCourses
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import java.time.temporal.TemporalAdjusters
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn

/**
 * One ViewModel for all three progress screens: they read the same thing in
 * different slices, and separate states would flicker while each redid its counts.
 */
data class ProgressState(
    val languagePair: LanguagePair = LanguagePair.DEFAULT,
    /**
     * Today's week, empty — the same drawing as a course with no words, so the
     * skeleton fills in instead of showing "0 of 10" for two frames.
     */
    val week: List<ProgressDay> = weekOf(LocalDate.now(), emptyList()),
    val month: LocalDate = LocalDate.now(),
    val dayStreak: Int = 0,
    val quota: DailyQuota = DailyQuota(done = 0, inQueue = 0),
    val words: List<Entry> = emptyList(),
    val events: List<Event> = emptyList(),
) {
    val total: Int get() = words.size

    /** By step: the number that does not change on its own overnight. */
    val byLevel: Map<MemoryLevel, List<Entry>>
        get() = words.groupBy { Steps.level(it.step) }

    val mastered: Int get() = byLevel[MemoryLevel.MASTERED]?.size ?: 0
    val familiar: Int get() = byLevel[MemoryLevel.FAMILIAR]?.size ?: 0
    val learning: Int get() = total - mastered - familiar

    /** The ones one correct answer from changing name. */
    val closeToLeveling: List<Entry>
        get() = words.filter { entry ->
            val step = entry.step
            Steps.hitsToLevelUp(step) == 1
        }
}

data class ProgressDay(
    val date: LocalDate,
    val reviews: Int,
    val today: Boolean,
    val future: Boolean,
)

/**
 * One course, not necessarily the open one: the screen opens from a Profile row,
 * and switching the open course to look at French would move Home's page and the
 * `+`'s destination. The course enters through [open] as a named [Scope.Course].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProgressViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = AppContainer.repository(app)
    private val preferences = AppContainer.preferences(app)

    /** Null until the route says which course; until then the open one applies. */
    private val course = MutableStateFlow<String?>(null)

    private val scope: Flow<Scope> = course.map { target ->
        target?.let(Scope::Course) ?: Scope.ActiveCourse
    }

    val state: StateFlow<ProgressState> = scope.flatMapLatest { crop ->
        // In two stages: `combine` is only typed up to five flows.
        val weekAndQuota = combine(
            repository.observeReviewSummary(crop),
            repository.observeActivity(84),
        ) { review, activity ->
            val today = LocalDate.now()
            ProgressState(
                week = weekOf(today, activity),
                month = today,
                // The streak counts any language, the quota one course: habit is
                // the person's, load is the subject's.
                dayStreak = review.dayStreak,
                quota = review.quota,
            )
        }

        combine(
            weekAndQuota,
            repository.observeReady(crop),
            repository.observeEvents(84, crop),
            preferences.observeLanguagePair(),
        ) { base, readyEntries, events, languagePair ->
            base.copy(
                languagePair = pairOf(crop, languagePair),
                words = readyEntries,
                events = events,
            )
        }
            // Clears on switch: otherwise English's numbers sit under the Spanish
            // flag for the length of a query, which is a wrong sentence rather
            // than a late one.
            .onStart { emit(ProgressState(languagePair = pairOf(crop, preferences.languagePair))) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressState())

    private fun pairOf(crop: Scope, activePair: LanguagePair) = LanguagePair(
        native = activePair.native,
        target = (crop as? Scope.Course)?.target ?: activePair.target,
    )

    /**
     * The course being looked at, not the open one — what the flag drawer does,
     * and the door the route enters through.
     */
    fun open(target: String?) {
        course.value = target?.takeIf { it.isNotBlank() }
    }

    /** Everyone enrolled, cards or not. */
    val courses: StateFlow<List<CourseSummary>> = enrolledCourses(repository, preferences)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Removing the last course would leave the app pageless. */
    val canRemove: StateFlow<Boolean> = preferences.observeCourses()
        .map { it.size > 1 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun removeCourse(target: String) = preferences.unenroll(target)
}

/** Monday first by choice, not by locale: a study streak is a work week. */
internal fun weekOf(today: LocalDate, activity: List<DailyActivity>): List<ProgressDay> {
    val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val byDay = activity.associate { it.day to it.reviews }
    return (0L until 7L).map { offset ->
        val date = monday.plusDays(offset)
        ProgressDay(
            date = date,
            reviews = byDay[date.toEpochDay() + JULIAN_DAY_OF_EPOCH] ?: 0,
            today = date == today,
            future = date.isAfter(today),
        )
    }
}

/**
 * The database resolves the local day in SQL (`julianday(...) + 0.5`), so the
 * turn of the day follows the device's timezone without common Kotlin needing a
 * date library. The offset to `java.time`'s epoch day is applied here only.
 */
private const val JULIAN_DAY_OF_EPOCH = 2_440_588L

/**
 * The locale is passed in, resolved in the composable: `Locale.getDefault()`
 * follows the device, which the in-app picker can differ from. Hand-written name
 * lists, the earlier approach, cannot follow a language setting at all.
 */
internal fun monthName(date: LocalDate, locale: Locale): String =
    date.month.getDisplayName(TextStyle.FULL, locale)

/** pt-BR returns "março" where en returns "March"; the header wants both capitalised. */
internal fun monthNameCapitalised(date: LocalDate, locale: Locale): String =
    monthName(date, locale).replaceFirstChar { it.titlecase(locale) }

internal fun weekdayLabels(locale: Locale): List<String> =
    (0L until 7L).map {
        DayOfWeek.MONDAY.plus(it).getDisplayName(TextStyle.SHORT, locale)
    }
