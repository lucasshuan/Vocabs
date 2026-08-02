package com.jean.vocabs.ui.perfil

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jean.vocabs.shared.AppContainer
import com.jean.vocabs.shared.domain.AtividadeDiaria
import com.jean.vocabs.shared.domain.NivelMemoria
import com.jean.vocabs.shared.domain.UsoIa
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PerfilEstado(
    val totalPalavras: Int = 0,
    val dominadas: Int = 0,
    val pendentes: Int = 0,
    val diasSeguidos: Int = 0,
    val acertos: Int = 0,
    val respondidas: Int = 0,
    val atividade: List<AtividadeDiaria> = emptyList(),
    val usoIa: UsoIa = UsoIa("", 0),
    val exportando: Boolean = false,
) {
    val taxaDeAcerto: Double? get() = if (respondidas == 0) null else acertos.toDouble() / respondidas
}

private data class PerfilBase(
    val total: Int,
    val dominadas: Int,
    val pendentes: Int,
    val dias: Int,
    val acertos: Int,
    val respondidas: Int,
)

class PerfilViewModel(app: Application) : AndroidViewModel(app) {
    private val repositorio = AppContainer.repositorio(app)
    private val exportando = MutableStateFlow(false)

    private val base = combine(
        repositorio.observarProntas(),
        repositorio.observarResumoDeRevisao(),
        repositorio.observarCapturasPendentes(),
        repositorio.observarInbox(),
    ) { prontas, revisao, capturas, fichasPendentes ->
        val agora = System.currentTimeMillis()
        PerfilBase(
            prontas.size,
            prontas.count { it.retencao?.nivelEm(agora) == NivelMemoria.DOMINADA },
            capturas.size + fichasPendentes.size,
            revisao.diasSeguidos,
            prontas.sumOf { it.retencao?.acertos ?: 0 },
            prontas.sumOf { it.retencao?.respondidas ?: 0 },
        )
    }

    val estado: StateFlow<PerfilEstado> = combine(
        base,
        repositorio.observarAtividade(84),
        repositorio.observarUsoIa(),
        exportando,
    ) { base, atividade, usoIa, estaExportando ->
        PerfilEstado(base.total, base.dominadas, base.pendentes, base.dias, base.acertos, base.respondidas, atividade, usoIa, estaExportando)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PerfilEstado())

    fun exportar(aoPronto: (File) -> Unit, aoErro: (String) -> Unit) {
        if (exportando.value) return
        viewModelScope.launch {
            exportando.value = true
            runCatching {
                ExportadorTagarara.criar(getApplication(), repositorio.dadosParaExportacao())
            }.onSuccess(aoPronto).onFailure { aoErro(it.message ?: "Não foi possível exportar os dados.") }
            exportando.value = false
        }
    }
}
