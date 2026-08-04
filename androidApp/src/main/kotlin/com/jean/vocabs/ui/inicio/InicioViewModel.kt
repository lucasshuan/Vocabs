package com.jean.vocabs.ui.inicio

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jean.vocabs.shared.AppContainer
import com.jean.vocabs.shared.domain.Degraus
import com.jean.vocabs.shared.domain.Entrada
import com.jean.vocabs.shared.domain.Escopo
import com.jean.vocabs.shared.domain.NivelMemoria
import com.jean.vocabs.shared.domain.ParIdiomas
import com.jean.vocabs.shared.domain.ResumoCurso
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
    val resumo: ResumoCurso,
    val forcaMedia: Int,
    /** Quantas vencem ainda nas próximas 24h — o "Próximas 5 em 19h". */
    val proximasEm24h: Int,
    val capturadasHoje: List<Entrada>,
) {
    val par: ParIdiomas get() = resumo.par
}

data class InicioEstado(
    val paginas: List<PaginaDoInicio> = emptyList(),
    val ativo: String = "",
    val nativo: String = "",
    val carregado: Boolean = false,
) {
    val cursos: List<ResumoCurso> get() = paginas.map { it.resumo }

    val indiceAtivo: Int get() = paginas.indexOfFirst { it.par.alvo == ativo }.coerceAtLeast(0)

    /** Com um curso só não há faixa nem carrossel: não há para onde deslizar. */
    val temCarrossel: Boolean get() = paginas.size > 1
}

class InicioViewModel(app: Application) : AndroidViewModel(app) {
    private val repositorio = AppContainer.repositorio(app)
    private val preferencias = AppContainer.preferencias(app)

    /**
     * Uma leitura só, de todos os cursos, repartida aqui.
     *
     * O carrossel mostra os três idiomas ao mesmo tempo — deslizar não pode
     * disparar consulta nova. Ler tudo de uma vez e agrupar em memória é o que
     * faz a troca de página ser instantânea, e é também o que garante que os
     * três cartões estejam falando do mesmo instante.
     */
    val estado: StateFlow<InicioEstado> = combine(
        preferencias.observarPar(),
        preferencias.observarCursos(),
        repositorio.observarProntas(Escopo.Todos),
    ) { par, matriculados, prontas ->
        val agora = System.currentTimeMillis()
        val hoje = LocalDate.now()
        val porCurso = prontas.groupBy { it.par.alvo }

        InicioEstado(
            paginas = matriculados.map { alvo ->
                pagina(
                    par = ParIdiomas(nativo = par.nativo, alvo = alvo),
                    entradas = porCurso[alvo].orEmpty(),
                    agora = agora,
                    hoje = hoje,
                )
            },
            ativo = par.alvo,
            nativo = par.nativo,
            carregado = true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InicioEstado())

    private fun pagina(
        par: ParIdiomas,
        entradas: List<Entrada>,
        agora: Long,
        hoje: LocalDate,
    ): PaginaDoInicio {
        val faltas = entradas.mapNotNull { it.retencao?.proximaRevisaoEm(agora) }
        return PaginaDoInicio(
            resumo = ResumoCurso(
                par = par,
                total = entradas.size,
                // Por degrau, como em toda tela de número: contar por força de
                // memória faria o mesmo total aparecer diferente em cada uma,
                // porque ela decai entre a leitura de uma e a da outra.
                dominadas = entradas.count { Degraus.nivel(it.degrau) == NivelMemoria.DOMINADA },
                naFila = entradas.count { it.precisaRevisar(agora) },
                proximaEmMillis = faltas.filter { it > 0L }.minOrNull(),
            ),
            forcaMedia = entradas.mapNotNull { it.retencao?.pontosEm(agora) }.mediaOuZero().toInt(),
            proximasEm24h = faltas.count { it in 1..UM_DIA_EM_MILLIS },
            capturadasHoje = entradas
                .filter { Instant.ofEpochMilli(it.criadoEm).atZone(ZoneId.systemDefault()).toLocalDate() == hoje }
                .take(3),
        )
    }

    /** Deslizar o carrossel **é** trocar de curso: a revisão e o `+` seguem a página. */
    fun abrirCurso(codigo: String) = preferencias.abrirCurso(codigo)

    private companion object {
        const val UM_DIA_EM_MILLIS = 86_400_000L
    }
}

private fun List<Double>.mediaOuZero(): Double = if (isEmpty()) 0.0 else average()
