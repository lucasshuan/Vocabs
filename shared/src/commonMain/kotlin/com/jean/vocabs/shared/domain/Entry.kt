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
 * mídia vivem em [Capture] e só ganham entradas depois da seleção.
 *
 * [retencao] segue a mesma regra de [ficha]: existe se e somente se há ficha
 * para revisar.
 */
data class Entry(
    val id: Long,
    val captureId: Long,
    val snippet: String?,
    val target: String?,
    val start: Int?,
    val end: Int?,
    val type: TargetType,
    val source: String?,
    val createdAt: Long,
    val status: EntryStatus,
    val format: CaptureFormat,
    val mediaPath: String?,
    val card: CardResponse?,
    val retention: Retention?,
    /** Qual falha, para a tela escolher o text. Nulo quando não houve. */
    val errorCode: ErrorCode?,
    /** O que não se traduz: a frase crua do provedor, ou o status HTTP. */
    val errorDetail: String?,
    /** Herdado da capture: o par em que esta card nasceu e no qual ela é regerada. */
    val languagePair: LanguagePair = LanguagePair.PADRAO,
) {
    fun needsReview(now: Long): Boolean = retention?.needsReview(now) == true

    /** Em que degrau da escada de "O que falta" ela está. */
    val degrau: Int get() = Steps.de(retention)

    /** O que mostrar como título quando ainda não há target digitado. */
    val title: String
        get() = target?.takeIf { it.isNotBlank() } ?: when (format) {
            CaptureFormat.PHOTO -> "Foto sem transcrição"
            CaptureFormat.AUDIO -> "Áudio sem transcrição"
            CaptureFormat.TEXT -> "Sem título"
    }
}

private val targetSpaces = Regex("\\s+")

fun duplicateOfTarget(
    target: String,
    entries: Iterable<Entry>,
    ignorarId: Long? = null,
): Entry? {
    val procurado = normalizeTarget(target)
    if (procurado.isBlank()) return null

    return entries
        .asSequence()
        .filter { entry -> entry.id != ignorarId }
        .filter { entry -> normalizeTarget(entry.target) == procurado }
        .sortedWith(
            compareBy<Entry> { duplicatePriority(it.status) }
                .thenByDescending { it.createdAt },
        )
        .firstOrNull()
}

private fun normalizeTarget(value: String?): String =
    value.orEmpty().trim().lowercase().replace(targetSpaces, " ")

private fun duplicatePriority(status: EntryStatus): Int = when (status) {
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
        fun de(value: String?): CaptureFormat =
            entries.firstOrNull { it.name == value } ?: TEXT
    }
}

/**
 * O que sustenta o critério de saída da Fase 1: a captura grava e volta na hora
 * (PENDING), e a geração da ficha acontece depois, em background.
 *
 * O rascunho agora pertence a [Capture]; uma entrada só existe depois que um
 * alvo foi confirmado.
 */
enum class EntryStatus {
    PENDING,
    GENERATING,
    READY,
    ERROR;

    companion object {
        fun de(value: String): EntryStatus =
            entries.firstOrNull { it.name == value } ?: PENDING
    }
}
