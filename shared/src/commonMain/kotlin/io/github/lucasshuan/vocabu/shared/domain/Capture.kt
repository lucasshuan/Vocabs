package io.github.lucasshuan.vocabu.shared.domain

import io.github.lucasshuan.vocabu.contracts.TargetType

/** The raw context one or several cards can be born from. */
data class Capture(
    val id: Long,
    val snippet: String?,
    val source: String?,
    val createdAt: Long,
    val status: CaptureStatus,
    val format: CaptureFormat,
    val mediaPath: String?,
    val durationMs: Long?,
    val transcriptionError: String?,
/** The course it was born in. A snippet is in one language only. */
    val languagePair: LanguagePair = LanguagePair.DEFAULT,
) {
    val awaitingSelection: Boolean get() = status == CaptureStatus.AWAITING_SELECTION
    val transcribing: Boolean get() = status == CaptureStatus.TRANSCRIBING
}

enum class CaptureStatus {
    TRANSCRIBING,
    AWAITING_SELECTION,
    PROCESSED;

    companion object {
        fun of(value: String): CaptureStatus =
            entries.firstOrNull { it.name == value } ?: AWAITING_SELECTION
    }
}

/** A confirmed range inside the snippet. Ranges may overlap. */
data class SelectedTarget(
    val text: String,
    val start: Int,
    val end: Int,
    val type: TargetType,
)

data class DailyActivity(
    val day: Long,
    val reviews: Int,
)

data class AiUsage(
    val month: String,
    val used: Int,
    val limit: Int = MONTHLY_AI_LIMIT,
) {
    val fraction: Float get() = (used.toFloat() / limit.coerceAtLeast(1)).coerceIn(0f, 1f)

    companion object {
        const val MONTHLY_AI_LIMIT = 100
    }
}

data class ExportData(
    val captures: List<Capture>,
    val entries: List<Entry>,
    val activity: List<DailyActivity>,
    val aiUsage: AiUsage,
)
