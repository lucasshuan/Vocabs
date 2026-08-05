package com.jean.vocabs.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jean.vocabs.shared.AppContainer
import com.jean.vocabs.shared.domain.CourseSummary
import com.jean.vocabs.shared.domain.Entry
import com.jean.vocabs.shared.domain.LanguagePair
import com.jean.vocabs.shared.domain.MemoryLevel
import com.jean.vocabs.shared.domain.Scope
import com.jean.vocabs.shared.domain.Steps
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Uma página do carrossel — tudo o que a Início mostra de um curso.
 *
 * O [resumo] é o mesmo objeto que alimenta o selo da faixa, e não uma segunda
 * contagem: o número no chip do inglês e o "3 esfriaram hoje" do cartão do
 * inglês são a mesma frase dita duas vezes, e discordarem seria pior que
 * qualquer um dos dois faltar.
 */
data class HomePage(
    val summary: CourseSummary,
    val averageStrength: Int,
    /** Quantas vencem ainda nas próximas 24h — o "Próximas 5 em 19h". */
    val nextIn24h: Int,
    val capturedToday: List<Entry>,
) {
    val languagePair: LanguagePair get() = summary.languagePair
}

data class HomeState(
    val pages: List<HomePage> = emptyList(),
    val ativo: String = "",
    val native: String = "",
    val loaded: Boolean = false,
) {
    val courses: List<CourseSummary> get() = pages.map { it.summary }

    val activeIndex: Int get() = pages.indexOfFirst { it.languagePair.target == ativo }.coerceAtLeast(0)

    /** Com um curso só não há faixa nem carrossel: não há para onde deslizar. */
    val hasCarousel: Boolean get() = pages.size > 1
}

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = AppContainer.repository(app)
    private val preferences = AppContainer.preferences(app)

    /**
     * Uma leitura só, de todos os cursos, repartida aqui.
     *
     * O carrossel mostra os três idiomas ao mesmo tempo — deslizar não pode
     * disparar consulta nova. Ler tudo de uma vez e agrupar em memória é o que
     * faz a troca de página ser instantânea, e é também o que garante que os
     * três cartões estejam falando do mesmo instante.
     */
    val state: StateFlow<HomeState> = combine(
        preferences.observeLanguagePair(),
        preferences.observeCourses(),
        repository.observeReady(Scope.All),
    ) { languagePair, enrolled, readyEntries ->
        val now = System.currentTimeMillis()
        val today = LocalDate.now()
        val byCourse = readyEntries.groupBy { it.languagePair.target }

        HomeState(
            pages = enrolled.map { target ->
                page(
                    languagePair = LanguagePair(native = languagePair.native, target = target),
                    entries = byCourse[target].orEmpty(),
                    now = now,
                    today = today,
                )
            },
            ativo = languagePair.target,
            native = languagePair.native,
            loaded = true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())

    private fun page(
        languagePair: LanguagePair,
        entries: List<Entry>,
        now: Long,
        today: LocalDate,
    ): HomePage {
        val misses = entries.mapNotNull { it.retention?.nextReviewIn(now) }
        return HomePage(
            summary = CourseSummary(
                languagePair = languagePair,
                total = entries.size,
                // Por degrau, como em toda tela de número: contar por força de
                // memória faria o mesmo total aparecer diferente em cada uma,
                // porque ela decai entre a leitura de uma e a da outra.
                mastered = entries.count { Steps.level(it.step) == MemoryLevel.MASTERED },
                inQueue = entries.count { it.needsReview(now) },
                nextInMillis = misses.filter { it > 0L }.minOrNull(),
            ),
            averageStrength = entries.mapNotNull { it.retention?.pointsAt(now) }.averageOrZero().toInt(),
            nextIn24h = misses.count { it in 1..ONE_DAY_IN_MILLIS },
            capturedToday = entries
                .filter { Instant.ofEpochMilli(it.createdAt).atZone(ZoneId.systemDefault()).toLocalDate() == today }
                .take(3),
        )
    }

    /** Deslizar o carrossel **é** trocar de curso: a revisão e o `+` seguem a página. */
    fun openCourse(code: String) = preferences.openCourse(code)

    private companion object {
        const val ONE_DAY_IN_MILLIS = 86_400_000L
    }
}

private fun List<Double>.averageOrZero(): Double = if (isEmpty()) 0.0 else average()
