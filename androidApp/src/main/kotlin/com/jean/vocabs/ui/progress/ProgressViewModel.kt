package com.jean.vocabs.ui.progress

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jean.vocabs.shared.AppContainer
import com.jean.vocabs.shared.domain.CourseSummary
import com.jean.vocabs.shared.domain.DailyActivity
import com.jean.vocabs.shared.domain.DailyQuota
import com.jean.vocabs.shared.domain.Entry
import com.jean.vocabs.shared.domain.Event
import com.jean.vocabs.shared.domain.LanguagePair
import com.jean.vocabs.shared.domain.MemoryLevel
import com.jean.vocabs.shared.domain.Scope
import com.jean.vocabs.shared.domain.Steps
import com.jean.vocabs.shared.enrolledCourses
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn

/**
 * O estado das três telas de progresso.
 *
 * Um ViewModel para as três porque elas leem exatamente a mesma coisa em cortes
 * diferentes: a semana e a quota aparecem em duas, o estoque de palavras em
 * duas, e navegar entre elas com estados independentes faria os números piscarem
 * de uma para a outra enquanto cada uma refizesse suas contas.
 */
data class ProgressState(
    val languagePair: LanguagePair = LanguagePair.DEFAULT,
    /**
     * A semana de hoje, vazia, enquanto o banco não responde.
     *
     * É o mesmo desenho do curso sem palavra nenhuma — e é por isso que ele pode
     * ser o estado inicial: quem abre a tela vê o esqueleto tracejado no lugar
     * certo e ele se preenche, em vez de ver "0 de 10" por dois quadros.
     */
    val semana: List<ProgressDay> = weekOf(LocalDate.now(), emptyList()),
    val month: String = monthName(LocalDate.now()),
    val dayStreak: Int = 0,
    val quota: DailyQuota = DailyQuota(done = 0, inQueue = 0),
    val words: List<Entry> = emptyList(),
    val events: List<Event> = emptyList(),
) {
    val total: Int get() = words.size

    /** Contadas por degrau: é o número que não muda sozinho enquanto a pessoa dorme. */
    val byLevel: Map<MemoryLevel, List<Entry>>
        get() = words.groupBy { Steps.level(it.step) }

    val mastered: Int get() = byLevel[MemoryLevel.MASTERED]?.size ?: 0
    val familiar: Int get() = byLevel[MemoryLevel.FAMILIAR]?.size ?: 0
    val learning: Int get() = total - mastered - familiar

    /** As que estão a um acerto de mudar de nome — o "3 estão perto de virar". */
    val closeToLeveling: List<Entry>
        get() = words.filter { entry ->
            val step = entry.step
            Steps.hitsToLevelUp(step) == 1
        }
}

/** Um dia da faixa da semana, já com o número e as revisões resolvidos. */
data class ProgressDay(
    val data: LocalDate,
    val reviews: Int,
    val today: Boolean,
    val future: Boolean,
)

/**
 * O progresso de **um** curso, que não é necessariamente o aberto.
 *
 * A tela abre de uma linha da Você, e a Você mostra os três idiomas. Trocar o
 * curso aberto só para poder olhar o progresso do francês mexeria na página da
 * Início e no destino do `+` — um efeito que ninguém pediu ao tocar numa linha.
 * Daí o curso entrar por [abrir] e virar um [Escopo.Curso] nomeado.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProgressViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = AppContainer.repository(app)
    private val preferences = AppContainer.preferences(app)

    /** Nulo até a rota dizer qual curso; nesse intervalo vale o curso aberto. */
    private val course = MutableStateFlow<String?>(null)

    private val scope: Flow<Scope> = course.map { target ->
        target?.let(Scope::Course) ?: Scope.ActiveCourse
    }

    val state: StateFlow<ProgressState> = scope.flatMapLatest { crop ->
        /** Em duas etapas: `combine` só tem sobrecarga tipada até cinco fluxos. */
        val weekAndQuota = combine(
            repository.observeReviewSummary(crop),
            repository.observeActivity(84),
        ) { review, activity ->
            val today = LocalDate.now()
            ProgressState(
                semana = weekOf(today, activity),
                month = monthName(today),
                // A sequência conta atividade em qualquer idioma; a quota é do
                // curso. São perguntas diferentes: hábito é da pessoa, carga é
                // da matéria.
                dayStreak = review.dayStreak,
                quota = review.quota,
            )
        }

        combine(
            weekAndQuota,
            repository.observeReady(crop),
            repository.observeEvents(84, crop),
            preferences.observeLanguagePair(),
        ) { base, readyEntries, events, languagePair ->
            base.copy(
                languagePair = pairOf(crop, languagePair),
                words = readyEntries,
                events = events,
            )
        }
            // Trocar de curso zera a tela antes de o banco responder.
            //
            // Sem isto, escolher o espanhol na gaveta deixaria os números do
            // inglês debaixo da bandeira espanhola pelo tempo de uma consulta —
            // e o rótulo "Quota de hoje no espanhol" sobre o "6 de 10" do inglês
            // é uma frase errada, não uma frase atrasada. O esqueleto é o único
            // estado honesto enquanto a resposta não chega.
            .onStart { emit(ProgressState(languagePair = pairOf(crop, preferences.languagePair))) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressState())

    /** O curso do recorte, com o nativo de quem lê — [Scope.CursoAberto] cai no aberto. */
    private fun pairOf(crop: Scope, activePair: LanguagePair) = LanguagePair(
        native = activePair.native,
        target = (crop as? Scope.Course)?.target ?: activePair.target,
    )

    /**
     * Trocar o curso **olhado**, e não o aberto.
     *
     * É o que a gaveta da bandeira faz, e é a mesma porta por onde a rota entra:
     * daí ela recarregar os dois cartões sem mexer na página da Início nem no
     * destino do `+`.
     */
    fun open(target: String?) {
        course.value = target?.takeIf { it.isNotBlank() }
    }

    /** Os cursos da gaveta: todos os matriculados, inclusive os que ainda não têm ficha. */
    val courses: StateFlow<List<CourseSummary>> = enrolledCourses(repository, preferences)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Quantos cursos existem — remover o último deixaria o app sem página nenhuma. */
    val canRemove: StateFlow<Boolean> = preferences.observeCourses()
        .map { it.size > 1 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun removeCourse(target: String) = preferences.unenroll(target)
}

/**
 * A semana corrente, de segunda a domingo.
 *
 * Segunda como primeiro dia porque é assim que o handoff a desenha, e porque a
 * sequência de estudo é uma semana de trabalho, não de calendário americano.
 */
internal fun weekOf(today: LocalDate, activity: List<DailyActivity>): List<ProgressDay> {
    val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val byDay = activity.associate { it.day to it.reviews }
    return (0L until 7L).map { offset ->
        val data = monday.plusDays(offset)
        ProgressDay(
            data = data,
            reviews = byDay[data.toEpochDay() + JULIAN_DAY_OF_EPOCH] ?: 0,
            today = data == today,
            future = data.isAfter(today),
        )
    }
}

/**
 * A diferença entre o dia juliano que o banco guarda e o epoch day do `java.time`.
 *
 * O banco resolve o dia local em SQL (`julianday(...) + 0.5`) para que a virada
 * do dia siga o fuso do aparelho sem o Kotlin comum precisar de uma biblioteca
 * de datas. Quem lê aqui converte uma vez, em vez de espalhar a soma.
 */
private const val JULIAN_DAY_OF_EPOCH = 2_440_588L

private val MONTHS = listOf(
    "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
    "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro",
)

/** Escrito à mão porque `Locale("pt","BR")` depende dos dados de ICU do aparelho. */
internal fun monthName(data: LocalDate): String = MONTHS[data.monthValue - 1]

internal val WEEKDAY_LABELS = listOf("seg", "ter", "qua", "qui", "sex", "sáb", "dom")
