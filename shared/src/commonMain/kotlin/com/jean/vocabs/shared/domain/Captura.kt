package com.jean.vocabs.shared.domain

import com.jean.vocabs.contracts.TargetType

/** O contexto bruto do qual podem nascer uma ou várias fichas. */
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
    /** O course em que ela nasceu. Um snippet está numa língua só. */
    val languagePair: LanguagePair = LanguagePair.PADRAO,
) {
    val awaitingSelection: Boolean get() = status == CaptureStatus.AWAITING_SELECTION
    val transcrevendo: Boolean get() = status == CaptureStatus.TRANSCRIBING
}

enum class CaptureStatus {
    TRANSCRIBING,
    AWAITING_SELECTION,
    PROCESSED;

    companion object {
        fun de(value: String): CaptureStatus =
            entries.firstOrNull { it.name == value } ?: AWAITING_SELECTION
    }
}

/** Um intervalo confirmado dentro do snippet. Intervalos podem se sobrepor. */
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
    val limit: Int = LIMITE_MENSAL_IA,
) {
    val fracao: Float get() = (used.toFloat() / limit.coerceAtLeast(1)).coerceIn(0f, 1f)

    companion object {
        const val LIMITE_MENSAL_IA = 100
    }
}

data class ExportData(
    val captures: List<Capture>,
    val entries: List<Entry>,
    val activity: List<DailyActivity>,
    val aiUsage: AiUsage,
)
