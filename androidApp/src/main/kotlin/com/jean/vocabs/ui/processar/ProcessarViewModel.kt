package com.jean.vocabs.ui.processar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jean.vocabs.shared.AppContainer
import com.jean.vocabs.shared.domain.AlvoSelecionado
import com.jean.vocabs.shared.domain.Captura
import com.jean.vocabs.shared.domain.Entrada
import com.jean.vocabs.shared.domain.duplicataDeAlvo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProcessarViewModel(app: Application) : AndroidViewModel(app) {
    private val repositorio = AppContainer.repositorio(app)
    private val alvo = MutableStateFlow("")

    val duplicata: StateFlow<Entrada?> = combine(
        repositorio.observarProntas(),
        repositorio.observarInbox(),
        alvo,
    ) { prontas, inbox, texto -> duplicataDeAlvo(texto, prontas + inbox) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun observar(id: Long): StateFlow<Captura?> = repositorio.observarCapturaPorId(id)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun procurarDuplicata(texto: String) {
        alvo.value = texto
    }

    fun confirmar(id: Long, trecho: String, alvos: List<AlvoSelecionado>) {
        AppContainer.escopo.launch {
            val ids = repositorio.confirmarCaptura(id, trecho, alvos)
            repositorio.gerarFichas(ids)
        }
    }

    fun excluir(id: Long) {
        AppContainer.escopo.launch { repositorio.excluirCaptura(id) }
    }
}
