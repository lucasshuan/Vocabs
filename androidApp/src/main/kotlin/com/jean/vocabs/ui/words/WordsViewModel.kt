package com.jean.vocabs.ui.words

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jean.vocabs.shared.AppContainer
import com.jean.vocabs.shared.domain.Entry
import com.jean.vocabs.shared.domain.LanguagePair
import com.jean.vocabs.shared.domain.MemoryLevel
import com.jean.vocabs.shared.domain.Scope
import com.jean.vocabs.shared.domain.Steps
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class MemoryFilter(val label: String) { ALL("Todas"), LEARNING("Aprendendo"), FAMILIAR("Familiar"), MASTERED("Dominada") }

/**
 * Um idioma como cabeçalho, e não como filtro.
 *
 * [total] e [naFila] são do curso inteiro, sem passar pela busca nem pelo nível:
 * o cabeçalho recolhido continua dizendo quanta coisa está ali dentro e quanto
 * dela pede revisão, e é isso que faz fechar um grupo não custar informação.
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
 * Vocabulários mostra os três idiomas de uma vez.
 *
 * O idioma virou cabeçalho e deixou de ser filtro porque filtro é estado
 * escondido: quem deixasse "só espanhol" ligado e voltasse na semana seguinte
 * veria uma coleção que encolheu sozinha. Cabeçalho recolhido também esconde,
 * mas continua na tela dizendo o que esconde — e o estado dele é preferência,
 * não segredo.
 */
class WordsViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = AppContainer.repository(app)
    private val preferences = AppContainer.preferences(app)
    private val filter = MutableStateFlow(MemoryFilter.ALL)
    private val query = MutableStateFlow("")

    /** Em duas etapas: `combine` só tem sobrecarga tipada até cinco fluxos. */
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
            // A ordem é a da matrícula, e não a da quantidade de fichas: é a mesma
            // ordem da faixa da Início, e trocá-la aqui faria as duas telas
            // discordarem sobre onde fica o francês.
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
