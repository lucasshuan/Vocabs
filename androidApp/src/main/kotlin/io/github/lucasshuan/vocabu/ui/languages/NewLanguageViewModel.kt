package io.github.lucasshuan.vocabu.ui.languages

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.lucasshuan.vocabu.contracts.Language
import io.github.lucasshuan.vocabu.contracts.Languages
import io.github.lucasshuan.vocabu.shared.AppContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class NewLanguageState(
    /** The "You already have" pills. */
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
     * The same list for both choices. Existing courses drop out, and so does the
     * native language: a course from a language to itself would produce cards
     * translating every word by itself. It holds both ways.
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
