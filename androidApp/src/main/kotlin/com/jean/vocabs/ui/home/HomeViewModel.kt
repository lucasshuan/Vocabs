package com.jean.vocabs.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jean.vocabs.shared.AppContainer
import com.jean.vocabs.shared.domain.Entrada
import com.jean.vocabs.shared.domain.ResumoRevisao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HomeEstado(
    /** Já filtradas pela origem selecionada. */
    val entradas: List<Entrada> = emptyList(),
    /** Origens distintas presentes nos dados — viram os chips de filtro. */
    val origens: List<String> = emptyList(),
    val filtro: String? = null,
    val total: Int = 0,
    val revisao: ResumoRevisao? = null,
    /** False só no instante entre abrir a tela e a primeira emissão do banco —
     * evita um flash do estado vazio para quem já tem palavras. */
    val carregado: Boolean = false,
)

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val repositorio = AppContainer.repositorio(app)
    private val filtro = MutableStateFlow<String?>(null)

    /**
     * Só fichas prontas. O que ainda está cru vive no inbox — essa separação é o
     * que a Fase 1.5 pede, e ela também deixa a home ser uma lista de conquistas
     * em vez de uma lista de pendências.
     *
     * A lista vem direto do Flow do SQLDelight: quando a ficha chega em
     * background, isto reemite sozinho. Sem refresh manual, sem polling.
     */
    val estado: StateFlow<HomeEstado> =
        combine(
            repositorio.observarProntas(),
            filtro,
            repositorio.observarResumoDeRevisao(),
        ) { prontas, origem, revisao ->
            HomeEstado(
                entradas = if (origem == null) prontas
                else prontas.filter { it.origem?.equals(origem, ignoreCase = true) == true },
                origens = prontas
                    .mapNotNull { entrada -> entrada.origem?.trim()?.takeIf { it.isNotEmpty() } }
                    .distinctBy { it.lowercase() },
                filtro = origem,
                total = prontas.size,
                revisao = revisao,
                carregado = true,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeEstado())

    fun filtrar(origem: String?) {
        filtro.value = origem
    }
}
