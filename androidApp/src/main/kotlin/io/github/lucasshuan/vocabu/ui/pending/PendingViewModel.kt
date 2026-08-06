package io.github.lucasshuan.vocabu.ui.pending

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.lucasshuan.vocabu.shared.AppContainer
import io.github.lucasshuan.vocabu.shared.domain.Capture
import io.github.lucasshuan.vocabu.shared.domain.Entry
import io.github.lucasshuan.vocabu.shared.domain.Scope
import io.github.lucasshuan.vocabu.ui.components.captureTitle
import io.github.lucasshuan.vocabu.ui.components.entryTitle
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Never sliced by language, badge included: stalled work in one language does
 * not stop existing because someone went to study another.
 */
data class PendingState(
    val captures: List<Capture> = emptyList(),
    val cards: List<Entry> = emptyList(),
) {
    val total: Int get() = captures.size + cards.size

    /** What each filter chip counts. */
    val byLanguage: Map<String, Int>
        get() = (captures.map { it.languagePair.target } + cards.map { it.languagePair.target })
            .groupingBy { it }
            .eachCount()
}

/**
 * Off the screen, still reversible. [key] makes two deletions of the same kind
 * in a row different events, so the strip restarts its countdown.
 */
data class PendingDeletion(
    val key: Long,
    val id: Long,
    val isCapture: Boolean,
    val title: String,
)

/** The capture notice's 5s: the interval the app already taught for a strip. */
private const val UNDO_WINDOW_MS = 5_000L

class PendingViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = AppContainer.repository(app)

    private val _exclusao = MutableStateFlow<PendingDeletion?>(null)

    /** The last deletion still inside the regret window. */
    val deletion: StateFlow<PendingDeletion?> = _exclusao.asStateFlow()

    private var count: Job? = null

    /**
     * Already without what was just swiped: the row leaves on the gesture, and
     * the database only hears about it when the undo window closes. The badge
     * reads `total`, which reads this, so the two cannot disagree.
     */
    val state: StateFlow<PendingState> = combine(
        repository.observePendingCaptures(Scope.All),
        repository.observeInbox(Scope.All),
        _exclusao,
    ) { captures, cards, deletion ->
        PendingState(
            captures = captures.filterNot { deletion != null && deletion.isCapture && deletion.id == it.id },
            cards = cards.filterNot { deletion != null && !deletion.isCapture && deletion.id == it.id },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PendingState())

    val total: StateFlow<Int> = state.map { it.total }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun tryAgain(id: Long) {
        AppContainer.scope.launch { repository.generateCard(id) }
    }

    fun deleteCapture(capture: Capture) {
        schedule(
            PendingDeletion(
                System.nanoTime(),
                capture.id,
                isCapture = true,
                title = getApplication<Application>().resources.captureTitle(capture),
            )
        )
    }

    fun deleteCard(entry: Entry) {
        schedule(PendingDeletion(System.nanoTime(), entry.id, isCapture = false, title = getApplication<Application>().resources.entryTitle(entry)))
    }

    /** The item returns and nothing reaches the database. */
    fun undo() {
        count?.cancel()
        count = null
        _exclusao.value = null
    }

    /**
     * A second swipe confirms the first: stacking strips would mean reading
     * three confirmations to clear three rows.
     */
    private fun schedule(fresh: PendingDeletion) {
        count?.cancel()
        _exclusao.value?.let(::confirm)
        _exclusao.value = fresh
        count = viewModelScope.launch {
            delay(UNDO_WINDOW_MS)
            confirm(fresh)
            _exclusao.compareAndSet(fresh, null)
        }
    }

    /**
     * No way back; the capture takes its media file with it. Runs in the app's
     * scope, since leaving Pending mid-call would half-delete the row.
     *
     * The countdown stays the ViewModel's: a process that dies before it
     * finishes leaves the item in the queue, the safe error of the two.
     */
    private fun confirm(deletion: PendingDeletion) {
        AppContainer.scope.launch {
            if (deletion.isCapture) repository.deleteCapture(deletion.id) else repository.delete(deletion.id)
        }
    }
}
