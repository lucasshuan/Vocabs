package com.jean.vocabs.ui.perfil

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jean.vocabs.shared.AppContainer
import com.jean.vocabs.shared.domain.NivelMemoria
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class PerfilEstado(
    val totalPalavras: Int = 0,
    val dominadas: Int = 0,
    val pendentes: Int = 0,
    val diasSeguidos: Int = 0,
    val revisouHoje: Boolean = false,
    val acertos: Int = 0,
    val respondidas: Int = 0,
) {
    /** Nulo quando ninguém respondeu nada ainda — 0% seria mentira, não zero. */
    val taxaDeAcerto: Double? get() =
        if (respondidas == 0) null else acertos.toDouble() / respondidas
}

class PerfilViewModel(app: Application) : AndroidViewModel(app) {

    private val repositorio = AppContainer.repositorio(app)

    val estado: StateFlow<PerfilEstado> =
        combine(
            repositorio.observarProntas(),
            repositorio.observarResumoDeRevisao(),
            repositorio.observarInbox(),
        ) { prontas, revisao, pendentes ->
            val agora = System.currentTimeMillis()
            PerfilEstado(
                totalPalavras = prontas.size,
                dominadas = prontas.count {
                    it.retencao?.nivelEm(agora) == NivelMemoria.DOMINADA
                },
                pendentes = pendentes.size,
                diasSeguidos = revisao.diasSeguidos,
                revisouHoje = revisao.revisouHoje,
                acertos = prontas.sumOf { it.retencao?.acertos ?: 0 },
                respondidas = prontas.sumOf { it.retencao?.respondidas ?: 0 },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PerfilEstado())
}
