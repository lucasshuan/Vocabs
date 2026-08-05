package com.jean.vocabs.shared.domain

import com.jean.vocabs.contracts.TargetType

/** O contexto bruto do qual podem nascer uma ou várias fichas. */
data class Captura(
    val id: Long,
    val trecho: String?,
    val origem: String?,
    val criadoEm: Long,
    val status: CaptureStatus,
    val formato: CaptureFormat,
    val midiaCaminho: String?,
    val duracaoMs: Long?,
    val erroTranscricao: String?,
    /** O curso em que ela nasceu. Um trecho está numa língua só. */
    val par: ParIdiomas = ParIdiomas.PADRAO,
) {
    val aguardandoSelecao: Boolean get() = status == CaptureStatus.AWAITING_SELECTION
    val transcrevendo: Boolean get() = status == CaptureStatus.TRANSCRIBING
}

enum class CaptureStatus {
    TRANSCRIBING,
    AWAITING_SELECTION,
    PROCESSED;

    companion object {
        fun de(valor: String): CaptureStatus =
            entries.firstOrNull { it.name == valor } ?: AWAITING_SELECTION
    }
}

/** Um intervalo confirmado dentro do trecho. Intervalos podem se sobrepor. */
data class AlvoSelecionado(
    val texto: String,
    val inicio: Int,
    val fim: Int,
    val tipo: TargetType,
)

data class AtividadeDiaria(
    val dia: Long,
    val revisoes: Int,
)

data class UsoIa(
    val mes: String,
    val usadas: Int,
    val limite: Int = LIMITE_MENSAL_IA,
) {
    val fracao: Float get() = (usadas.toFloat() / limite.coerceAtLeast(1)).coerceIn(0f, 1f)

    companion object {
        const val LIMITE_MENSAL_IA = 100
    }
}

data class DadosExportacao(
    val capturas: List<Captura>,
    val entradas: List<Entrada>,
    val atividade: List<AtividadeDiaria>,
    val usoIa: UsoIa,
)
