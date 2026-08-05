package com.jean.vocabs.ui.idiomas

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

data class NovoIdiomaEstado(
    /** Os cursos que já existem — as pílulas de "Você já tem". */
    val jaTem: List<Language> = emptyList(),
    val nativo: String = Languages.NATIVO_PADRAO,
)

class NovoIdiomaViewModel(app: Application) : AndroidViewModel(app) {
    private val preferencias = AppContainer.preferencias(app)

    val estado: StateFlow<NovoIdiomaEstado> = combine(
        preferencias.observarCursos(),
        preferencias.observarPar(),
    ) { cursos, par ->
        NovoIdiomaEstado(jaTem = cursos.mapNotNull(Languages::de), nativo = par.nativo)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NovoIdiomaEstado())

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
        val atual = estado.value
        val ocupados = atual.jaTem.map { it.code } + atual.nativo
        return Languages.CATALOGO.filter { it.code !in ocupados }
    }

    fun matricular(codigo: String) = preferencias.matricular(codigo)

    fun trocarNativo(codigo: String) {
        preferencias.nativo = codigo
    }
}
