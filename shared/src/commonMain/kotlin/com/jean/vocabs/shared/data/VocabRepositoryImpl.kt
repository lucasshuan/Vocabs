package com.jean.vocabs.shared.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.jean.vocabs.contracts.CardResponse
import com.jean.vocabs.contracts.ErrorCode
import com.jean.vocabs.contracts.TargetType
import com.jean.vocabs.shared.data.remote.CardApi
import com.jean.vocabs.shared.data.remote.CardException
import com.jean.vocabs.shared.db.Capture as CaptureRow
import com.jean.vocabs.shared.db.Entry_with_capture as EntryRow
import com.jean.vocabs.shared.db.VocabsDatabase
import com.jean.vocabs.shared.domain.AiUsage
import com.jean.vocabs.shared.domain.Capture
import com.jean.vocabs.shared.domain.CaptureFormat
import com.jean.vocabs.shared.domain.CaptureStatus
import com.jean.vocabs.shared.domain.CourseSummary
import com.jean.vocabs.shared.domain.DailyActivity
import com.jean.vocabs.shared.domain.DailyQuota
import com.jean.vocabs.shared.domain.Entry
import com.jean.vocabs.shared.domain.EntryStatus
import com.jean.vocabs.shared.domain.Event
import com.jean.vocabs.shared.domain.EventType
import com.jean.vocabs.shared.domain.ExportData
import com.jean.vocabs.shared.domain.LanguagePair
import com.jean.vocabs.shared.domain.MemoryLevel
import com.jean.vocabs.shared.domain.Retention
import com.jean.vocabs.shared.domain.RetentionNow
import com.jean.vocabs.shared.domain.ReviewSummary
import com.jean.vocabs.shared.domain.Scope
import com.jean.vocabs.shared.domain.SelectedTarget
import com.jean.vocabs.shared.domain.Steps
import com.jean.vocabs.shared.domain.VocabRepository
import com.jean.vocabs.shared.domain.bestStreakOf
import com.jean.vocabs.shared.domain.isValidIn
import com.jean.vocabs.shared.domain.streakOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class VocabRepositoryImpl(
    private val db: VocabsDatabase,
    private val api: CardApi,
    private val io: CoroutineDispatcher,
    private val now: () -> Long,
    /**
     * O curso aberto, como fluxo.
     *
     * Entra pelo construtor porque a escolha é uma preferência do aparelho e o
     * repositório é comum aos dois lados do KMP. Ser fluxo (e não um getter) é o
     * que faz todas as telas se refazerem sozinhas quando a pessoa troca de
     * idioma na faixa — sem isso cada ViewModel teria que se reinscrever.
     */
    private val activeCourse: Flow<LanguagePair> = flowOf(LanguagePair.DEFAULT),
    private val removeFile: (String) -> Unit = {},
) : VocabRepository {

    private val json = Json { ignoreUnknownKeys = true }
    private val queries get() = db.vocabsQueries

    override fun observeActiveCourse(): Flow<LanguagePair> = activeCourse

    override fun observeCourses(): Flow<List<CourseSummary>> =
        allReady().map { prontas ->
            val instant = now()
            prontas
                .groupBy { it.languagePair }
                .map { (languagePair, entries) ->
                    CourseSummary(
                        languagePair = languagePair,
                        total = entries.size,
                        mastered = entries.count { Steps.level(it.step) == MemoryLevel.MASTERED },
                        inQueue = entries.count { it.needsReview(instant) },
                        nextInMillis = entries
                            .mapNotNull { it.retention?.nextReviewIn(instant) }
                            .minOrNull(),
                    )
                }
        }

    private fun allReady(): Flow<List<Entry>> =
        queries.listReady().asFlow().mapToList(io).map { linhas -> linhas.map(::toDomain) }

    override fun observeReady(scope: Scope): Flow<List<Entry>> =
        allReady().no(scope) { it.languagePair }

    override fun observeInbox(scope: Scope): Flow<List<Entry>> =
        queries.listInbox().asFlow().mapToList(io).map { linhas -> linhas.map(::toDomain) }
            .no(scope) { it.languagePair }

    override fun observePendingCaptures(scope: Scope): Flow<List<Capture>> =
        queries.listPendingCaptures().asFlow().mapToList(io)
            .map { linhas -> linhas.map(::captureToDomain) }
            .no(scope) { it.languagePair }

    /**
     * O recorte de [Scope] aplicado a uma lista já montada.
     *
     * Filtrar em memória e não em SQL é de propósito: o curso aberto é um fluxo
     * de preferência, e uma consulta parametrizada por ele teria que ser refeita
     * — e o cursor reaberto — a cada troca de idioma na faixa. A lista inteira de
     * fichas de um aparelho cabe folgada na memória; o cursor recriado a cada
     * deslize do carrossel, não.
     */
    private fun <T> Flow<List<T>>.no(scope: Scope, languagePair: (T) -> LanguagePair): Flow<List<T>> =
        combine(fitsScope(scope)) { itens, cabe -> itens.filter { cabe(languagePair(it)) } }

    private fun fitsScope(scope: Scope): Flow<(LanguagePair) -> Boolean> = when (scope) {
        Scope.All -> flowOf({ _: LanguagePair -> true })
        is Scope.Course -> flowOf({ languagePair: LanguagePair -> languagePair.target == scope.target })
        Scope.ActiveCourse -> activeCourse.map { aberto -> { languagePair: LanguagePair -> languagePair == aberto } }
    }

    override fun observeCaptureById(id: Long): Flow<Capture?> =
        queries.findCaptureById(id).asFlow().mapToOneOrNull(io)
            .map { it?.let(::captureToDomain) }

    override fun observeById(id: Long): Flow<Entry?> =
        queries.findEntryById(id).asFlow().mapToOneOrNull(io).map { it?.let(::toDomain) }

    override fun observeEntries(ids: List<Long>): Flow<List<Entry>> {
        // `IN ()` não é SQL válido no SQLite, e uma lista vazia é o estado normal
        // da tela de confirmação enquanto o argumento de navegação não chegou.
        if (ids.isEmpty()) return flowOf(emptyList())
        return queries.listEntriesByIds(ids).asFlow().mapToList(io)
            .map { linhas -> linhas.map(::toDomain) }
    }

    override fun observeReviewQueue(scope: Scope): Flow<List<Entry>> =
        observeReady(scope).map { prontas ->
            val instant = now()
            prontas
                .filter { it.needsReview(instant) }
                .sortedBy { it.retention?.pointsAt(instant) ?: 0.0 }
        }

    override fun observeReviewSummary(scope: Scope): Flow<ReviewSummary> = combine(
        observeReady(scope),
        queries.listReviewedDays().asFlow().mapToList(io),
    ) { prontas, days ->
        val instant = now()
        val today = localDayOf(instant)
        val streak = streakOf(days, today)
        val inQueue = prontas.count { it.needsReview(instant) }
        ReviewSummary(
            inQueue = inQueue,
            nextInMillis = prontas
                .mapNotNull { it.retention?.nextReviewIn(instant) }
                .minOrNull(),
            dayStreak = streak.dayStreak,
            reviewedToday = streak.reviewedToday,
            bestStreak = bestStreakOf(days),
            // O que já saiu hoje sai da própria retenção, e não de `dia_revisado`:
            // aquela tabela conta o dia inteiro, de todos os cursos juntos, e a
            // quota é do curso aberto.
            //
            // O corte de 48h antes da conversão não é otimização prematura:
            // `localDayOf` é uma consulta ao banco, e sem ele seria uma por
            // palavra a cada emissão do fluxo. Nenhuma revisão de hoje pode estar
            // fora dessa janela, então o corte não muda a resposta.
            quota = DailyQuota(
                done = prontas.count { entry ->
                    val retention = entry.retention ?: return@count false
                    retention.reviews > 0 &&
                        instant - retention.lastInteraction < TWO_DAYS_IN_MILLIS &&
                        localDayOf(retention.lastInteraction) == today
                },
                inQueue = inQueue,
            ),
        )
    }

    override fun observeRetention(id: Long): Flow<RetentionNow?> = observeById(id).map { entry ->
        val retention = entry?.retention ?: return@map null
        val instant = now()
        RetentionNow(
            points = retention.pointsAt(instant),
            level = retention.levelAt(instant),
            nextInMillis = retention.nextReviewIn(instant),
            reviews = retention.reviews,
            hits = retention.hits,
            misses = retention.misses,
        )
    }

    override fun observeActivity(days: Int): Flow<List<DailyActivity>> {
        val firstDay = localDayOf(now()) - (days.coerceAtLeast(1) - 1)
        return queries.listActivitySince(firstDay) { day, reviews ->
            DailyActivity(day = day, reviews = reviews.toInt())
        }.asFlow().mapToList(io)
    }

    override fun observeEvents(days: Int, scope: Scope): Flow<List<Event>> {
        val firstDay = localDayOf(now()) - (days.coerceAtLeast(1) - 1)
        return queries.listEventsSince(firstDay) { id, entryId, day, instant, type, detail, target, native, alvoIdioma ->
            Event(
                id = id,
                entryId = entryId,
                day = day,
                instant = instant,
                type = EventType.of(type),
                target = target,
                languagePair = LanguagePair(native = native, target = alvoIdioma),
                detail = detail,
            )
        }.asFlow().mapToList(io).no(scope) { it.languagePair }
    }

    override fun observeAiUsage(): Flow<AiUsage> {
        val month = localMonthOf(now())
        return queries.observeAiUsageOfMonth(month).asFlow().mapToOneOrNull(io).map { geracoes ->
            AiUsage(month = month, used = geracoes?.toInt() ?: 0)
        }
    }

    override suspend fun exportData(): ExportData = withContext(io) {
        val month = localMonthOf(now())
        queries.transactionWithResult {
            ExportData(
                captures = queries.listAllCaptures().executeAsList().map(::captureToDomain),
                entries = queries.listAllEntries().executeAsList().map(::toDomain),
                activity = queries.listActivitySince(Long.MIN_VALUE) { day, reviews ->
                    DailyActivity(day, reviews.toInt())
                }.executeAsList(),
                aiUsage = AiUsage(
                    month = month,
                    used = queries.observeAiUsageOfMonth(month).executeAsOneOrNull()?.toInt() ?: 0,
                ),
            )
        }
    }

    override suspend fun captureText(
        snippet: String,
        alvos: List<SelectedTarget>,
        languagePair: LanguagePair?,
    ): List<Long> = withContext(io) {
        val text = snippet
        require(text.isNotBlank()) { "O snippet é obrigatório." }
        require(alvos.isNotEmpty()) { "Selecione ao menos um target." }
        require(alvos.all { it.isValidIn(text) }) { "Há uma seleção fora do snippet current." }

        val course = courseOfCapture(languagePair)
        queries.transactionWithResult {
            val captureId = insertCapture(
                snippet = text,
                status = CaptureStatus.PROCESSED,
                format = CaptureFormat.TEXT,
                course = course,
            )
            insertTargets(captureId, alvos)
        }
    }

    override suspend fun captureSnippet(snippet: String, languagePair: LanguagePair?): Long = withContext(io) {
        require(snippet.isNotBlank()) { "O snippet é obrigatório." }
        val course = courseOfCapture(languagePair)
        queries.transactionWithResult {
            insertCapture(
                snippet = snippet,
                status = CaptureStatus.AWAITING_SELECTION,
                format = CaptureFormat.TEXT,
                course = course,
            )
        }
    }

    override suspend fun captureMedia(
        format: CaptureFormat,
        path: String,
        durationMs: Long?,
        languagePair: LanguagePair?,
    ): Long = withContext(io) {
        require(format != CaptureFormat.TEXT) { "Míday precisa ser foto ou áudio." }
        val course = courseOfCapture(languagePair)
        queries.transactionWithResult {
            insertCapture(
                snippet = null,
                status = CaptureStatus.TRANSCRIBING,
                format = format,
                course = course,
                path = path,
                durationMs = durationMs,
            )
        }
    }

    override suspend fun changeCaptureLanguage(id: Long, target: String): Unit = withContext(io) {
        queries.changeCaptureLanguage(target_language = target, id = id)
    }

    private fun insertCapture(
        snippet: String?,
        status: CaptureStatus,
        format: CaptureFormat,
        course: LanguagePair,
        path: String? = null,
        durationMs: Long? = null,
    ): Long {
        queries.insertCapture(
            snippet = snippet,
            source = null,
            created_at = now(),
            status = status.name,
            format = format.name,
            media_path = path,
            duration_ms = durationMs,
            transcription_error = null,
            native_language = course.native,
            target_language = course.target,
        )
        return queries.lastInsertedId().executeAsOne()
    }

    /**
     * Em que par esta captura nasce — o escolhido na folha, ou o curso aberto.
     *
     * Desde que o idioma passou a ser decidido no ato da gravação, o curso aberto
     * virou só o palpite inicial: capturar em espanhol estando na página do
     * inglês é um caso normal, e o que chega aqui é a decisão já tomada.
     */
    private suspend fun courseOfCapture(languagePair: LanguagePair?): LanguagePair = languagePair ?: activeCourse.first()

    override suspend fun recordTranscription(id: Long, snippet: String?, error: String?) {
        withContext(io) {
            queries.recordTranscription(
                snippet = snippet?.trim()?.ifBlank { null },
                transcription_error = error?.trim()?.ifBlank { null },
                id = id,
            )
        }
    }

    override suspend fun confirmCapture(
        id: Long,
        snippet: String,
        alvos: List<SelectedTarget>,
    ): List<Long> = withContext(io) {
        val text = snippet
        require(text.isNotBlank()) { "O snippet é obrigatório." }
        require(alvos.isNotEmpty()) { "Selecione ao menos um target." }
        require(alvos.all { it.isValidIn(text) }) { "Há uma seleção fora do snippet current." }

        queries.transactionWithResult {
            val existentes = queries.listIdsOfCapture(id).executeAsList()
            if (existentes.isNotEmpty()) return@transactionWithResult existentes
            queries.processCapture(snippet = text, id = id)
            insertTargets(id, alvos)
        }
    }

    private fun insertTargets(captureId: Long, alvos: List<SelectedTarget>): List<Long> =
        alvos.distinctBy { it.start to it.end }.map { target ->
            queries.insertEntry(
                capture_id = captureId,
                target = target.text.trim(),
                start_index = target.start.toLong(),
                end_index = target.end.toLong(),
                type = target.type.name,
                status = EntryStatus.PENDING.name,
            )
            val id = queries.lastInsertedId().executeAsOne()
            anotar(id, EventType.CAPTURED)
            id
        }

    /**
     * Uma linha na linha do tempo. Sempre dentro da transação de quem chamou —
     * um evento sem o fato que ele descreve seria pior que evento nenhum.
     */
    private fun anotar(entryId: Long, type: EventType, detail: String? = null) {
        val instant = now()
        queries.recordEvent(
            entry_id = entryId,
            day = localDayOf(instant),
            occurred_at = instant,
            type = type.name,
            detail = detail,
        )
    }

    override suspend fun generateCard(id: Long): Boolean = withContext(io) {
        val row = queries.findEntryById(id).executeAsOneOrNull() ?: return@withContext false
        val snippet = row.snippet?.takeIf { it.isNotBlank() } ?: return@withContext false
        val target = row.target.takeIf { it.isNotBlank() } ?: return@withContext false
        val type = typeOf(row.type)
        val wasAlreadyReady = EntryStatus.of(row.status) == EntryStatus.READY

        queries.markStatus(status = EntryStatus.GENERATING.name, id = id)
        try {
            val card = api.gerar(
                snippet = snippet,
                target = target,
                type = type,
                languagePair = LanguagePair(native = row.native_language, target = row.target_language),
            )
            queries.transaction {
                queries.saveCard(
                    status = EntryStatus.READY.name,
                    type = type.name,
                    translation = card.translation,
                    definitions_json = json.encodeToString(card.definitions),
                    example = card.example,
                    pronunciation = card.pronunciation,
                    related_json = json.encodeToString(card.related),
                    id = id,
                )
                if (!wasAlreadyReady) {
                    val initial = Retention.initial(now())
                    queries.saveRetention(
                        points = initial.points,
                        decay_rate = initial.decayRate,
                        last_interaction_at = initial.lastInteraction,
                        reviews = initial.reviews.toLong(),
                        correct_count = initial.hits.toLong(),
                        incorrect_count = initial.misses.toLong(),
                        id = id,
                    )
                    // Só na primeira vez: regerar uma ficha que já existia não é
                    // um acontecimento do dia, é conserto.
                    anotar(id, EventType.CARD_READY)
                }
                val month = localMonthOf(now())
                queries.openAiMonth(month)
                queries.addAiGeneration(month)
            }
            true
        } catch (cancelamento: CancellationException) {
            queries.markStatus(status = EntryStatus.PENDING.name, id = id)
            throw cancelamento
        } catch (falha: Exception) {
            // Anything that is not a CardException came from this side, not from
            // the server, so it has no code of its own to record.
            val error = falha as? CardException
            queries.markError(
                status = EntryStatus.ERROR.name,
                error_code = (error?.code ?: ErrorCode.GENERATION_FAILED).name,
                error_detail = error?.detail ?: falha.message,
                id = id,
            )
            false
        }
    }

    override suspend fun generateCards(ids: List<Long>, concorrencia: Int): List<Boolean> =
        coroutineScope {
            val limit = Semaphore(concorrencia.coerceAtLeast(1))
            ids.map { id -> async { limit.withPermit { generateCard(id) } } }.awaitAll()
        }

    override suspend fun recordAnswer(id: Long, correct: Boolean) = withContext(io) {
        val row = queries.findEntryById(id).executeAsOneOrNull() ?: return@withContext
        val instant = now()
        val previous = buildRetention(row)
        val nova = previous.after(correct = correct, now = instant)
        val day = localDayOf(instant)

        queries.transaction {
            queries.saveRetention(
                points = nova.points,
                decay_rate = nova.decayRate,
                last_interaction_at = nova.lastInteraction,
                reviews = nova.reviews.toLong(),
                correct_count = nova.hits.toLong(),
                incorrect_count = nova.misses.toLong(),
                id = id,
            )
            queries.openDay(day)
            queries.addReview(day)

            anotar(
                entryId = id,
                type = if (correct) EventType.CORRECT else EventType.INCORRECT,
                detail = nova.reviews.toString(),
            )
            // A mudança de nível é o que a linha do tempo chama de "virou
            // dominada", e ela só existe comparando antes e depois — depois de
            // gravado, o antes some.
            val subiu = Steps.level(Steps.of(nova))
            if (subiu != Steps.level(Steps.of(previous))) {
                anotar(entryId = id, type = EventType.LEVELED_UP, detail = subiu.name)
            }
        }
    }

    override suspend fun excluir(id: Long) = withContext(io) {
        val path = queries.transactionWithResult {
            val row = queries.findEntryById(id).executeAsOneOrNull()
                ?: return@transactionWithResult null
            queries.deleteEntry(id)
            if (queries.countEntriesOfCapture(row.capture_id).executeAsOne() == 0L) {
                queries.deleteCapture(row.capture_id)
                row.media_path
            } else {
                null
            }
        }
        path?.let(removeFile)
        Unit
    }

    override suspend fun deleteCapture(id: Long) = withContext(io) {
        val path = queries.findCaptureById(id).executeAsOneOrNull()?.media_path
        queries.transaction {
            queries.deleteEntriesOfCapture(id)
            queries.deleteCapture(id)
        }
        path?.let(removeFile)
        Unit
    }

    private fun localDayOf(instant: Long): Long = queries.localDay(instant).executeAsOne()

    private fun localMonthOf(instant: Long): String = queries.localMonth(instant).executeAsOne()

    private fun captureToDomain(row: CaptureRow) = Capture(
        id = row.id,
        snippet = row.snippet,
        source = row.source,
        createdAt = row.created_at,
        status = CaptureStatus.of(row.status),
        format = CaptureFormat.of(row.format),
        mediaPath = row.media_path,
        durationMs = row.duration_ms,
        transcriptionError = row.transcription_error,
        languagePair = LanguagePair(native = row.native_language, target = row.target_language),
    )

    private fun toDomain(row: EntryRow): Entry {
        val status = EntryStatus.of(row.status)
        val type = typeOf(row.type)
        return Entry(
            id = row.id,
            captureId = row.capture_id,
            snippet = row.snippet,
            target = row.target,
            start = row.start_index?.toInt(),
            end = row.end_index?.toInt(),
            type = type,
            source = row.source,
            createdAt = row.created_at,
            status = status,
            format = CaptureFormat.of(row.format),
            mediaPath = row.media_path,
            card = if (status == EntryStatus.READY) buildCard(row, type) else null,
            retention = if (status == EntryStatus.READY) buildRetention(row) else null,
            errorCode = row.error_code?.let(ErrorCode::of),
            errorDetail = row.error_detail,
            languagePair = LanguagePair(native = row.native_language, target = row.target_language),
        )
    }

    private fun buildRetention(row: EntryRow) = Retention(
        points = row.points,
        decayRate = row.decay_rate,
        lastInteraction = row.last_interaction_at,
        reviews = row.reviews.toInt(),
        hits = row.correct_count.toInt(),
        misses = row.incorrect_count.toInt(),
    )

    private fun buildCard(row: EntryRow, type: TargetType) = CardResponse(
        type = type,
        translation = row.translation.orEmpty(),
        definitions = row.definitions_json.listaJson(),
        example = row.example.orEmpty(),
        pronunciation = row.pronunciation.orEmpty(),
        related = row.related_json.listaJson(),
    )

    private fun String?.listaJson(): List<String> = this
        ?.let { runCatching { json.decodeFromString<List<String>>(it) }.getOrNull() }
        ?: emptyList()

    private fun typeOf(value: String): TargetType =
        runCatching { TargetType.valueOf(value) }.getOrDefault(TargetType.WORD)

    private companion object {
        const val TWO_DAYS_IN_MILLIS = 2 * 86_400_000L
    }
}
