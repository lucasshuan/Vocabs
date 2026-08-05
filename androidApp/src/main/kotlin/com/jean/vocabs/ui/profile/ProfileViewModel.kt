package com.jean.vocabs.ui.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jean.vocabs.shared.AppContainer
import com.jean.vocabs.shared.domain.AiUsage
import com.jean.vocabs.shared.domain.CourseSummary
import com.jean.vocabs.shared.domain.LanguagePair
import com.jean.vocabs.shared.domain.Scope
import com.jean.vocabs.shared.enrolledCourses
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Profile: the totals first, the per-language breakdown after.
 *
 * Streak and stock are habit, and habit belongs to the person rather than the
 * course — someone who studied Spanish yesterday and French today studied two
 * days running. So the three numbers at the top sum everything.
 */
data class ProfileState(
    val languagePair: LanguagePair = LanguagePair.DEFAULT,
    /** Every enrolled course, in strip order — empty ones included. */
    val courses: List<CourseSummary> = emptyList(),
    val dayStreak: Int = 0,
    val aiUsage: AiUsage = AiUsage("", 0),
) {
    val totalCards: Int get() = courses.sumOf { it.total }
    val totalMastered: Int get() = courses.sumOf { it.mastered }
}

class ProfileViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = AppContainer.repository(app)
    private val preferences = AppContainer.preferences(app)

    val state: StateFlow<ProfileState> = combine(
        enrolledCourses(repository, preferences),
        preferences.observeLanguagePair(),
        // Across every course: the day streak counts activity in any language.
        repository.observeReviewSummary(Scope.All),
        repository.observeAiUsage(),
    ) { courseList, languagePair, review, aiUsage ->
        ProfileState(
            languagePair = languagePair,
            courses = courseList,
            dayStreak = review.dayStreak,
            aiUsage = aiUsage,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileState())

    /** How many languages are still choosable — New language's "37 languages". */
    val available: StateFlow<Int> = preferences.observeCourses()
        .map { enrolled -> com.jean.vocabs.contracts.Languages.CATALOG.count { it.code !in enrolled } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
}
