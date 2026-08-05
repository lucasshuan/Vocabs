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

    fun observe(id: Long): StateFlow<Entry?> {
        this.id = id
        return repository.observeById(id)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    }

    /** Memory strength already resolved for now — the clock lives in the repository. */
    fun observeMemory(id: Long): StateFlow<RetentionNow?> =
        repository.observeRetention(id)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Used when generation failed (server down, network dropped). */
    fun tryAgain() {
        AppContainer.scope.launch { repository.generateCard(id) }
    }

    fun delete() {
        AppContainer.scope.launch { repository.delete(id) }
    }
}
