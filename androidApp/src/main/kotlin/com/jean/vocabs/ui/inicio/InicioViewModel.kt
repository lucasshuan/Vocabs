package com.jean.vocabs.ui.inicio

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jean.vocabs.shared.AppContainer
import com.jean.vocabs.shared.domain.Captura
import com.jean.vocabs.shared.domain.Entrada
import com.jean.vocabs.shared.domain.NivelMemoria
import com.jean.vocabs.shared.domain.ResumoRevisao
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class InicioEstado(
    val totalPalavras: Int = 0,
    val dominadas: Int = 0,
    val forcaMedia: Int = 0,
    val revisao: ResumoRevisao? = null,
    /** Só capturas cruas: é isso que o aviso da tela promete que dá para resolver. */
    val capturasPendentes: Int = 0,
    val capturaMaisAntiga: Captura? = null,
    val recentesHoje: List<Entrada> = emptyList(),
)

class InicioViewModel(app: Application) : AndroidViewModel(app) {
    private val repositorio = AppContainer.repositorio(app)

    val estado: StateFlow<InicioEstado> = combine(
        repositorio.observarProntas(),
        repositorio.observarResumoDeRevisao(),
        repositorio.observarCapturasPendentes(),
    ) { prontas, revisao, capturas ->
        val agora = System.currentTimeMillis()
        val hoje = LocalDate.now()
        InicioEstado(
            totalPalavras = prontas.size,
            dominadas = prontas.count { it.retencao?.nivelEm(agora) == NivelMemoria.DOMINADA },
            forcaMedia = prontas.mapNotNull { it.retencao?.pontosEm(agora) }.averageOrZero().toInt(),
            revisao = revisao,
            capturasPendentes = capturas.size,
            capturaMaisAntiga = capturas.firstOrNull(),
            recentesHoje = prontas.filter {
                Instant.ofEpochMilli(it.criadoEm).atZone(ZoneId.systemDefault()).toLocalDate() == hoje
            }.take(3),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InicioEstado())
}

private fun List<Double>.averageOrZero(): Double = if (isEmpty()) 0.0 else average()
