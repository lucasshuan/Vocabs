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
 * A language as a header, not as a filter.
 *
 * [total] and [inQueue] are for the whole course, bypassing the search and the
 * level: a collapsed header still says how much is inside and how much of it is
 * due, which is what makes closing a group cost no information.
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
 * Words shows all three languages at once.
 *
 * The language became a header and stopped being a filter because a filter is
 * hidden state: leaving "Spanish only" on and coming back a week later would show
 * a collection that shrank by itself. A collapsed header also hides, but stays on
 * screen saying what it hides.
 */
class WordsViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = AppContainer.repository(app)
    private val preferences = AppContainer.preferences(app)
    private val filter = MutableStateFlow(MemoryFilter.ALL)
    private val query = MutableStateFlow("")

    /** In two stages: `combine` only has a typed overload up to five flows. */
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
            // Ordered by enrollment rather than card count: the same order as
            // Home's strip, and changing it here would make the two screens
            // disagree about where French is.
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
