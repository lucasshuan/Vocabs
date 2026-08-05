package com.jean.vocabs.ui.saved

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jean.vocabs.shared.AppContainer
import com.jean.vocabs.shared.domain.Entry
import com.jean.vocabs.shared.domain.EntryStatus
import com.jean.vocabs.shared.domain.Scope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

/**
 * O que acabou de ser guardado, enquanto a IA trabalha.
 *
 * [totalDoCurso] é o estoque **depois** da captura — "26 fichas agora". O
 * número existe para fechar o ciclo: a pessoa acabou de acrescentar duas coisas
 * a alguma coisa, e o tamanho dessa coisa é o que dá sentido ao gesto.
 */
data class SavedState(
    val entries: List<Entry> = emptyList(),
    val courseTotal: Int = 0,
) {
    val target: String get() = entries.firstOrNull()?.languagePair?.target.orEmpty()

    /** Enquanto houver ficha em construção há o que olhar, e a tela não se fecha sozinha. */
    val working: Boolean
        get() = entries.any { it.status == EntryStatus.PENDING || it.status == EntryStatus.GENERATING }
}

@OptIn(ExperimentalCoroutinesApi::class)
class SavedViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = AppContainer.repository(app)
    private val ids = MutableStateFlow<List<Long>>(emptyList())

    val state: StateFlow<SavedState> = ids.flatMapLatest { list ->
        combine(
            repository.observeEntries(list),
            repository.observeReady(Scope.All),
        ) { entries, readyEntries ->
            val course = entries.firstOrNull()?.languagePair
            SavedState(
                entries = entries,
                courseTotal = readyEntries.count { it.languagePair == course },
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SavedState())

    fun follow(list: List<Long>) {
        if (ids.value != list) ids.value = list
    }
}
