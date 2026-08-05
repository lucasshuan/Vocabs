package com.jean.vocabs.ui.pending

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jean.vocabs.shared.AppContainer
import com.jean.vocabs.shared.domain.Capture
import com.jean.vocabs.shared.domain.Entry
import com.jean.vocabs.shared.domain.Scope
import com.jean.vocabs.ui.components.captureTitle
import com.jean.vocabs.ui.components.entryTitle
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
 * Pending is a queue, not a per-course notification.
 *
 * It is never sliced by language — not by the open course, not by a filter left
 * on from a previous visit. What lands here is stalled work, and stalled work in
 * one language does not stop existing because the person went to study another.
 * The tab badge counts everything for the same reason.
 */
data class PendingState(
    val captures: List<Capture> = emptyList(),
    val cards: List<Entry> = emptyList(),
) {
    val total: Int get() = captures.size + cards.size

    /** Raw captures per language — the number each filter chip shows. */
    val byLanguage: Map<String, Int>
        get() = (captures.map { it.languagePair.target } + cards.map { it.languagePair.target })
            .groupingBy { it }
            .eachCount()
}

/**
 * A deletion that has left the screen and can still come back.
 *
 * [key] exists so that two deletions of the same kind in a row count as different
 * events, and the strip restarts its countdown instead of continuing the previous
 * one.
 */
data class PendingDeletion(
    val key: Long,
    val id: Long,
    val isCapture: Boolean,
    val title: String,
)

/**
 * How long "Undo" stands.
 *
 * The same 5 s as the capture notice, and not by coincidence: it is the interval
 * the app has already taught as "how long a strip lasts". The bar running along
 * the footer shows what is left, so ignoring it is a choice and not a surprise.
 */
private const val UNDO_WINDOW_MS = 5_000L

class PendingViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = AppContainer.repository(app)

    private val _exclusao = MutableStateFlow<PendingDeletion?>(null)

    /** The last deletion still inside the regret window, if any. */
    val deletion: StateFlow<PendingDeletion?> = _exclusao.asStateFlow()

    private var count: Job? = null

    /**
     * The queue **already without** what was just swiped away.
     *
     * The item leaves the list at the instant of the gesture, long before the
     * database knows: whoever swiped needs to see the queue shrink now, and the
     * real deletion only happens when the undo window closes. The tab badge comes
     * from `total`, which comes from here, so the two never disagree.
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
                title = getApplication<Application>().captureTitle(capture),
            )
        )
    }

    fun deleteCard(entry: Entry) {
        schedule(PendingDeletion(System.nanoTime(), entry.id, isCapture = false, title = getApplication<Application>().entryTitle(entry)))
    }

    /** The gesture was a mistake: the item returns and nothing reaches the database. */
    fun undo() {
        count?.cancel()
        count = null
        _exclusao.value = null
    }

    /**
     * One deletion at a time.
     *
     * Swiping a second card before the first strip disappears **confirms** the
     * first — stacking strips would mean reading three confirmations to clear
     * three rows, and clearing the queue in series is what the gesture is for.
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
     * The window closed — no way back: the capture takes its media file with it.
     *
     * The erase itself runs in the app's scope rather than the screen's, because
     * leaving Pending mid-call would leave the row half-deleted. The 5 s count is
     * the ViewModel's on purpose: if the process dies before it finishes the item
     * simply stays in the queue, which is the safe error of the two.
     */
    private fun confirm(deletion: PendingDeletion) {
        AppContainer.scope.launch {
            if (deletion.isCapture) repository.deleteCapture(deletion.id) else repository.delete(deletion.id)
        }
    }
}
