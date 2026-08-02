package com.jean.vocabs.ui.inicio

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jean.vocabs.shared.AppContainer
import com.jean.vocabs.shared.domain.FormatoCaptura
import com.jean.vocabs.shared.domain.ResumoRevisao
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class InicioEstado(
    val totalPalavras: Int = 0,
    val revisao: ResumoRevisao? = null,
    val pendentes: Int = 0,
)

class InicioViewModel(app: Application) : AndroidViewModel(app) {

    private val repositorio = AppContainer.repositorio(app)

    val estado: StateFlow<InicioEstado> =
        combine(
            repositorio.observarProntas(),
            repositorio.observarResumoDeRevisao(),
            repositorio.observarInbox(),
        ) { prontas, revisao, pendentes ->
            InicioEstado(
                totalPalavras = prontas.size,
                revisao = revisao,
                pendentes = pendentes.size,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InicioEstado())

    /**
     * Foto e áudio entram como rascunho e esperam transcrição — não chamam a IA.
     *
     * No escopo da aplicação e não no viewModelScope: capturar daqui troca de
     * aba na hora, e um escopo preso a esta tela cancelaria a gravação do banco
     * no meio do caminho.
     */
    fun salvarMidia(formato: FormatoCaptura, caminho: String) {
        AppContainer.escopo.launch {
            repositorio.capturarMidia(formato = formato, caminho = caminho, origem = null)
        }
    }
}
