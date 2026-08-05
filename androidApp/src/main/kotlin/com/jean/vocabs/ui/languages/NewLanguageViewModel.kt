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
    val jaTem: List<Language> = emptyList(),
    val native: String = Languages.NATIVO_PADRAO,
)

class NewLanguageViewModel(app: Application) : AndroidViewModel(app) {
    private val preferences = AppContainer.preferences(app)

    val estado: StateFlow<NewLanguageState> = combine(
        preferences.observeCourses(),
        preferences.observeLanguagePair(),
    ) { courses, languagePair ->
        NewLanguageState(jaTem = courses.mapNotNull(Languages::de), native = languagePair.native)
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
    fun disponiveis(): List<Language> {
        val current = estado.value
        val ocupados = current.jaTem.map { it.code } + current.native
        return Languages.CATALOGO.filter { it.code !in ocupados }
    }

    fun enroll(codigo: String) = preferences.enroll(codigo)

    fun trocarNativo(codigo: String) {
        preferences.native = codigo
    }
}
