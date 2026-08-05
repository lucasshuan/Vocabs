package io.github.lucasshuan.vocabu.ui.saved

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.lucasshuan.vocabu.shared.AppContainer
import io.github.lucasshuan.vocabu.shared.domain.Entry
import io.github.lucasshuan.vocabu.shared.domain.EntryStatus
import io.github.lucasshuan.vocabu.shared.domain.Scope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

/**
 * What was just saved, while the AI works.
 *
 * [courseTotal] is the stock **after** the capture — "26 cards now". The number
 * closes the loop: something was just added to something, and the size of that
 * something is what gives the gesture meaning.
 */
data class SavedState(
    val entries: List<Entry> = emptyList(),
    val courseTotal: Int = 0,
) {
    val target: String get() = entries.firstOrNull()?.languagePair?.target.orEmpty()

    /** While a card is still being built there is something to watch. */
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
            // Counted by target: the pair a card was generated in does not decide
            // which course it belongs to.
            val target = entries.firstOrNull()?.languagePair?.target
            SavedState(
                entries = entries,
                courseTotal = readyEntries.count { it.languagePair.target == target },
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SavedState())

    fun follow(list: List<Long>) {
        if (ids.value != list) ids.value = list
    }
}
