package com.jean.vocabs.ui.processar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jean.vocabs.shared.AppContainer
import com.jean.vocabs.shared.domain.Entrada
import com.jean.vocabs.shared.domain.duplicataDeAlvo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private data class BuscaDuplicata(
    val alvo: String = "",
    val ignorarId: Long? = null,
)

class ProcessarViewModel(app: Application) : AndroidViewModel(app) {

    private val repositorio = AppContainer.repositorio(app)
    private val buscaDuplicata = MutableStateFlow(BuscaDuplicata())

    val duplicata: StateFlow<Entrada?> =
        combine(
            repositorio.observarProntas(),
            repositorio.observarInbox(),
            buscaDuplicata,
        ) { prontas, inbox, busca ->
            duplicataDeAlvo(
                alvo = busca.alvo,
                entradas = prontas + inbox,
                ignorarId = busca.ignorarId,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun observar(id: Long): StateFlow<Entrada?> =
        repositorio.observarPorId(id)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun procurarDuplicata(alvo: String, ignorarId: Long) {
        buscaDuplicata.value = BuscaDuplicata(alvo = alvo, ignorarId = ignorarId)
    }

    /**
     * Fecha o ciclo do rascunho: grava a transcrição e manda gerar.
     *
     * No escopo da aplicação porque a tela fecha na hora — mesmo motivo da
     * captura de texto.
     */
    fun transcrever(id: Long, trecho: String, alvo: String, origem: String) {
        AppContainer.escopo.launch {
            repositorio.transcrever(
                id = id,
                trecho = trecho,
                alvo = alvo,
                origem = origem.ifBlank { null },
            )
            repositorio.gerarFicha(id)
        }
    }

    fun excluir(id: Long) {
        AppContainer.escopo.launch { repositorio.excluir(id) }
    }
}
