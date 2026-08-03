package com.jean.vocabs.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jean.vocabs.shared.AppContainer
import com.jean.vocabs.shared.domain.Degraus
import com.jean.vocabs.shared.domain.Entrada
import com.jean.vocabs.shared.domain.Escopo
import com.jean.vocabs.shared.domain.NivelMemoria
import com.jean.vocabs.shared.domain.ParIdiomas
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class FiltroMemoria(val rotulo: String) { TODAS("Todas"), APRENDENDO("Aprendendo"), FAMILIAR("Familiar"), DOMINADA("Dominada") }

/**
 * Um idioma como cabeçalho, e não como filtro.
 *
 * [total] e [naFila] são do curso inteiro, sem passar pela busca nem pelo nível:
 * o cabeçalho recolhido continua dizendo quanta coisa está ali dentro e quanto
 * dela pede revisão, e é isso que faz fechar um grupo não custar informação.
 */
data class GrupoDeIdioma(
    val par: ParIdiomas,
    val entradas: List<Entrada>,
    val total: Int,
    val naFila: Int,
    val recolhido: Boolean,
) {
    val vazioPorFiltro: Boolean get() = total > 0 && entradas.isEmpty()
}

data class HomeEstado(
    val grupos: List<GrupoDeIdioma> = emptyList(),
    val filtro: FiltroMemoria = FiltroMemoria.TODAS,
    val busca: String = "",
    val total: Int = 0,
    val dominadas: Int = 0,
    val carregado: Boolean = false,
) {
    val encontradas: Int get() = grupos.sumOf { it.entradas.size }
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
class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val repositorio = AppContainer.repositorio(app)
    private val preferencias = AppContainer.preferencias(app)
    private val filtro = MutableStateFlow(FiltroMemoria.TODAS)
    private val busca = MutableStateFlow("")

    /** Em duas etapas: `combine` só tem sobrecarga tipada até cinco fluxos. */
    private val recorte = combine(filtro, busca, ::Pair)

    val estado: StateFlow<HomeEstado> = combine(
        repositorio.observarProntas(Escopo.Todos),
        preferencias.observarCursos(),
        preferencias.observarPar(),
        preferencias.observarGruposRecolhidos(),
        recorte,
    ) { prontas, matriculados, par, recolhidos, (filtroAtual, termo) ->
        val agora = System.currentTimeMillis()
        val procurado = termo.normalizado()
        val porCurso = prontas.groupBy { it.par.alvo }

        HomeEstado(
            // A ordem é a da matrícula, e não a da quantidade de fichas: é a mesma
            // ordem da faixa da Início, e trocá-la aqui faria as duas telas
            // discordarem sobre onde fica o francês.
            grupos = matriculados.map { alvo ->
                val doCurso = porCurso[alvo].orEmpty()
                GrupoDeIdioma(
                    par = ParIdiomas(nativo = par.nativo, alvo = alvo),
                    entradas = doCurso.filter { cabe(it, filtroAtual, procurado, agora) },
                    total = doCurso.size,
                    naFila = doCurso.count { it.precisaRevisar(agora) },
                    recolhido = alvo in recolhidos,
                )
            },
            filtro = filtroAtual,
            busca = termo,
            total = prontas.size,
            dominadas = prontas.count { Degraus.nivel(it.degrau) == NivelMemoria.DOMINADA },
            carregado = true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeEstado())

    private fun cabe(entrada: Entrada, filtro: FiltroMemoria, procurado: String, agora: Long): Boolean {
        val nivel = entrada.retencao?.nivelEm(agora) ?: NivelMemoria.NOVA
        val bateNivel = when (filtro) {
            FiltroMemoria.TODAS -> true
            FiltroMemoria.APRENDENDO -> nivel == NivelMemoria.NOVA || nivel == NivelMemoria.APRENDENDO
            FiltroMemoria.FAMILIAR -> nivel == NivelMemoria.FAMILIAR
            FiltroMemoria.DOMINADA -> nivel == NivelMemoria.DOMINADA
        }
        val bateBusca = procurado.isBlank() ||
            entrada.alvo.orEmpty().normalizado().contains(procurado) ||
            entrada.ficha?.traducao.orEmpty().normalizado().contains(procurado)
        return bateNivel && bateBusca
    }

    fun filtrar(novo: FiltroMemoria) { filtro.value = novo }
    fun buscar(texto: String) { busca.value = texto }
    fun alternarGrupo(alvo: String) = preferencias.alternarGrupo(alvo)
}

private val espacos = Regex("\\s+")
private fun String.normalizado() = trim().lowercase().replace(espacos, " ")
