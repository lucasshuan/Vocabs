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
    val nivel: MemoryLevel,
    val proximaEmMillis: Long,
    val revisoes: Int,
    val acertos: Int,
    val erros: Int,
) {
    val respondidas: Int get() = acertos + erros
    val taxaDeAcerto: Double? get() = if (respondidas == 0) null else acertos.toDouble() / respondidas
}

/**
 * O curso aberto é o padrão, e não a única resposta possível.
 *
 * Toda leitura recortável aceita um [Escopo], e ele começa em
 * [Escopo.CursoAberto]: uma tela que esquecesse de escolher mostraria o curso em
 * que a pessoa está, que é o certo em quase todas. Vocabulários, Pendentes e Você
 * pedem [Escopo.Todos] de propósito; "Seu progresso" pede um curso nomeado, que
 * pode não ser o aberto.
 */
interface VocabRepository {

    /** Qual curso está aberto agora — o padrão de [Escopo.CursoAberto]. */
    fun observarCursoAtivo(): Flow<ParIdiomas>

    /** Um resumo por curso, com o selo da faixa já resolvido. Sempre de todos. */
    fun observarCursos(): Flow<List<ResumoCurso>>

    /** Só o que já virou ficha. */
    fun observarProntas(escopo: Escopo = Escopo.CursoAberto): Flow<List<Entrada>>

    /** Entradas que ainda estão na fila da IA, sendo geradas ou falharam. */
    fun observarInbox(escopo: Escopo = Escopo.CursoAberto): Flow<List<Entrada>>

    /** Capturas ainda aguardando transcrição ou seleção confirmada. */
    fun observarCapturasPendentes(escopo: Escopo = Escopo.CursoAberto): Flow<List<Captura>>

    fun observarCapturaPorId(id: Long): Flow<Captura?>

    fun observarPorId(id: Long): Flow<Entrada?>

    /** As entradas de um punhado de ids — o que a tela "Guardado" acompanha. */
    fun observarEntradas(ids: List<Long>): Flow<List<Entrada>>

    /** A fila de agora: fichas cuja força de memória já caiu abaixo do limiar. */
    fun observarFilaDeRevisao(escopo: Escopo = Escopo.CursoAberto): Flow<List<Entrada>>

    fun observarResumoDeRevisao(escopo: Escopo = Escopo.CursoAberto): Flow<ResumoRevisao>

    fun observarRetencao(id: Long): Flow<RetencaoAgora?>

    fun observarAtividade(dias: Int = 84): Flow<List<AtividadeDiaria>>

    /** A linha do tempo da tela Dia a dia, do mais recente para o mais antigo. */
    fun observarEventos(dias: Int = 84, escopo: Escopo = Escopo.CursoAberto): Flow<List<Evento>>

    fun observarUsoIa(): Flow<UsoIa>

    /** Retrato consistente usado pelo ZIP de portabilidade local. */
    suspend fun dadosParaExportacao(): DadosExportacao

    /** Cria uma captura textual e todas as fichas selecionadas numa transação. */
    suspend fun capturarTexto(
        trecho: String,
        alvos: List<AlvoSelecionado>,
        par: ParIdiomas? = null,
    ): List<Long>

    /**
     * Guarda o trecho colado antes de haver seleção nenhuma.
     *
     * É o que faz o "Continuar" da folha ser barato: a partir daqui, fechar o
     * app ou desistir da seleção deixa a captura em Pendentes, no idioma que já
     * foi escolhido, em vez de perder o que a pessoa colou.
     */
    suspend fun capturarTrecho(trecho: String, par: ParIdiomas? = null): Long

    /**
     * Guarda foto ou áudio como captura em transcrição. O arquivo fica seguro
     * antes de OCR/voz começar e sempre pode seguir para edição manual.
     */
    suspend fun capturarMidia(
        formato: CaptureFormat,
        caminho: String,
        duracaoMs: Long? = null,
        par: ParIdiomas? = null,
    ): Long

    /**
     * Troca o idioma de destino de uma captura ainda não processada.
     *
     * Só faz sentido antes da seleção: depois dela existem fichas nascidas nesse
     * par, e mudá-lo por baixo delas as deixaria órfãs do próprio idioma.
     */
    suspend fun alterarIdiomaDaCaptura(id: Long, alvo: String)

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
