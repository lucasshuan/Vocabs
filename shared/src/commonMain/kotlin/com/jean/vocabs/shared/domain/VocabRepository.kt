package com.jean.vocabs.shared.domain

import kotlinx.coroutines.flow.Flow

/** O que o cartão de revisão da home precisa, já resolvido para o instant current. */
data class ReviewSummary(
    val inQueue: Int,
    /** Millis até a próxima word pedir revisão. Nulo quando não há card nenhuma. */
    val nextInMillis: Long?,
    val dayStreak: Int,
    val reviewedToday: Boolean,
    val bestStreak: Int = 0,
    val quota: DailyQuota = DailyQuota(done = 0, inQueue = inQueue),
)

/** A barra da card, já resolvida — depende do relógio, e o relógio mora no repositório. */
data class RetentionNow(
    val points: Double,
    val level: MemoryLevel,
    val nextInMillis: Long,
    val reviews: Int,
    val hits: Int,
    val misses: Int,
) {
    val answered: Int get() = hits + misses
    val hitRate: Double? get() = if (answered == 0) null else hits.toDouble() / answered
}

/**
 * O curso aberto é o padrão, e não a única resposta possível.
 *
 * Toda leitura recortável aceita um [Scope], e ele começa em
 * [Scope.CursoAberto]: uma tela que esquecesse de escolher mostraria o curso em
 * que a pessoa está, que é o certo em quase todas. Vocabulários, Pendentes e Você
 * pedem [Scope.Todos] de propósito; "Seu progresso" pede um curso nomeado, que
 * pode não ser o aberto.
 */
interface VocabRepository {

    /** Qual course está aberto now — o padrão de [Scope.CursoAberto]. */
    fun observeActiveCourse(): Flow<LanguagePair>

    /** Um resumo por course, com o badge da faixa já resolvido. Sempre de todos. */
    fun observeCourses(): Flow<List<CourseSummary>>

    /** Só o que já virou card. */
    fun observeReady(scope: Scope = Scope.ActiveCourse): Flow<List<Entry>>

    /** Entradas que ainda estão na fila da IA, sendo geradas ou falharam. */
    fun observeInbox(scope: Scope = Scope.ActiveCourse): Flow<List<Entry>>

    /** Capturas ainda aguardando transcrição ou seleção confirmada. */
    fun observePendingCaptures(scope: Scope = Scope.ActiveCourse): Flow<List<Capture>>

    fun observeCaptureById(id: Long): Flow<Capture?>

    fun observeById(id: Long): Flow<Entry?>

    /** As entries de um punhado de ids — o que a tela "Guardado" acompanha. */
    fun observeEntries(ids: List<Long>): Flow<List<Entry>>

    /** A fila de now: fichas cuja força de memória já caiu abaixo do limiar. */
    fun observeReviewQueue(scope: Scope = Scope.ActiveCourse): Flow<List<Entry>>

    fun observeReviewSummary(scope: Scope = Scope.ActiveCourse): Flow<ReviewSummary>

    fun observeRetention(id: Long): Flow<RetentionNow?>

    fun observeActivity(days: Int = 84): Flow<List<DailyActivity>>

    /** A row do tempo da tela Dia a day, do mais recente para o mais antigo. */
    fun observeEvents(days: Int = 84, scope: Scope = Scope.ActiveCourse): Flow<List<Event>>

    fun observeAiUsage(): Flow<AiUsage>

    /** Retrato consistente usado pelo ZIP de portabilidade local. */
    suspend fun exportData(): ExportData

    /** Cria uma capture textual e todas as fichas selecionadas numa transação. */
    suspend fun captureText(
        snippet: String,
        alvos: List<SelectedTarget>,
        languagePair: LanguagePair? = null,
    ): List<Long>

    /**
     * Guarda o trecho colado antes de haver seleção nenhuma.
     *
     * É o que faz o "Continuar" da folha ser barato: a partir daqui, fechar o
     * app ou desistir da seleção deixa a captura em Pendentes, no idioma que já
     * foi escolhido, em vez de perder o que a pessoa colou.
     */
    suspend fun captureSnippet(snippet: String, languagePair: LanguagePair? = null): Long

    /**
     * Guarda foto ou áudio como captura em transcrição. O arquivo fica seguro
     * antes de OCR/voz começar e sempre pode seguir para edição manual.
     */
    suspend fun captureMedia(
        format: CaptureFormat,
        path: String,
        durationMs: Long? = null,
        languagePair: LanguagePair? = null,
    ): Long

    /**
     * Troca o idioma de destino de uma captura ainda não processada.
     *
     * Só faz sentido antes da seleção: depois dela existem fichas nascidas nesse
     * par, e mudá-lo por baixo delas as deixaria órfãs do próprio idioma.
     */
    suspend fun changeCaptureLanguage(id: Long, target: String)

    /** Conclui a tentativa automática; error não impede a edição manual. */
    suspend fun recordTranscription(id: Long, snippet: String?, error: String? = null)

    /** Confirma o text editado e cria uma entry por seleção. */
    suspend fun confirmCapture(
        id: Long,
        snippet: String,
        alvos: List<SelectedTarget>,
    ): List<Long>

    /** Chama o servidor e grava o result (ou o error) na entry. */
    suspend fun generateCard(id: Long): Boolean

    /** Processa entries independentes com no máximo duas requisições simultâneas. */
    suspend fun generateCards(ids: List<Long>, concorrencia: Int = 2): List<Boolean>

    /** Grava o result de um cartão e marca o day no calendário de revisões. */
    suspend fun recordAnswer(id: Long, correct: Boolean)

    /** Descarta a entry e o file de míday, se houver. */
    suspend fun excluir(id: Long)

    suspend fun deleteCapture(id: Long)
}
