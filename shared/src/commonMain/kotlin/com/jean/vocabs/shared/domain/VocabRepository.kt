package com.jean.vocabs.shared.domain

import kotlinx.coroutines.flow.Flow

/** O que o cartão de revisão da home precisa, já resolvido para o instante atual. */
data class ResumoRevisao(
    val naFila: Int,
    /** Millis até a próxima palavra pedir revisão. Nulo quando não há ficha nenhuma. */
    val proximaEmMillis: Long?,
    val diasSeguidos: Int,
    val revisouHoje: Boolean,
)

/** A barra da ficha, já resolvida — depende do relógio, e o relógio mora no repositório. */
data class RetencaoAgora(
    val pontos: Double,
    val nivel: NivelMemoria,
    val proximaEmMillis: Long,
    val revisoes: Int,
    val acertos: Int,
    val erros: Int,
) {
    val respondidas: Int get() = acertos + erros
    val taxaDeAcerto: Double? get() = if (respondidas == 0) null else acertos.toDouble() / respondidas
}

interface VocabRepository {

    /** A home observa isto: só o que já virou ficha. */
    fun observarProntas(): Flow<List<Entrada>>

    /** O inbox: tudo que ainda não é ficha — rascunho, na fila, gerando ou com erro. */
    fun observarInbox(): Flow<List<Entrada>>

    fun observarPorId(id: Long): Flow<Entrada?>

    /** A fila de agora: fichas cuja força de memória já caiu abaixo do limiar. */
    fun observarFilaDeRevisao(): Flow<List<Entrada>>

    fun observarResumoDeRevisao(): Flow<ResumoRevisao>

    fun observarRetencao(id: Long): Flow<RetencaoAgora?>

    /** Grava a captura de texto como PENDENTE e devolve na hora. Não espera a IA. */
    suspend fun capturarTexto(trecho: String, alvo: String, origem: String?): Long

    /**
     * Guarda uma foto ou um áudio como RASCUNHO. É a captura de 5 segundos: o
     * arquivo já está salvo e a transcrição fica para quando você tiver calma.
     */
    suspend fun capturarMidia(formato: FormatoCaptura, caminho: String, origem: String?): Long

    /** Transcreve um rascunho e o coloca na fila da IA. */
    suspend fun transcrever(id: Long, trecho: String, alvo: String, origem: String?)

    /** Chama o servidor e grava o resultado (ou o erro) na entrada. */
    suspend fun gerarFicha(id: Long)

    /** Grava o resultado de um cartão e marca o dia no calendário de revisões. */
    suspend fun registrarResposta(id: Long, acertou: Boolean)

    /** Descarta a entrada e o arquivo de mídia, se houver. */
    suspend fun excluir(id: Long)
}
