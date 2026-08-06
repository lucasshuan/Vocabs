package io.github.lucasshuan.vocabu.shared.domain

import io.github.lucasshuan.vocabu.contracts.CardResponse
import io.github.lucasshuan.vocabu.contracts.ErrorCode
import io.github.lucasshuan.vocabu.contracts.TargetType

/**
 * A confirmed target inside a capture and, once ready, its card.
 *
 * [card] reuses the contract type rather than restating it, and [retention]
 * exists if and only if [card] does.
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
    /** The screen picks the wording from this. */
    val errorCode: ErrorCode?,
    /** The provider's raw sentence or the HTTP status — untranslatable. */
    val errorDetail: String?,
    /** Inherited from the capture: what this card is born in and regenerated in. */
    val languagePair: LanguagePair = LanguagePair.DEFAULT,
) {
    fun needsReview(now: Long): Boolean = retention?.needsReview(now) == true

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
 * Photo and audio exist to capture in seconds and resolve later — mid-game or
 * hands-busy, leaving the app is the cost that matters.
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
 * A capture returns immediately at PENDING; the card is generated afterwards.
 * The draft belongs to [Capture] — an entry exists only once a target is confirmed.
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
