package com.jean.vocabs.ui.ficha

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jean.vocabs.shared.AppContainer
import com.jean.vocabs.shared.domain.Entrada
import com.jean.vocabs.shared.domain.RetencaoAgora
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FichaViewModel(app: Application) : AndroidViewModel(app) {

    private val repositorio = AppContainer.repositorio(app)

    private var id: Long = 0L

    fun observar(id: Long): StateFlow<Entrada?> {
        this.id = id
        return repositorio.observarPorId(id)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    }

    /** A força de memória já resolvida para agora — o relógio mora no repositório. */
    fun observarMemoria(id: Long): StateFlow<RetencaoAgora?> =
        repositorio.observarRetencao(id)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Usado quando a geração falhou (servidor fora do ar, rede caiu). */
    fun tentarDeNovo() {
        AppContainer.escopo.launch { repositorio.gerarFicha(id) }
    }
}
