package com.jean.vocabs.ui.progresso

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jean.vocabs.shared.AppContainer
import com.jean.vocabs.shared.cursosMatriculados
import com.jean.vocabs.shared.domain.AtividadeDiaria
import com.jean.vocabs.shared.domain.Degraus
import com.jean.vocabs.shared.domain.Entrada
import com.jean.vocabs.shared.domain.Escopo
import com.jean.vocabs.shared.domain.Evento
import com.jean.vocabs.shared.domain.NivelMemoria
import com.jean.vocabs.shared.domain.ParIdiomas
import com.jean.vocabs.shared.domain.QuotaDoDia
import com.jean.vocabs.shared.domain.ResumoCurso
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
data class ProgressoEstado(
    val par: ParIdiomas = ParIdiomas.PADRAO,
    /**
     * A semana de hoje, vazia, enquanto o banco não responde.
     *
     * É o mesmo desenho do curso sem palavra nenhuma — e é por isso que ele pode
     * ser o estado inicial: quem abre a tela vê o esqueleto tracejado no lugar
     * certo e ele se preenche, em vez de ver "0 de 10" por dois quadros.
     */
    val semana: List<DiaDoProgresso> = semanaDe(LocalDate.now(), emptyList()),
    val mes: String = nomeDoMes(LocalDate.now()),
    val diasSeguidos: Int = 0,
    val quota: QuotaDoDia = QuotaDoDia(feita = 0, naFila = 0),
    val palavras: List<Entrada> = emptyList(),
    val eventos: List<Evento> = emptyList(),
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
}

/** Um dia da faixa da semana, já com o número e as revisões resolvidos. */
data class DiaDoProgresso(
    val data: LocalDate,
    val revisoes: Int,
    val hoje: Boolean,
    val futuro: Boolean,
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
class ProgressoViewModel(app: Application) : AndroidViewModel(app) {
    private val repositorio = AppContainer.repositorio(app)
    private val preferencias = AppContainer.preferencias(app)

    /** Nulo até a rota dizer qual curso; nesse intervalo vale o curso aberto. */
    private val curso = MutableStateFlow<String?>(null)

    private val escopo: Flow<Escopo> = curso.map { alvo ->
        alvo?.let(Escopo::Curso) ?: Escopo.CursoAberto
    }

    val estado: StateFlow<ProgressoEstado> = escopo.flatMapLatest { recorte ->
        /** Em duas etapas: `combine` só tem sobrecarga tipada até cinco fluxos. */
        val semanaEQuota = combine(
            repositorio.observarResumoDeRevisao(recorte),
            repositorio.observarAtividade(84),
        ) { revisao, atividade ->
            val hoje = LocalDate.now()
            ProgressoEstado(
                semana = semanaDe(hoje, atividade),
                mes = nomeDoMes(hoje),
                // A sequência conta atividade em qualquer idioma; a quota é do
                // curso. São perguntas diferentes: hábito é da pessoa, carga é
                // da matéria.
                diasSeguidos = revisao.diasSeguidos,
                quota = revisao.quota,
            )
        }

        combine(
            semanaEQuota,
            repositorio.observarProntas(recorte),
            repositorio.observarEventos(84, recorte),
            preferencias.observarPar(),
        ) { base, prontas, eventos, par ->
            base.copy(
                par = parDe(recorte, par),
                palavras = prontas,
                eventos = eventos,
            )
        }
            // Trocar de curso zera a tela antes de o banco responder.
            //
            // Sem isto, escolher o espanhol na gaveta deixaria os números do
            // inglês debaixo da bandeira espanhola pelo tempo de uma consulta —
            // e o rótulo "Quota de hoje no espanhol" sobre o "6 de 10" do inglês
            // é uma frase errada, não uma frase atrasada. O esqueleto é o único
            // estado honesto enquanto a resposta não chega.
            .onStart { emit(ProgressoEstado(par = parDe(recorte, preferencias.par))) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressoEstado())

    /** O curso do recorte, com o nativo de quem lê — [Escopo.CursoAberto] cai no aberto. */
    private fun parDe(recorte: Escopo, aberto: ParIdiomas) = ParIdiomas(
        nativo = aberto.nativo,
        alvo = (recorte as? Escopo.Curso)?.alvo ?: aberto.alvo,
    )

    /**
     * Trocar o curso **olhado**, e não o aberto.
     *
     * É o que a gaveta da bandeira faz, e é a mesma porta por onde a rota entra:
     * daí ela recarregar os dois cartões sem mexer na página da Início nem no
     * destino do `+`.
     */
    fun abrir(alvo: String?) {
        curso.value = alvo?.takeIf { it.isNotBlank() }
    }

    /** Os cursos da gaveta: todos os matriculados, inclusive os que ainda não têm ficha. */
    val cursos: StateFlow<List<ResumoCurso>> = cursosMatriculados(repositorio, preferencias)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Quantos cursos existem — remover o último deixaria o app sem página nenhuma. */
    val podeRemover: StateFlow<Boolean> = preferencias.observarCursos()
        .map { it.size > 1 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun removerCurso(alvo: String) = preferencias.desmatricular(alvo)
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
