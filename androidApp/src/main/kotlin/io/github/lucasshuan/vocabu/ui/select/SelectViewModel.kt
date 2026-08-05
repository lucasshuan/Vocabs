package io.github.lucasshuan.vocabu.ui.select

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.lucasshuan.vocabu.shared.AppContainer
import io.github.lucasshuan.vocabu.shared.domain.Capture
import io.github.lucasshuan.vocabu.shared.domain.Entry
import io.github.lucasshuan.vocabu.shared.domain.Scope
import io.github.lucasshuan.vocabu.shared.domain.SelectedTarget
import io.github.lucasshuan.vocabu.shared.domain.duplicateOfTarget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The selection screen: marking what caught the eye in an already-saved capture.
 *
 * The duplicate is looked for **in the target language**, not across the whole
 * collection: "carne" existing in Spanish is no reason to warn someone saving
 * "carne" in Italian. That is why the searched target carries its course.
 */
class SelectViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = AppContainer.repository(app)
    private val preferences = AppContainer.preferences(app)
    private val wanted = MutableStateFlow(Search())

    private data class Search(val text: String = "", val target: String = "")

    val duplicate: StateFlow<Entry?> = combine(
        repository.observeReady(Scope.All),
        repository.observeInbox(Scope.All),
        wanted,
    ) { readyEntries, inbox, query ->
        if (query.text.isBlank()) return@combine null
        duplicateOfTarget(query.text, (readyEntries + inbox).filter { it.languagePair.target == query.target })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** The languages it can be saved to — the header picker's list. */
    val courses: StateFlow<List<String>> = preferences.observeCourses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun observe(id: Long): StateFlow<Capture?> = repository.observeCaptureById(id)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun findDuplicate(text: String, target: String) {
        wanted.value = Search(text, target)
    }

    /** Still changeable here: nothing is born in this pair until "Save". */
    fun switchLanguage(id: Long, target: String) {
        AppContainer.scope.launch { repository.changeCaptureLanguage(id, target) }
    }

    /**
     * Confirms and returns the created ids, which the confirmation screen follows
     * while the AI works. Generation continues in the app's scope: the navigation
     * happens before the first card is ready.
     */
    fun save(id: Long, snippet: String, targets: List<SelectedTarget>, onReady: (List<Long>) -> Unit) {
        viewModelScope.launch {
            val ids = repository.confirmCapture(id, snippet, targets)
            onReady(ids)
            AppContainer.scope.launch { repository.generateCards(ids) }
        }
    }

    fun delete(id: Long) {
        AppContainer.scope.launch { repository.deleteCapture(id) }
    }
}
