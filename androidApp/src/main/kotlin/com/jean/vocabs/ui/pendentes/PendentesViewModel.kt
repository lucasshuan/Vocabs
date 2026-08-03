package com.jean.vocabs.ui.pendentes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jean.vocabs.shared.AppContainer
import com.jean.vocabs.shared.domain.Captura
import com.jean.vocabs.shared.domain.Entrada
import com.jean.vocabs.shared.domain.Escopo
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Pendentes é uma fila, não uma notificação por curso.
 *
 * Ela nunca é recortada por idioma — nem pelo curso aberto, nem por um filtro
 * que tenha ficado ligado da visita passada. O que chega aqui é trabalho parado,
 * e trabalho parado num idioma não deixa de existir porque a pessoa foi estudar
 * outro. O selo da aba conta tudo pela mesma razão.
 */
data class PendentesEstado(
    val capturas: List<Captura> = emptyList(),
    val fichas: List<Entrada> = emptyList(),
) {
    val total: Int get() = capturas.size + fichas.size

    /** Quantas capturas cruas por idioma — o número que cada chip de filtro mostra. */
    val porIdioma: Map<String, Int>
        get() = (capturas.map { it.par.alvo } + fichas.map { it.par.alvo })
            .groupingBy { it }
            .eachCount()
}

class PendentesViewModel(app: Application) : AndroidViewModel(app) {
    private val repositorio = AppContainer.repositorio(app)

    val estado: StateFlow<PendentesEstado> = combine(
        repositorio.observarCapturasPendentes(Escopo.Todos),
        repositorio.observarInbox(Escopo.Todos),
        ::PendentesEstado,
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PendentesEstado())

    val total: StateFlow<Int> = estado.map { it.total }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun tentarDeNovo(id: Long) {
        AppContainer.escopo.launch { repositorio.gerarFicha(id) }
    }

    fun excluirFicha(id: Long) {
        AppContainer.escopo.launch { repositorio.excluir(id) }
    }

    fun excluirCaptura(id: Long) {
        AppContainer.escopo.launch { repositorio.excluirCaptura(id) }
    }
}
