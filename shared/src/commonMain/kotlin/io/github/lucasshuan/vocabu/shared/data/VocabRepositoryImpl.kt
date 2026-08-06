package io.github.lucasshuan.vocabu.shared.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import io.github.lucasshuan.vocabu.contracts.CardResponse
import io.github.lucasshuan.vocabu.contracts.ErrorCode
import io.github.lucasshuan.vocabu.contracts.TargetType
import io.github.lucasshuan.vocabu.shared.data.remote.CardApi
import io.github.lucasshuan.vocabu.shared.data.remote.CardException
import io.github.lucasshuan.vocabu.shared.db.Capture as CaptureRow
import io.github.lucasshuan.vocabu.shared.db.Entry_with_capture as EntryRow
import io.github.lucasshuan.vocabu.shared.db.VocabsDatabase
import io.github.lucasshuan.vocabu.shared.domain.AiUsage
import io.github.lucasshuan.vocabu.shared.domain.Capture
import io.github.lucasshuan.vocabu.shared.domain.CaptureFormat
import io.github.lucasshuan.vocabu.shared.domain.CaptureStatus
import io.github.lucasshuan.vocabu.shared.domain.CourseSummary
import io.github.lucasshuan.vocabu.shared.domain.DailyActivity
import io.github.lucasshuan.vocabu.shared.domain.DailyQuota
import io.github.lucasshuan.vocabu.shared.domain.Entry
import io.github.lucasshuan.vocabu.shared.domain.EntryStatus
import io.github.lucasshuan.vocabu.shared.domain.Event
import io.github.lucasshuan.vocabu.shared.domain.EventType
import io.github.lucasshuan.vocabu.shared.domain.ExportData
import io.github.lucasshuan.vocabu.shared.domain.LanguagePair
import io.github.lucasshuan.vocabu.shared.domain.MemoryLevel
import io.github.lucasshuan.vocabu.shared.domain.Retention
import io.github.lucasshuan.vocabu.shared.domain.RetentionNow
import io.github.lucasshuan.vocabu.shared.domain.ReviewSummary
import io.github.lucasshuan.vocabu.shared.domain.Scope
import io.github.lucasshuan.vocabu.shared.domain.SelectedTarget
import io.github.lucasshuan.vocabu.shared.domain.Steps
import io.github.lucasshuan.vocabu.shared.domain.VocabRepository
import io.github.lucasshuan.vocabu.shared.domain.bestStreakOf
import io.github.lucasshuan.vocabu.shared.domain.isValidIn
import io.github.lucasshuan.vocabu.shared.domain.streakOf
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

/**
 * The `require` messages are literals on purpose: no screen renders them, they
 * describe a programming error. Text a person reads lives in `:androidApp`.
 */
class VocabRepositoryImpl(
    private val db: VocabsDatabase,
    private val api: CardApi,
    private val io: CoroutineDispatcher,
    private val now: () -> Long,
    /**
     * A flow, not a getter: every screen rebuilds itself when the strip switches
     * course, instead of each ViewModel resubscribing.
     */
    private val activeCourse: Flow<LanguagePair> = flowOf(LanguagePair.DEFAULT),
    private val removeFile: (String) -> Unit = {},
) : VocabRepository {

    private val json = Json { ignoreUnknownKeys = true }
    private val queries get() = db.vocabsQueries

    override fun observeActiveCourse(): Flow<LanguagePair> = activeCourse

    override fun observeCourses(): Flow<List<CourseSummary>> =
        allReady().map { readyEntries ->
            val instant = now()
            readyEntries
                // By target, not by pair: see CourseSummary.target.
                .groupBy { it.languagePair.target }
                .map { (target, entries) ->
                    CourseSummary(
                        target = target,
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
        queries.listReady().asFlow().mapToList(io).map { lines -> lines.map(::toDomain) }

    override fun observeReady(scope: Scope): Flow<List<Entry>> =
        allReady().no(scope) { it.languagePair }

    override fun observeInbox(scope: Scope): Flow<List<Entry>> =
        queries.listInbox().asFlow().mapToList(io).map { lines -> lines.map(::toDomain) }
            .no(scope) { it.languagePair }

    override fun observePendingCaptures(scope: Scope): Flow<List<Capture>> =
        queries.listPendingCaptures().asFlow().mapToList(io)
            .map { lines -> lines.map(::captureToDomain) }
            .no(scope) { it.languagePair }

    /**
     * In memory, not in SQL: the active course is a preference flow, and a query
     * parameterised by it reopens the cursor on every carousel swipe. A device's
     * card list fits in memory; the reopened cursor does not fit the frame budget.
     */
    private fun <T> Flow<List<T>>.no(scope: Scope, languagePair: (T) -> LanguagePair): Flow<List<T>> =
        combine(fitsScope(scope)) { itens, cabe -> itens.filter { cabe(languagePair(it)) } }

    private fun fitsScope(scope: Scope): Flow<(LanguagePair) -> Boolean> = when (scope) {
        Scope.All -> flowOf({ _: LanguagePair -> true })
        is Scope.Course -> flowOf({ languagePair: LanguagePair -> languagePair.target == scope.target })
        // Reads filter by target; writes still record the whole pair, so a card
        // regenerated later comes back in the language it was born in.
        Scope.ActiveCourse -> activeCourse.map { activePair ->
            { languagePair: LanguagePair -> languagePair.target == activePair.target }
        }
    }

    override fun observeCaptureById(id: Long): Flow<Capture?> =
        queries.findCaptureById(id).asFlow().mapToOneOrNull(io)
            .map { it?.let(::captureToDomain) }

    override fun observeById(id: Long): Flow<Entry?> =
        queries.findEntryById(id).asFlow().mapToOneOrNull(io).map { it?.let(::toDomain) }

    override fun observeEntries(ids: List<Long>): Flow<List<Entry>> {
        // `IN ()` is not valid SQLite, and empty is the confirmation screen's
        // normal state before the navigation argument arrives.
        if (ids.isEmpty()) return flowOf(emptyList())
        return queries.listEntriesByIds(ids).asFlow().mapToList(io)
            .map { lines -> lines.map(::toDomain) }
    }

    override fun observeReviewQueue(scope: Scope): Flow<List<Entry>> =
        observeReady(scope).map { readyEntries ->
            val instant = now()
            readyEntries
                .filter { it.needsReview(instant) }
                .sortedBy { it.retention?.pointsAt(instant) ?: 0.0 }
        }

    override fun observeReviewSummary(scope: Scope): Flow<ReviewSummary> = combine(
        observeReady(scope),
        queries.listReviewedDays().asFlow().mapToList(io),
    ) { readyEntries, days ->
        val instant = now()
        val today = localDayOf(instant)
        val streak = streakOf(days, today)
        val inQueue = readyEntries.count { it.needsReview(instant) }
        ReviewSummary(
            inQueue = inQueue,
            nextInMillis = readyEntries
                .mapNotNull { it.retention?.nextReviewIn(instant) }
                .minOrNull(),
            dayStreak = streak.dayStreak,
            reviewedToday = streak.reviewedToday,
            bestStreak = bestStreakOf(days),
            // From retention, not `reviewed_day`: that table counts every course,
            // and the quota belongs to the active one.
            //
            // The 48h cut comes first because `localDayOf` is a database query —
            // otherwise one per word, per emission. Nothing from today falls
            // outside that window, so the cut does not change the answer.
            quota = DailyQuota(
                done = readyEntries.count { entry ->
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
        return queries.listEventsSince(firstDay) { id, entryId, day, instant, type, detail, target, native, languageTarget ->
            Event(
                id = id,
                entryId = entryId,
                day = day,
                instant = instant,
                type = EventType.of(type),
                target = target,
                languagePair = LanguagePair(native = native, target = languageTarget),
                detail = detail,
            )
        }.asFlow().mapToList(io).no(scope) { it.languagePair }
    }

    override fun observeAiUsage(): Flow<AiUsage> {
        val month = localMonthOf(now())
        return queries.observeAiUsageOfMonth(month).asFlow().mapToOneOrNull(io).map { generations ->
            AiUsage(month = month, used = generations?.toInt() ?: 0)
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
        targets: List<SelectedTarget>,
        languagePair: LanguagePair?,
    ): List<Long> = withContext(io) {
        val text = snippet
        require(text.isNotBlank()) { "snippet must not be blank" }
        require(targets.isNotEmpty()) { "at least one target must be selected" }
        require(targets.all { it.isValidIn(text) }) { "a selection falls outside the snippet" }

        val course = courseOfCapture(languagePair)
        queries.transactionWithResult {
            val captureId = insertCapture(
                snippet = text,
                status = CaptureStatus.PROCESSED,
                format = CaptureFormat.TEXT,
                course = course,
            )
            insertTargets(captureId, targets)
        }
    }

    override suspend fun captureSnippet(snippet: String, languagePair: LanguagePair?): Long = withContext(io) {
        require(snippet.isNotBlank()) { "snippet must not be blank" }
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
        require(format != CaptureFormat.TEXT) { "media must be a photo or audio" }
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
     * The active course is only the opening guess: capturing in Spanish from the
     * English page is normal, and the sheet's choice arrives here already made.
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
        targets: List<SelectedTarget>,
    ): List<Long> = withContext(io) {
        val text = snippet
        require(text.isNotBlank()) { "snippet must not be blank" }
        require(targets.isNotEmpty()) { "at least one target must be selected" }
        require(targets.all { it.isValidIn(text) }) { "a selection falls outside the snippet" }

        queries.transactionWithResult {
            val existing = queries.listIdsOfCapture(id).executeAsList()
            if (existing.isNotEmpty()) return@transactionWithResult existing
            queries.processCapture(snippet = text, id = id)
            insertTargets(id, targets)
        }
    }

    private fun insertTargets(captureId: Long, targets: List<SelectedTarget>): List<Long> =
        targets.distinctBy { it.start to it.end }.map { target ->
            queries.insertEntry(
                capture_id = captureId,
                target = target.text.trim(),
                start_index = target.start.toLong(),
                end_index = target.end.toLong(),
                type = target.type.name,
                status = EntryStatus.PENDING.name,
            )
            val id = queries.lastInsertedId().executeAsOne()
            note(id, EventType.CAPTURED)
            id
        }

    /**
     * One row on the timeline. Always inside the caller's transaction — an event
     * without the fact it describes would be worse than no event.
     */
    private fun note(entryId: Long, type: EventType, detail: String? = null) {
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
            val card = api.generate(
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
                    // First time only: regenerating an existing card is a repair,
                    // not something that happened today.
                    note(id, EventType.CARD_READY)
                }
                val month = localMonthOf(now())
                queries.openAiMonth(month)
                queries.addAiGeneration(month)
            }
            true
        } catch (cancellation: CancellationException) {
            queries.markStatus(status = EntryStatus.PENDING.name, id = id)
            throw cancellation
        } catch (failure: Exception) {
            // Anything but a CardException came from this side, so it carries no
            // code of its own.
            val error = failure as? CardException
            queries.markError(
                status = EntryStatus.ERROR.name,
                error_code = (error?.code ?: ErrorCode.GENERATION_FAILED).name,
                error_detail = error?.detail ?: failure.message,
                id = id,
            )
            false
        }
    }

    override suspend fun generateCards(ids: List<Long>, concurrency: Int): List<Boolean> =
        coroutineScope {
            val limit = Semaphore(concurrency.coerceAtLeast(1))
            ids.map { id -> async { limit.withPermit { generateCard(id) } } }.awaitAll()
        }

    override suspend fun recordAnswer(id: Long, correct: Boolean) = withContext(io) {
        val row = queries.findEntryById(id).executeAsOneOrNull() ?: return@withContext
        val instant = now()
        val previous = buildRetention(row)
        val fresh = previous.after(correct = correct, now = instant)
        val day = localDayOf(instant)

        queries.transaction {
            queries.saveRetention(
                points = fresh.points,
                decay_rate = fresh.decayRate,
                last_interaction_at = fresh.lastInteraction,
                reviews = fresh.reviews.toLong(),
                correct_count = fresh.hits.toLong(),
                incorrect_count = fresh.misses.toLong(),
                id = id,
            )
            queries.openDay(day)
            queries.addReview(day)

            note(
                entryId = id,
                type = if (correct) EventType.CORRECT else EventType.INCORRECT,
                detail = fresh.reviews.toString(),
            )
            // "Became mastered" only exists by comparing before and after, and
            // once written the before is gone.
            val rose = Steps.level(Steps.of(fresh))
            if (rose != Steps.level(Steps.of(previous))) {
                note(entryId = id, type = EventType.LEVELED_UP, detail = rose.name)
            }
        }
    }

    override suspend fun delete(id: Long) = withContext(io) {
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
        definitions = row.definitions_json.jsonList(),
        example = row.example.orEmpty(),
        pronunciation = row.pronunciation.orEmpty(),
        related = row.related_json.jsonList(),
    )

    private fun String?.jsonList(): List<String> = this
        ?.let { runCatching { json.decodeFromString<List<String>>(it) }.getOrNull() }
        ?: emptyList()

    private fun typeOf(value: String): TargetType =
        runCatching { TargetType.valueOf(value) }.getOrDefault(TargetType.WORD)

    private companion object {
        const val TWO_DAYS_IN_MILLIS = 2 * 86_400_000L
    }
}
