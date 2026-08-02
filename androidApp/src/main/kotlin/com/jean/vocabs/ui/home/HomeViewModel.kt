package com.jean.vocabs.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jean.vocabs.shared.AppContainer
import com.jean.vocabs.shared.domain.Entrada
import com.jean.vocabs.shared.domain.NivelMemoria
import com.jean.vocabs.shared.domain.ResumoRevisao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class FiltroMemoria(val rotulo: String) { TODAS("Todas"), APRENDENDO("Aprendendo"), FAMILIAR("Familiar"), DOMINADA("Dominada") }

data class HomeEstado(
    val entradas: List<Entrada> = emptyList(),
    val filtro: FiltroMemoria = FiltroMemoria.TODAS,
    val busca: String = "",
    val total: Int = 0,
    val dominadas: Int = 0,
    val revisao: ResumoRevisao? = null,
    val carregado: Boolean = false,
)

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val repositorio = AppContainer.repositorio(app)
    private val filtro = MutableStateFlow(FiltroMemoria.TODAS)
    private val busca = MutableStateFlow("")

    val estado: StateFlow<HomeEstado> = combine(
        repositorio.observarProntas(),
        repositorio.observarResumoDeRevisao(),
        filtro,
        busca,
    ) { prontas, revisao, filtroAtual, termo ->
        val agora = System.currentTimeMillis()
        val procurado = termo.normalizado()
        HomeEstado(
            entradas = prontas.filter { entrada ->
                val nivel = entrada.retencao?.nivelEm(agora) ?: NivelMemoria.NOVA
                val bateNivel = when (filtroAtual) {
                    FiltroMemoria.TODAS -> true
                    FiltroMemoria.APRENDENDO -> nivel == NivelMemoria.NOVA || nivel == NivelMemoria.APRENDENDO
                    FiltroMemoria.FAMILIAR -> nivel == NivelMemoria.FAMILIAR
                    FiltroMemoria.DOMINADA -> nivel == NivelMemoria.DOMINADA
                }
                val bateBusca = procurado.isBlank() ||
                    entrada.alvo.orEmpty().normalizado().contains(procurado) ||
                    entrada.ficha?.traducao.orEmpty().normalizado().contains(procurado)
                bateNivel && bateBusca
            },
            filtro = filtroAtual,
            busca = termo,
            total = prontas.size,
            dominadas = prontas.count { it.retencao?.nivelEm(agora) == NivelMemoria.DOMINADA },
            revisao = revisao,
            carregado = true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeEstado())

    fun filtrar(novo: FiltroMemoria) { filtro.value = novo }
    fun buscar(texto: String) { busca.value = texto }
}

private val espacos = Regex("\\s+")
private fun String.normalizado() = trim().lowercase().replace(espacos, " ")
