package com.jean.vocabs.ui.words

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jean.vocabs.shared.AppContainer
import com.jean.vocabs.shared.domain.Steps
import com.jean.vocabs.shared.domain.Entry
import com.jean.vocabs.shared.domain.Scope
import com.jean.vocabs.shared.domain.MemoryLevel
import com.jean.vocabs.shared.domain.LanguagePair
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class MemoryFilter(val rotulo: String) { TODAS("Todas"), APRENDENDO("Aprendendo"), FAMILIAR("Familiar"), DOMINADA("Dominada") }

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
    val recolhido: Boolean,
) {
    val vazioPorFiltro: Boolean get() = total > 0 && entries.isEmpty()
}

data class WordsState(
    val grupos: List<LanguageGroup> = emptyList(),
    val filtro: MemoryFilter = MemoryFilter.TODAS,
    val busca: String = "",
    val total: Int = 0,
    val mastered: Int = 0,
    val carregado: Boolean = false,
) {
    val encontradas: Int get() = grupos.sumOf { it.entries.size }
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
    private val filtro = MutableStateFlow(MemoryFilter.TODAS)
    private val busca = MutableStateFlow("")

    /** Em duas etapas: `combine` só tem sobrecarga tipada até cinco fluxos. */
    private val recorte = combine(filtro, busca, ::Pair)

    val estado: StateFlow<WordsState> = combine(
        repository.observeReady(Scope.Todos),
        preferences.observeCourses(),
        preferences.observeLanguagePair(),
        preferences.observeCollapsedGroups(),
        recorte,
    ) { prontas, matriculados, languagePair, recolhidos, (filtroAtual, termo) ->
        val now = System.currentTimeMillis()
        val procurado = termo.normalizado()
        val porCurso = prontas.groupBy { it.languagePair.target }

        WordsState(
            // A ordem é a da matrícula, e não a da quantidade de fichas: é a mesma
            // ordem da faixa da Início, e trocá-la aqui faria as duas telas
            // discordarem sobre onde fica o francês.
            grupos = matriculados.map { target ->
                val doCurso = porCurso[target].orEmpty()
                LanguageGroup(
                    languagePair = LanguagePair(native = languagePair.native, target = target),
                    entries = doCurso.filter { cabe(it, filtroAtual, procurado, now) },
                    total = doCurso.size,
                    inQueue = doCurso.count { it.needsReview(now) },
                    recolhido = target in recolhidos,
                )
            },
            filtro = filtroAtual,
            busca = termo,
            total = prontas.size,
            mastered = prontas.count { Steps.level(it.degrau) == MemoryLevel.MASTERED },
            carregado = true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WordsState())

    private fun cabe(entry: Entry, filtro: MemoryFilter, procurado: String, now: Long): Boolean {
        val level = entry.retention?.levelAt(now) ?: MemoryLevel.NEW
        val bateNivel = when (filtro) {
            MemoryFilter.TODAS -> true
            MemoryFilter.APRENDENDO -> level == MemoryLevel.NEW || level == MemoryLevel.LEARNING
            MemoryFilter.FAMILIAR -> level == MemoryLevel.FAMILIAR
            MemoryFilter.DOMINADA -> level == MemoryLevel.MASTERED
        }
        val bateBusca = procurado.isBlank() ||
            entry.target.orEmpty().normalizado().contains(procurado) ||
            entry.card?.translation.orEmpty().normalizado().contains(procurado)
        return bateNivel && bateBusca
    }

    fun filtrar(novo: MemoryFilter) { filtro.value = novo }
    fun buscar(text: String) { busca.value = text }
    fun toggleGroup(target: String) = preferences.toggleGroup(target)
}

private val espacos = Regex("\\s+")
private fun String.normalizado() = trim().lowercase().replace(espacos, " ")
