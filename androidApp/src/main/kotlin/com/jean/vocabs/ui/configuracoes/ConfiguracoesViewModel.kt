package com.jean.vocabs.ui.configuracoes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jean.vocabs.shared.AppContainer
import com.jean.vocabs.shared.PreferenciaDeTema
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class ConfiguracoesViewModel(app: Application) : AndroidViewModel(app) {
    private val preferencias = AppContainer.preferencias(app)

    val tema: StateFlow<PreferenciaDeTema> = preferencias.observarTema()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), preferencias.tema)

    fun escolherTema(valor: PreferenciaDeTema) {
        preferencias.tema = valor
    }
}
