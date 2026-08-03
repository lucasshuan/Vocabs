package com.jean.vocabs.shared.domain

import kotlinx.coroutines.flow.Flow

/** O que o cartão de revisão da home precisa, já resolvido para o instante atual. */
data class ResumoRevisao(
    val naFila: Int,
    /** Millis até a próxima palavra pedir revisão. Nulo quando não há ficha nenhuma. */
    val proximaEmMillis: Long?,
    val diasSeguidos: Int,
    val revisouHoje: Boolean,
    val melhorSequencia: Int = 0,
    val quota: QuotaDoDia = QuotaDoDia(feita = 0, naFila = naFila),
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

/**
 * Tudo aqui é do **curso aberto**.
 *
 * O par de idiomas não é parâmetro de cada método porque ele não é uma pergunta
 * que cada tela faz: é o contexto em que o app inteiro está. Uma tela que
 * esquecesse de passar o par mostraria palavras alemãs numa sessão de inglês, e
 * é exatamente esse esquecimento que a assinatura evita. As duas exceções estão
 * marcadas com `DeTodosOsCursos` no nome.
 */
interface VocabRepository {

    /** Qual curso está aberto agora — o que filtra todo o resto desta interface. */
    fun observarCursoAtivo(): Flow<ParIdiomas>

    /** A faixa de idiomas da tela Você: quanto vocabulário existe em cada curso. */
    fun observarCursos(): Flow<List<ResumoCurso>>

    /** A home observa isto: só o que já virou ficha. */
    fun observarProntas(): Flow<List<Entrada>>

    /** Entradas que ainda estão na fila da IA, sendo geradas ou falharam. */
    fun observarInbox(): Flow<List<Entrada>>

    /** Capturas de mídia ainda aguardando uma seleção confirmada. */
    fun observarCapturasPendentes(): Flow<List<Captura>>

    fun observarCapturaPorId(id: Long): Flow<Captura?>

    fun observarPorId(id: Long): Flow<Entrada?>

    /** A fila de agora: fichas cuja força de memória já caiu abaixo do limiar. */
    fun observarFilaDeRevisao(): Flow<List<Entrada>>

    fun observarResumoDeRevisao(): Flow<ResumoRevisao>

    fun observarRetencao(id: Long): Flow<RetencaoAgora?>

    fun observarAtividade(dias: Int = 84): Flow<List<AtividadeDiaria>>

    /** A linha do tempo da tela Dia a dia, do mais recente para o mais antigo. */
    fun observarEventos(dias: Int = 84): Flow<List<Evento>>

    fun observarUsoIa(): Flow<UsoIa>

    /** Retrato consistente usado pelo ZIP de portabilidade local. */
    suspend fun dadosParaExportacao(): DadosExportacao

    /** Cria uma captura textual e todas as fichas selecionadas numa transação. */
    suspend fun capturarTexto(trecho: String, alvos: List<AlvoSelecionado>): List<Long>

    /**
     * Guarda foto ou áudio como captura em transcrição. O arquivo fica seguro
     * antes de OCR/voz começar e sempre pode seguir para edição manual.
     */
    suspend fun capturarMidia(
        formato: FormatoCaptura,
        caminho: String,
        duracaoMs: Long? = null,
    ): Long

    /** Conclui a tentativa automática; erro não impede a edição manual. */
    suspend fun registrarTranscricao(id: Long, trecho: String?, erro: String? = null)

    /** Confirma o texto editado e cria uma entrada por seleção. */
    suspend fun confirmarCaptura(
        id: Long,
        trecho: String,
        alvos: List<AlvoSelecionado>,
    ): List<Long>

    /** Chama o servidor e grava o resultado (ou o erro) na entrada. */
    suspend fun gerarFicha(id: Long): Boolean

    /** Processa entradas independentes com no máximo duas requisições simultâneas. */
    suspend fun gerarFichas(ids: List<Long>, concorrencia: Int = 2): List<Boolean>

    /** Grava o resultado de um cartão e marca o dia no calendário de revisões. */
    suspend fun registrarResposta(id: Long, acertou: Boolean)

    /** Descarta a entrada e o arquivo de mídia, se houver. */
    suspend fun excluir(id: Long)

    suspend fun excluirCaptura(id: Long)
}
