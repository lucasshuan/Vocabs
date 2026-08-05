package com.jean.vocabs.ui.card

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jean.vocabs.shared.AppContainer
import com.jean.vocabs.shared.domain.Entry
import com.jean.vocabs.shared.domain.RetentionNow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CardViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = AppContainer.repository(app)

    private var id: Long = 0L

    fun observar(id: Long): StateFlow<Entry?> {
        this.id = id
        return repository.observeById(id)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    }

    /** A força de memória já resolvida para agora — o relógio mora no repositório. */
    fun observarMemoria(id: Long): StateFlow<RetentionNow?> =
        repository.observeRetention(id)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Usado quando a geração falhou (servidor fora do ar, rede caiu). */
    fun tentarDeNovo() {
        AppContainer.scope.launch { repository.generateCard(id) }
    }

    fun excluir() {
        AppContainer.scope.launch { repository.excluir(id) }
    }
}
