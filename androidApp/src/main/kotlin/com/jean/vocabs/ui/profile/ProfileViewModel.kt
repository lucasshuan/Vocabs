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
 * A tela Você: o total primeiro, a quebra por idioma depois.
 *
 * Sequência e estoque são hábito, e hábito é da pessoa e não do curso — quem
 * estudou espanhol ontem e francês hoje estudou dois dias seguidos. Por isso os
 * três números do topo somam tudo, e a lista logo abaixo é que reparte.
 */
data class ProfileState(
    val languagePair: LanguagePair = LanguagePair.DEFAULT,
    /** Todos os cursos matriculados, na ordem da faixa — inclusive os vazios. */
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
        // De todos os cursos: a sequência de dias conta atividade em qualquer
        // idioma, e é o único número desta tela que já era assim antes.
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

    /** Quantos idiomas ainda dá para escolher — o "37 idiomas" da tela Novo idioma. */
    val available: StateFlow<Int> = preferences.observeCourses()
        .map { enrolled -> com.jean.vocabs.contracts.Languages.CATALOG.count { it.code !in enrolled } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
}
