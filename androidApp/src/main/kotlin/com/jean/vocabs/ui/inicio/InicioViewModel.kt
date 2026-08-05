package com.jean.vocabs.ui.start

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jean.vocabs.shared.AppContainer
import com.jean.vocabs.shared.domain.Steps
import com.jean.vocabs.shared.domain.Entry
import com.jean.vocabs.shared.domain.Scope
import com.jean.vocabs.shared.domain.MemoryLevel
import com.jean.vocabs.shared.domain.LanguagePair
import com.jean.vocabs.shared.domain.CourseSummary
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
data class PaginaDoInicio(
    val resumo: CourseSummary,
    val forcaMedia: Int,
    /** Quantas vencem ainda nas próximas 24h — o "Próximas 5 em 19h". */
    val proximasEm24h: Int,
    val capturadasHoje: List<Entry>,
) {
    val languagePair: LanguagePair get() = resumo.languagePair
}

data class InicioEstado(
    val paginas: List<PaginaDoInicio> = emptyList(),
    val ativo: String = "",
    val native: String = "",
    val carregado: Boolean = false,
) {
    val courses: List<CourseSummary> get() = paginas.map { it.resumo }

    val indiceAtivo: Int get() = paginas.indexOfFirst { it.languagePair.target == ativo }.coerceAtLeast(0)

    /** Com um curso só não há faixa nem carrossel: não há para onde deslizar. */
    val temCarrossel: Boolean get() = paginas.size > 1
}

class InicioViewModel(app: Application) : AndroidViewModel(app) {
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
    val estado: StateFlow<InicioEstado> = combine(
        preferences.observeLanguagePair(),
        preferences.observeCourses(),
        repository.observeReady(Scope.Todos),
    ) { languagePair, matriculados, prontas ->
        val now = System.currentTimeMillis()
        val today = LocalDate.now()
        val porCurso = prontas.groupBy { it.languagePair.target }

        InicioEstado(
            paginas = matriculados.map { target ->
                pagina(
                    languagePair = LanguagePair(native = languagePair.native, target = target),
                    entries = porCurso[target].orEmpty(),
                    now = now,
                    today = today,
                )
            },
            ativo = languagePair.target,
            native = languagePair.native,
            carregado = true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InicioEstado())

    private fun pagina(
        languagePair: LanguagePair,
        entries: List<Entry>,
        now: Long,
        today: LocalDate,
    ): PaginaDoInicio {
        val faltas = entries.mapNotNull { it.retention?.nextReviewIn(now) }
        return PaginaDoInicio(
            resumo = CourseSummary(
                languagePair = languagePair,
                total = entries.size,
                // Por degrau, como em toda tela de número: contar por força de
                // memória faria o mesmo total aparecer diferente em cada uma,
                // porque ela decai entre a leitura de uma e a da outra.
                mastered = entries.count { Steps.level(it.degrau) == MemoryLevel.MASTERED },
                inQueue = entries.count { it.needsReview(now) },
                nextInMillis = faltas.filter { it > 0L }.minOrNull(),
            ),
            forcaMedia = entries.mapNotNull { it.retention?.pointsAt(now) }.mediaOuZero().toInt(),
            proximasEm24h = faltas.count { it in 1..UM_DIA_EM_MILLIS },
            capturadasHoje = entries
                .filter { Instant.ofEpochMilli(it.createdAt).atZone(ZoneId.systemDefault()).toLocalDate() == today }
                .take(3),
        )
    }

    /** Deslizar o carrossel **é** trocar de curso: a revisão e o `+` seguem a página. */
    fun openCourse(codigo: String) = preferences.openCourse(codigo)

    private companion object {
        const val UM_DIA_EM_MILLIS = 86_400_000L
    }
}

private fun List<Double>.mediaOuZero(): Double = if (isEmpty()) 0.0 else average()
