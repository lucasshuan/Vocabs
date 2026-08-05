package com.jean.vocabs.ui.languages

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jean.vocabs.contracts.Language
import com.jean.vocabs.contracts.Languages
import com.jean.vocabs.shared.AppContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class NewLanguageState(
    /** Os cursos que já existem — as pílulas de "Você já tem". */
    val alreadyHas: List<Language> = emptyList(),
    val native: String = Languages.DEFAULT_NATIVE,
)

class NewLanguageViewModel(app: Application) : AndroidViewModel(app) {
    private val preferences = AppContainer.preferences(app)

    val state: StateFlow<NewLanguageState> = combine(
        preferences.observeCourses(),
        preferences.observeLanguagePair(),
    ) { courses, languagePair ->
        NewLanguageState(alreadyHas = courses.mapNotNull(Languages::of), native = languagePair.native)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NewLanguageState())

    /**
     * O que sobra para escolher — e é a mesma lista nas duas escolhas.
     *
     * Saem os cursos que já existem, porque repeti-los não cria nada, e sai o
     * idioma de partida atual, porque um curso de um idioma para ele mesmo
     * produziria fichas que traduzem cada palavra por ela mesma. Vale nos dois
     * sentidos: adotar como partida um idioma que se está aprendendo teria o
     * mesmo efeito, visto do outro lado.
     */
    fun available(): List<Language> {
        val current = state.value
        val taken = current.alreadyHas.map { it.code } + current.native
        return Languages.CATALOG.filter { it.code !in taken }
    }

    fun enroll(code: String) = preferences.enroll(code)

    fun switchNative(code: String) {
        preferences.native = code
    }
}
