package com.jean.vocabs.shared.domain

import kotlinx.coroutines.flow.Flow

/** What the home review card needs, already resolved for the current instant. */
data class ReviewSummary(
    val inQueue: Int,
    /** Millis until the next word is due. Null when there is no card at all. */
    val nextInMillis: Long?,
    val dayStreak: Int,
    val reviewedToday: Boolean,
    val bestStreak: Int = 0,
    val quota: DailyQuota = DailyQuota(done = 0, inQueue = inQueue),
)

/** The card's bar, already resolved — it depends on the clock, which lives here. */
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
 * The active course is the default, not the only possible answer.
 *
 * Every sliceable read takes a [Scope] starting at [Scope.ActiveCourse]: a screen
 * that forgot to choose would show the course the person is in, which is right
 * almost everywhere. Words, Pending and Profile ask for [Scope.All] on purpose;
 * "Your progress" asks for a named course, which may not be the active one.
 */
interface VocabRepository {

    /** Which course is active now — the default of [Scope.ActiveCourse]. */
    fun observeActiveCourse(): Flow<LanguagePair>

    /** One summary per course, badge already resolved. Always across all of them. */
    fun observeCourses(): Flow<List<CourseSummary>>

    /** Only what has become a card. */
    fun observeReady(scope: Scope = Scope.ActiveCourse): Flow<List<Entry>>

    /** Entries still in the AI queue, being generated, or failed. */
    fun observeInbox(scope: Scope = Scope.ActiveCourse): Flow<List<Entry>>

    /** Captures still awaiting transcription or a confirmed selection. */
    fun observePendingCaptures(scope: Scope = Scope.ActiveCourse): Flow<List<Capture>>

    fun observeCaptureById(id: Long): Flow<Capture?>

    fun observeById(id: Long): Flow<Entry?>

    /** The entries of a handful of ids — what the "Saved" screen follows. */
    fun observeEntries(ids: List<Long>): Flow<List<Entry>>

    /** The current queue: cards whose memory strength fell below the threshold. */
    fun observeReviewQueue(scope: Scope = Scope.ActiveCourse): Flow<List<Entry>>

    fun observeReviewSummary(scope: Scope = Scope.ActiveCourse): Flow<ReviewSummary>

    fun observeRetention(id: Long): Flow<RetentionNow?>

    fun observeActivity(days: Int = 84): Flow<List<DailyActivity>>

    /** The Day-by-day timeline, most recent first. */
    fun observeEvents(days: Int = 84, scope: Scope = Scope.ActiveCourse): Flow<List<Event>>

    fun observeAiUsage(): Flow<AiUsage>

    /** A consistent snapshot, used by the local portability ZIP. */
    suspend fun exportData(): ExportData

    /** Creates a text capture and every selected card in one transaction. */
    suspend fun captureText(
        snippet: String,
        targets: List<SelectedTarget>,
        languagePair: LanguagePair? = null,
    ): List<Long>

    /**
     * Saves the pasted snippet before there is any selection.
     *
     * This is what makes the sheet's "Continue" cheap: from here on, closing the
     * app or abandoning the selection leaves the capture in Pending, in the
     * language already chosen, instead of losing what was pasted.
     */
    suspend fun captureSnippet(snippet: String, languagePair: LanguagePair? = null): Long

    /**
     * Saves a photo or audio as a capture in transcription. The file is safe
     * before OCR or speech starts, and can always go on to manual editing.
     */
    suspend fun captureMedia(
        format: CaptureFormat,
        path: String,
        durationMs: Long? = null,
        languagePair: LanguagePair? = null,
    ): Long

    /**
     * Changes a not-yet-processed capture's target language.
     *
     * Only meaningful before the selection: after it there are cards born in that
     * pair, and changing it underneath them would orphan them from their own
     * language.
     */
    suspend fun changeCaptureLanguage(id: Long, target: String)

    /** Finishes the automatic attempt; an error does not block manual editing. */
    suspend fun recordTranscription(id: Long, snippet: String?, error: String? = null)

    /** Confirms the edited text and creates one entry per selection. */
    suspend fun confirmCapture(
        id: Long,
        snippet: String,
        targets: List<SelectedTarget>,
    ): List<Long>

    /** Calls the server and records the result (or the error) on the entry. */
    suspend fun generateCard(id: Long): Boolean

    /** Processes independent entries with at most two requests in flight. */
    suspend fun generateCards(ids: List<Long>, concurrency: Int = 2): List<Boolean>

    /** Records a card's result and marks the day on the review calendar. */
    suspend fun recordAnswer(id: Long, correct: Boolean)

    /** Discards the entry and its media file, if any. */
    suspend fun delete(id: Long)

    suspend fun deleteCapture(id: Long)
}
