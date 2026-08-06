package io.github.lucasshuan.vocabu.shared.domain

import kotlinx.coroutines.flow.Flow

/** Resolved against one instant, so the pieces cannot disagree. */
data class ReviewSummary(
    val inQueue: Int,
    /** Millis until the next word is due. Null when there is no card at all. */
    val nextInMillis: Long?,
    val dayStreak: Int,
    val reviewedToday: Boolean,
    val bestStreak: Int = 0,
    val quota: DailyQuota = DailyQuota(done = 0, inQueue = inQueue),
)

/** [Retention] resolved against a clock the UI does not own. */
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
 * Every sliceable read defaults to [Scope.ActiveCourse], so a screen that forgot
 * to choose lands on the right answer almost everywhere. Words, Pending and
 * Profile pass [Scope.All] on purpose.
 */
interface VocabRepository {

    fun observeActiveCourse(): Flow<LanguagePair>

    /** Always across every course, whatever the active one. */
    fun observeCourses(): Flow<List<CourseSummary>>

    fun observeReady(scope: Scope = Scope.ActiveCourse): Flow<List<Entry>>

    /** PENDING, GENERATING or ERROR — everything not yet a card. */
    fun observeInbox(scope: Scope = Scope.ActiveCourse): Flow<List<Entry>>

    fun observePendingCaptures(scope: Scope = Scope.ActiveCourse): Flow<List<Capture>>

    fun observeCaptureById(id: Long): Flow<Capture?>

    fun observeById(id: Long): Flow<Entry?>

    fun observeEntries(ids: List<Long>): Flow<List<Entry>>

    /** Cards whose points fell below [Retention.REVIEW_THRESHOLD]. */
    fun observeReviewQueue(scope: Scope = Scope.ActiveCourse): Flow<List<Entry>>

    fun observeReviewSummary(scope: Scope = Scope.ActiveCourse): Flow<ReviewSummary>

    fun observeRetention(id: Long): Flow<RetentionNow?>

    fun observeActivity(days: Int = 84): Flow<List<DailyActivity>>

    /** Most recent first. */
    fun observeEvents(days: Int = 84, scope: Scope = Scope.ActiveCourse): Flow<List<Event>>

    fun observeAiUsage(): Flow<AiUsage>

    /** One snapshot — the export ZIP must not mix two moments. */
    suspend fun exportData(): ExportData

    /** Capture and every selected entry in one transaction. */
    suspend fun captureText(
        snippet: String,
        targets: List<SelectedTarget>,
        languagePair: LanguagePair? = null,
    ): List<Long>

    /**
     * Saved before any selection exists, so abandoning the sheet leaves the
     * capture in Pending instead of losing what was pasted.
     */
    suspend fun captureSnippet(snippet: String, languagePair: LanguagePair? = null): Long

    /** The file is durable before OCR or speech starts; failure falls back to typing. */
    suspend fun captureMedia(
        format: CaptureFormat,
        path: String,
        durationMs: Long? = null,
        languagePair: LanguagePair? = null,
    ): Long

    /**
     * Only before the selection: afterwards there are cards born in that pair,
     * and moving it underneath them orphans them from their own language.
     */
    suspend fun changeCaptureLanguage(id: Long, target: String)

    /** An error here does not block manual editing. */
    suspend fun recordTranscription(id: Long, snippet: String?, error: String? = null)

    suspend fun confirmCapture(
        id: Long,
        snippet: String,
        targets: List<SelectedTarget>,
    ): List<Long>

    suspend fun generateCard(id: Long): Boolean

    /** Bounded concurrency: the phone is on mobile data as often as not. */
    suspend fun generateCards(ids: List<Long>, concurrency: Int = 2): List<Boolean>

    /** Also marks the day on the review calendar, which the streak reads. */
    suspend fun recordAnswer(id: Long, correct: Boolean)

    /** Takes the media file with it, when no sibling entry still points at it. */
    suspend fun delete(id: Long)

    suspend fun deleteCapture(id: Long)
}
