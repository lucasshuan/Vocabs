package com.jean.vocabs.ui.progresso

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jean.vocabs.shared.AppContainer
import com.jean.vocabs.shared.domain.AtividadeDiaria
import com.jean.vocabs.shared.domain.Degraus
import com.jean.vocabs.shared.domain.Entrada
import com.jean.vocabs.shared.domain.Evento
import com.jean.vocabs.shared.domain.NivelMemoria
import com.jean.vocabs.shared.domain.ParIdiomas
import com.jean.vocabs.shared.domain.QuotaDoDia
import com.jean.vocabs.shared.domain.UsoIa
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * O estado das três telas de progresso.
 *
 * Um ViewModel para as três porque elas leem exatamente a mesma coisa em cortes
 * diferentes: a semana e a quota aparecem em duas, o estoque de palavras em
 * duas, e navegar entre elas com estados independentes faria os números piscarem
 * de uma para a outra enquanto cada uma refizesse suas contas.
 */
data class ProgressoEstado(
    val par: ParIdiomas = ParIdiomas.PADRAO,
    val semana: List<DiaDoProgresso> = emptyList(),
    val mes: String = "",
    val diasSeguidos: Int = 0,
    val melhorSequencia: Int = 0,
    val quota: QuotaDoDia = QuotaDoDia(feita = 0, naFila = 0),
    val palavras: List<Entrada> = emptyList(),
    val eventos: List<Evento> = emptyList(),
    val acertos: Int = 0,
    val respondidas: Int = 0,
    val usoIa: UsoIa = UsoIa("", 0),
) {
    val total: Int get() = palavras.size

    /** Contadas por degrau: é o número que não muda sozinho enquanto a pessoa dorme. */
    val porNivel: Map<NivelMemoria, List<Entrada>>
        get() = palavras.groupBy { Degraus.nivel(it.degrau) }

    val dominadas: Int get() = porNivel[NivelMemoria.DOMINADA]?.size ?: 0
    val familiares: Int get() = porNivel[NivelMemoria.FAMILIAR]?.size ?: 0
    val aprendendo: Int get() = total - dominadas - familiares

    /** As que estão a um acerto de mudar de nome — o "3 estão perto de virar". */
    val pertoDeVirar: List<Entrada>
        get() = palavras.filter { entrada ->
            val degrau = entrada.degrau
            Degraus.acertosParaSubirDeNivel(degrau) == 1
        }

    val taxaDeAcerto: Double? get() = if (respondidas == 0) null else acertos.toDouble() / respondidas

    /**
     * Palavras por dia desde a primeira captura.
     *
     * Nulo enquanto não houver ao menos um dia inteiro de uso: no primeiro dia a
     * conta seria "tudo o que capturei dividido por um", e uma tarde animada
     * viraria uma média de 12 palavras/dia que nunca mais se repete.
     */
    val palavrasPorDia: Double?
        get() {
            val primeira = palavras.minOfOrNull { it.criadoEm } ?: return null
            val dias = (System.currentTimeMillis() - primeira) / 86_400_000.0
            return if (dias < 1.0) null else total / dias
        }
}

/** Um dia da faixa da semana, já com o número e as revisões resolvidos. */
data class DiaDoProgresso(
    val data: LocalDate,
    val revisoes: Int,
    val hoje: Boolean,
    val futuro: Boolean,
)

class ProgressoViewModel(app: Application) : AndroidViewModel(app) {
    private val repositorio = AppContainer.repositorio(app)

    /** Em duas etapas: `combine` só tem sobrecarga tipada até cinco fluxos. */
    private val semanaEQuota = combine(
        repositorio.observarResumoDeRevisao(),
        repositorio.observarAtividade(84),
    ) { revisao, atividade ->
        val hoje = LocalDate.now()
        ProgressoEstado(
            semana = semanaDe(hoje, atividade),
            mes = nomeDoMes(hoje),
            diasSeguidos = revisao.diasSeguidos,
            melhorSequencia = revisao.melhorSequencia,
            quota = revisao.quota,
        )
    }

    val estado: StateFlow<ProgressoEstado> = combine(
        semanaEQuota,
        repositorio.observarProntas(),
        repositorio.observarEventos(84),
        repositorio.observarUsoIa(),
        repositorio.observarCursoAtivo(),
    ) { base, prontas, eventos, usoIa, par ->
        base.copy(
            par = par,
            palavras = prontas,
            eventos = eventos,
            acertos = prontas.sumOf { it.retencao?.acertos ?: 0 },
            respondidas = prontas.sumOf { it.retencao?.respondidas ?: 0 },
            usoIa = usoIa,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressoEstado())
}

/**
 * A semana corrente, de segunda a domingo.
 *
 * Segunda como primeiro dia porque é assim que o handoff a desenha, e porque a
 * sequência de estudo é uma semana de trabalho, não de calendário americano.
 */
internal fun semanaDe(hoje: LocalDate, atividade: List<AtividadeDiaria>): List<DiaDoProgresso> {
    val segunda = hoje.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val porDia = atividade.associate { it.dia to it.revisoes }
    return (0L until 7L).map { deslocamento ->
        val data = segunda.plusDays(deslocamento)
        DiaDoProgresso(
            data = data,
            revisoes = porDia[data.toEpochDay() + DIA_JULIANO_DA_EPOCA] ?: 0,
            hoje = data == hoje,
            futuro = data.isAfter(hoje),
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
private const val DIA_JULIANO_DA_EPOCA = 2_440_588L

private val MESES = listOf(
    "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
    "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro",
)

/** Escrito à mão porque `Locale("pt","BR")` depende dos dados de ICU do aparelho. */
internal fun nomeDoMes(data: LocalDate): String = MESES[data.monthValue - 1]

internal val SIGLAS_DA_SEMANA = listOf("seg", "ter", "qua", "qui", "sex", "sáb", "dom")
