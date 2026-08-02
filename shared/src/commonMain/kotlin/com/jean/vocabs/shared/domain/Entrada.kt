package com.jean.vocabs.shared.domain

import com.jean.vocabs.contracts.FichaResponse
import com.jean.vocabs.contracts.TipoAlvo

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
    val tipo: TipoAlvo,
    val origem: String?,
    val criadoEm: Long,
    val status: StatusEntrada,
    val formato: FormatoCaptura,
    val midiaCaminho: String?,
    val ficha: FichaResponse?,
    val retencao: Retencao?,
    val erro: String?,
) {
    fun precisaRevisar(agora: Long): Boolean = retencao?.precisaRevisar(agora) == true

    /** O que mostrar como título quando ainda não há alvo digitado. */
    val titulo: String
        get() = alvo?.takeIf { it.isNotBlank() } ?: when (formato) {
            FormatoCaptura.FOTO -> "Foto sem transcrição"
            FormatoCaptura.AUDIO -> "Áudio sem transcrição"
            FormatoCaptura.TEXTO -> "Sem título"
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

private fun prioridadeDuplicata(status: StatusEntrada): Int = when (status) {
    StatusEntrada.PRONTA -> 0
    StatusEntrada.GERANDO -> 1
    StatusEntrada.PENDENTE -> 2
    StatusEntrada.ERRO -> 3
}

/**
 * Como o sinal entrou no app.
 *
 * O ponto da Fase 1.5 é que cada contexto tem uma restrição diferente — jogando
 * você não quer sair do jogo, lendo as mãos estão ocupadas. Foto e áudio existem
 * para capturar em segundos e resolver depois, não para serem processados na hora.
 */
enum class FormatoCaptura {
    TEXTO,
    FOTO,
    AUDIO;

    companion object {
        fun de(valor: String?): FormatoCaptura =
            entries.firstOrNull { it.name == valor } ?: TEXTO
    }
}

/**
 * O que sustenta o critério de saída da Fase 1: a captura grava e volta na hora
 * (PENDENTE), e a geração da ficha acontece depois, em background.
 *
 * O rascunho agora pertence a [Captura]; uma entrada só existe depois que um
 * alvo foi confirmado.
 */
enum class StatusEntrada {
    PENDENTE,
    GERANDO,
    PRONTA,
    ERRO;

    companion object {
        fun de(valor: String): StatusEntrada =
            entries.firstOrNull { it.name == valor } ?: PENDENTE
    }
}
