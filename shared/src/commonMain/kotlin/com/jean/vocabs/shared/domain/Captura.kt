package com.jean.vocabs.shared.domain

import com.jean.vocabs.contracts.TipoAlvo

/** O contexto bruto do qual podem nascer uma ou várias fichas. */
data class Captura(
    val id: Long,
    val trecho: String?,
    val origem: String?,
    val criadoEm: Long,
    val status: StatusCaptura,
    val formato: FormatoCaptura,
    val midiaCaminho: String?,
    val duracaoMs: Long?,
    val erroTranscricao: String?,
    /** O curso em que ela nasceu. Um trecho está numa língua só. */
    val par: ParIdiomas = ParIdiomas.PADRAO,
) {
    val aguardandoSelecao: Boolean get() = status == StatusCaptura.AGUARDANDO_SELECAO
    val transcrevendo: Boolean get() = status == StatusCaptura.TRANSCREVENDO
}

enum class StatusCaptura {
    TRANSCREVENDO,
    AGUARDANDO_SELECAO,
    PROCESSADA;

    companion object {
        fun de(valor: String): StatusCaptura =
            entries.firstOrNull { it.name == valor } ?: AGUARDANDO_SELECAO
    }
}

/** Um intervalo confirmado dentro do trecho. Intervalos podem se sobrepor. */
data class AlvoSelecionado(
    val texto: String,
    val inicio: Int,
    val fim: Int,
    val tipo: TipoAlvo,
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
