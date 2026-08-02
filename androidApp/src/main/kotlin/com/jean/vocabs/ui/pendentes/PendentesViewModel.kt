package com.jean.vocabs.ui.pendentes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jean.vocabs.shared.AppContainer
import com.jean.vocabs.shared.domain.Captura
import com.jean.vocabs.shared.domain.Entrada
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PendentesEstado(
    val capturas: List<Captura> = emptyList(),
    val fichas: List<Entrada> = emptyList(),
) {
    val total: Int get() = capturas.size + fichas.size
}

class PendentesViewModel(app: Application) : AndroidViewModel(app) {
    private val repositorio = AppContainer.repositorio(app)

    val estado: StateFlow<PendentesEstado> = combine(
        repositorio.observarCapturasPendentes(),
        repositorio.observarInbox(),
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
