package com.jean.vocabs.ui.inbox

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jean.vocabs.shared.AppContainer
import com.jean.vocabs.shared.domain.Entrada
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class InboxViewModel(app: Application) : AndroidViewModel(app) {

    private val repositorio = AppContainer.repositorio(app)

    /** Mais antigo primeiro: o que está parado há mais tempo aparece no topo. */
    val entradas: StateFlow<List<Entrada>> = repositorio.observarInbox()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * O número que vai no selo da aba.
     *
     * O documento de métricas chama o backlog do inbox de "provavelmente o alerta
     * mais importante de todos": se ele só cresce, você captura mais rápido do
     * que processa. Deixar isso visível o tempo todo é a versão barata do alerta.
     */
    val total: StateFlow<Int> = repositorio.observarInbox()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** Usado quando a geração falhou (servidor fora do ar, rede caiu). */
    fun tentarDeNovo(id: Long) {
        AppContainer.escopo.launch { repositorio.gerarFicha(id) }
    }

    fun excluir(id: Long) {
        AppContainer.escopo.launch { repositorio.excluir(id) }
    }
}
