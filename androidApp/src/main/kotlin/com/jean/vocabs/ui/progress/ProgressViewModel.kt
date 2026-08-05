package com.jean.vocabs.ui.progress

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jean.vocabs.shared.AppContainer
import com.jean.vocabs.shared.domain.CourseSummary
import com.jean.vocabs.shared.domain.DailyActivity
import com.jean.vocabs.shared.domain.DailyQuota
import com.jean.vocabs.shared.domain.Entry
import com.jean.vocabs.shared.domain.Event
import com.jean.vocabs.shared.domain.LanguagePair
import com.jean.vocabs.shared.domain.MemoryLevel
import com.jean.vocabs.shared.domain.Scope
import com.jean.vocabs.shared.domain.Steps
import com.jean.vocabs.shared.enrolledCourses
import java.time.DayOfWeek
import java.time.LocalDate
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
 * The state of the three progress screens.
 *
 * One ViewModel for all three because they read the same thing in different
 * slices: the week and the quota appear in two, the word stock in two.
 * Independent states would make the numbers flicker while each redid its counts.
 */
data class ProgressState(
    val languagePair: LanguagePair = LanguagePair.DEFAULT,
    /**
     * Today's week, empty, while the database has not answered.
     *
     * It is the same drawing as a course with no words, which is why it can be
     * the initial state: the dashed skeleton appears in the right place and
     * fills in, rather than showing "0 of 10" for two frames.
     */
    val semana: List<ProgressDay> = weekOf(LocalDate.now(), emptyList()),
    val month: String = monthName(LocalDate.now()),
    val dayStreak: Int = 0,
    val quota: DailyQuota = DailyQuota(done = 0, inQueue = 0),
    val words: List<Entry> = emptyList(),
    val events: List<Event> = emptyList(),
) {
    val total: Int get() = words.size

    /** Counted by step: the number that does not change on its own overnight. */
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

/** One day of the week strip, with its number and reviews already resolved. */
data class ProgressDay(
    val data: LocalDate,
    val reviews: Int,
    val today: Boolean,
    val future: Boolean,
)

/**
 * The progress of **one** course, not necessarily the open one.
 *
 * The screen opens from a Profile row, and Profile lists all three languages.
 * Changing the open course just to look at French would move Home's page and the
 * `+`'s destination — an effect nobody asked for by tapping a row. So the course
 * enters through [open] and becomes a named [Scope.Course].
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
        /** In two stages: `combine` only has a typed overload up to five flows. */
        val weekAndQuota = combine(
            repository.observeReviewSummary(crop),
            repository.observeActivity(84),
        ) { review, activity ->
            val today = LocalDate.now()
            ProgressState(
                semana = weekOf(today, activity),
                month = monthName(today),
                // The streak counts activity in any language; the quota belongs
                // to the course. Different questions: habit is the person's, load
                // is the subject's.
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
            // Switching course clears the screen before the database answers.
            //
            // Without this, picking Spanish in the drawer would leave English's
            // numbers under the Spanish flag for the length of a query — and
            // "Today's quota in Spanish" over English's "6 of 10" is a wrong
            // sentence, not a late one. The skeleton is the only honest state
            // while the answer is in flight.
            .onStart { emit(ProgressState(languagePair = pairOf(crop, preferences.languagePair))) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressState())

    /** The slice's course, with the reader's native language. */
    private fun pairOf(crop: Scope, activePair: LanguagePair) = LanguagePair(
        native = activePair.native,
        target = (crop as? Scope.Course)?.target ?: activePair.target,
    )

    /**
     * Changes the course being **looked at**, not the open one.
     *
     * This is what the flag drawer does, and the same door the route enters
     * through: it reloads both cards without touching Home's page or the `+`.
     */
    fun open(target: String?) {
        course.value = target?.takeIf { it.isNotBlank() }
    }

    /** The drawer's courses: everyone enrolled, including those with no cards. */
    val courses: StateFlow<List<CourseSummary>> = enrolledCourses(repository, preferences)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** How many courses exist — removing the last would leave the app pageless. */
    val canRemove: StateFlow<Boolean> = preferences.observeCourses()
        .map { it.size > 1 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun removeCourse(target: String) = preferences.unenroll(target)
}

/**
 * The current week, Monday to Sunday.
 *
 * Monday first is a deliberate design choice, not a locale artifact: a study
 * streak is a work week.
 */
internal fun weekOf(today: LocalDate, activity: List<DailyActivity>): List<ProgressDay> {
    val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val byDay = activity.associate { it.day to it.reviews }
    return (0L until 7L).map { offset ->
        val data = monday.plusDays(offset)
        ProgressDay(
            data = data,
            reviews = byDay[data.toEpochDay() + JULIAN_DAY_OF_EPOCH] ?: 0,
            today = data == today,
            future = data.isAfter(today),
        )
    }
}

/**
 * The gap between the Julian day the database stores and `java.time`'s epoch day.
 *
 * The database resolves the local day in SQL (`julianday(...) + 0.5`) so the turn
 * of the day follows the device's timezone without common Kotlin needing a date
 * library. Readers convert once here instead of spreading the addition around.
 */
private const val JULIAN_DAY_OF_EPOCH = 2_440_588L

private val MONTHS = listOf(
    "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
    "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro",
)

/** Hand-written because `Locale("pt","BR")` depends on the device's ICU data. */
internal fun monthName(data: LocalDate): String = MONTHS[data.monthValue - 1]

internal val WEEKDAY_LABELS = listOf("seg", "ter", "qua", "qui", "sex", "sáb", "dom")
