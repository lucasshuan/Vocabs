package io.github.lucasshuan.vocabu.ui.words

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.lucasshuan.vocabu.shared.AppContainer
import io.github.lucasshuan.vocabu.shared.domain.Entry
import io.github.lucasshuan.vocabu.shared.domain.LanguagePair
import io.github.lucasshuan.vocabu.shared.domain.MemoryLevel
import io.github.lucasshuan.vocabu.shared.domain.Scope
import io.github.lucasshuan.vocabu.shared.domain.Steps
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class MemoryFilter(val label: String) { ALL("Todas"), LEARNING("Aprendendo"), FAMILIAR("Familiar"), MASTERED("Dominada") }

/**
 * [total] and [inQueue] bypass the search and the level, so a collapsed header
 * still says how much is inside and how much of it is due.
 */
data class LanguageGroup(
    val languagePair: LanguagePair,
    val entries: List<Entry>,
    val total: Int,
    val inQueue: Int,
    val isCollapsed: Boolean,
) {
    val emptyByFilter: Boolean get() = total > 0 && entries.isEmpty()
}

data class WordsState(
    val groups: List<LanguageGroup> = emptyList(),
    val filter: MemoryFilter = MemoryFilter.ALL,
    val query: String = "",
    val total: Int = 0,
    val mastered: Int = 0,
    val loaded: Boolean = false,
) {
    val matches: Int get() = groups.sumOf { it.entries.size }
}

/**
 * Every language at once. A language filter is hidden state — "Spanish only"
 * left on returns a week later as a collection that shrank by itself. A
 * collapsed header hides too, but stays on screen saying what it hides.
 */
class WordsViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = AppContainer.repository(app)
    private val preferences = AppContainer.preferences(app)
    private val filter = MutableStateFlow(MemoryFilter.ALL)
    private val query = MutableStateFlow("")

    // In two stages: `combine` is only typed up to five flows.
    private val crop = combine(filter, query, ::Pair)

    val state: StateFlow<WordsState> = combine(
        repository.observeReady(Scope.All),
        preferences.observeCourses(),
        preferences.observeLanguagePair(),
        preferences.observeCollapsedGroups(),
        crop,
    ) { readyEntries, enrolled, languagePair, collapsed, (currentFilter, term) ->
        val now = System.currentTimeMillis()
        val wanted = term.normalizado()
        val byCourse = readyEntries.groupBy { it.languagePair.target }

        WordsState(
            // By enrolment, not card count: Home's strip order, and changing it
            // would make the two screens disagree about where French is.
            groups = enrolled.map { target ->
                val ofCourse = byCourse[target].orEmpty()
                LanguageGroup(
                    languagePair = LanguagePair(native = languagePair.native, target = target),
                    entries = ofCourse.filter { cabe(it, currentFilter, wanted, now) },
                    total = ofCourse.size,
                    inQueue = ofCourse.count { it.needsReview(now) },
                    isCollapsed = target in collapsed,
                )
            },
            filter = currentFilter,
            query = term,
            total = readyEntries.size,
            mastered = readyEntries.count { Steps.level(it.step) == MemoryLevel.MASTERED },
            loaded = true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WordsState())

    private fun cabe(entry: Entry, filter: MemoryFilter, wanted: String, now: Long): Boolean {
        val level = entry.retention?.levelAt(now) ?: MemoryLevel.NEW
        val matchesLevel = when (filter) {
            MemoryFilter.ALL -> true
            MemoryFilter.LEARNING -> level == MemoryLevel.NEW || level == MemoryLevel.LEARNING
            MemoryFilter.FAMILIAR -> level == MemoryLevel.FAMILIAR
            MemoryFilter.MASTERED -> level == MemoryLevel.MASTERED
        }
        val matchesSearch = wanted.isBlank() ||
            entry.target.orEmpty().normalizado().contains(wanted) ||
            entry.card?.translation.orEmpty().normalizado().contains(wanted)
        return matchesLevel && matchesSearch
    }

    fun filter(new: MemoryFilter) { filter.value = new }
    fun search(text: String) { query.value = text }
    fun toggleGroup(target: String) = preferences.toggleGroup(target)
}

private val spaces = Regex("\\s+")
private fun String.normalizado() = trim().lowercase().replace(spaces, " ")
