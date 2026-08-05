package com.jean.vocabs.shared.domain

import com.jean.vocabs.contracts.CardResponse
import com.jean.vocabs.contracts.ErrorCode
import com.jean.vocabs.contracts.TargetType

/**
 * Um alvo confirmado dentro de uma captura e, quando pronta, sua ficha.
 *
 * [ficha] reusa o tipo do contrato de propósito: um terceiro formato só para o
 * domínio seria uma cópia a mais para manter em sincronia, sem ganho nenhum.
 *
 * [trecho] pode ser nulo apenas em dados legados incompletos. Capturas cruas de
 * mídia vivem em [Captura] e só ganham entradas depois da seleção.
 *
 * [retencao] segue a mesma regra de [ficha]: existe se e somente se há ficha
 * para revisar.
 */
data class Entrada(
    val id: Long,
    val capturaId: Long,
    val trecho: String?,
    val alvo: String?,
    val inicio: Int?,
    val fim: Int?,
    val tipo: TargetType,
    val origem: String?,
    val criadoEm: Long,
    val status: EntryStatus,
    val formato: CaptureFormat,
    val midiaCaminho: String?,
    val ficha: CardResponse?,
    val retencao: Retencao?,
    /** Qual falha, para a tela escolher o texto. Nulo quando não houve. */
    val errorCode: ErrorCode?,
    /** O que não se traduz: a frase crua do provedor, ou o status HTTP. */
    val errorDetail: String?,
    /** Herdado da captura: o par em que esta ficha nasceu e no qual ela é regerada. */
    val par: ParIdiomas = ParIdiomas.PADRAO,
) {
    fun precisaRevisar(agora: Long): Boolean = retencao?.precisaRevisar(agora) == true

    /** Em que degrau da escada de "O que falta" ela está. */
    val degrau: Int get() = Degraus.de(retencao)

    /** O que mostrar como título quando ainda não há alvo digitado. */
    val titulo: String
        get() = alvo?.takeIf { it.isNotBlank() } ?: when (formato) {
            CaptureFormat.PHOTO -> "Foto sem transcrição"
            CaptureFormat.AUDIO -> "Áudio sem transcrição"
            CaptureFormat.TEXT -> "Sem título"
    }
}

private val espacosDoAlvo = Regex("\\s+")

fun duplicataDeAlvo(
    alvo: String,
    entradas: Iterable<Entrada>,
    ignorarId: Long? = null,
): Entrada? {
    val procurado = normalizarAlvo(alvo)
    if (procurado.isBlank()) return null

    return entradas
        .asSequence()
        .filter { entrada -> entrada.id != ignorarId }
        .filter { entrada -> normalizarAlvo(entrada.alvo) == procurado }
        .sortedWith(
            compareBy<Entrada> { prioridadeDuplicata(it.status) }
                .thenByDescending { it.criadoEm },
        )
        .firstOrNull()
}

private fun normalizarAlvo(valor: String?): String =
    valor.orEmpty().trim().lowercase().replace(espacosDoAlvo, " ")

private fun prioridadeDuplicata(status: EntryStatus): Int = when (status) {
    EntryStatus.READY -> 0
    EntryStatus.GENERATING -> 1
    EntryStatus.PENDING -> 2
    EntryStatus.ERROR -> 3
}

/**
 * Como o sinal entrou no app.
 *
 * O ponto da Fase 1.5 é que cada contexto tem uma restrição diferente — jogando
 * você não quer sair do jogo, lendo as mãos estão ocupadas. Foto e áudio existem
 * para capturar em segundos e resolver depois, não para serem processados na hora.
 */
enum class CaptureFormat {
    TEXT,
    PHOTO,
    AUDIO;

    companion object {
        fun de(valor: String?): CaptureFormat =
            entries.firstOrNull { it.name == valor } ?: TEXT
    }
}

/**
 * O que sustenta o critério de saída da Fase 1: a captura grava e volta na hora
 * (PENDING), e a geração da ficha acontece depois, em background.
 *
 * O rascunho agora pertence a [Captura]; uma entrada só existe depois que um
 * alvo foi confirmado.
 */
enum class EntryStatus {
    PENDING,
    GENERATING,
    READY,
    ERROR;

    companion object {
        fun de(valor: String): EntryStatus =
            entries.firstOrNull { it.name == valor } ?: PENDING
    }
}
