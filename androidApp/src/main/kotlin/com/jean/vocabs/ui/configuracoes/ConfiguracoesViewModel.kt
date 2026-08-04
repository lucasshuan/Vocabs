package com.jean.vocabs.ui.configuracoes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jean.vocabs.shared.AppContainer
import com.jean.vocabs.shared.PreferenciaDeTema
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ConfiguracoesViewModel(app: Application) : AndroidViewModel(app) {
    private val preferencias = AppContainer.preferencias(app)
    private val repositorio = AppContainer.repositorio(app)

    val tema: StateFlow<PreferenciaDeTema> = preferencias.observarTema()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), preferencias.tema)

    /**
     * O idioma-base, observado e não lido uma vez.
     *
     * Trocá-lo abre a tela de escolha por cima desta e volta para cá: sem o
     * fluxo, a linha continuaria mostrando o idioma antigo até a Configurações
     * ser recriada — e a única prova de que a troca pegou é justamente essa
     * linha.
     */
    val nativo: StateFlow<String> = preferencias.observarNativo()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), preferencias.nativo)

    private val _exportando = MutableStateFlow(false)
    val exportando: StateFlow<Boolean> = _exportando.asStateFlow()

    fun escolherTema(valor: PreferenciaDeTema) {
        preferencias.tema = valor
    }

    fun exportar(aoPronto: (File) -> Unit, aoErro: (String) -> Unit) {
        if (_exportando.value) return
        viewModelScope.launch {
            _exportando.value = true
            runCatching {
                ExportadorVocabu.criar(getApplication(), repositorio.dadosParaExportacao())
            }.onSuccess(aoPronto).onFailure { aoErro(it.message ?: "Não foi possível exportar os dados.") }
            _exportando.value = false
        }
    }
}
