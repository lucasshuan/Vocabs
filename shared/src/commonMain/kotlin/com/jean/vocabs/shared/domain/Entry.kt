package com.jean.vocabs.shared.domain

import com.jean.vocabs.contracts.CardResponse
import com.jean.vocabs.contracts.ErrorCode
import com.jean.vocabs.contracts.TargetType

/**
 * A confirmed target inside a capture and, once ready, its card.
 *
 * [card] deliberately reuses the contract type: a third shape just for the domain
 * would be one more copy to keep in sync, for nothing.
 *
 * [retention] follows the same rule as [card]: it exists if and only if there is
 * a card to review.
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
    /** Which failure, so the screen can pick the wording. Null when there was none. */
    val errorCode: ErrorCode?,
    /** What does not translate: the provider's raw sentence, or the HTTP status. */
    val errorDetail: String?,
    /** Inherited from the capture: the pair this card is born in and regenerated in. */
    val languagePair: LanguagePair = LanguagePair.DEFAULT,
) {
    fun needsReview(now: Long): Boolean = retention?.needsReview(now) == true

    /** Which rung of the "What's left" ladder it is on. */
    val step: Int get() = Steps.of(retention)

}

private val targetSpaces = Regex("\\s+")

fun duplicateOfTarget(
    target: String,
    entries: Iterable<Entry>,
    ignoreId: Long? = null,
): Entry? {
    val wanted = normalizeTarget(target)
    if (wanted.isBlank()) return null

    return entries
        .asSequence()
        .filter { entry -> entry.id != ignoreId }
        .filter { entry -> normalizeTarget(entry.target) == wanted }
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
 * How the signal entered the app.
 *
 * Each context has a different constraint — gaming, you do not want to leave the
 * game; reading, your hands are busy. Photo and audio exist to capture in seconds
 * and resolve later, not to be processed on the spot.
 */
enum class CaptureFormat {
    TEXT,
    PHOTO,
    AUDIO;

    companion object {
        fun of(value: String?): CaptureFormat =
            entries.firstOrNull { it.name == value } ?: TEXT
    }
}

/**
 * A capture records and returns immediately (PENDING); generating the card
 * happens afterwards, in the background.
 *
 * The draft belongs to [Capture]; an entry only exists once a target has been
 * confirmed.
 */
enum class EntryStatus {
    PENDING,
    GENERATING,
    READY,
    ERROR;

    companion object {
        fun of(value: String): EntryStatus =
            entries.firstOrNull { it.name == value } ?: PENDING
    }
}
