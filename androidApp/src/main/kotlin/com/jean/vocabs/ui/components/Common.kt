package com.jean.vocabs.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jean.vocabs.contracts.TargetType
import com.jean.vocabs.shared.domain.Capture
import com.jean.vocabs.shared.domain.CaptureFormat
import com.jean.vocabs.shared.domain.Entry
import com.jean.vocabs.shared.domain.EntryStatus
import com.jean.vocabs.shared.domain.MemoryLevel
import com.jean.vocabs.shared.domain.Retention
import com.jean.vocabs.ui.theme.LocalDarkTheme

/**
 * The "word" / "phrase" badge.
 *
 * Phrase is the one that gets the plum tone: two or more tokens is the less
 * frequent case and the one that should stand out in a list. Lower case on
 * purpose — it is a classification, not a title.
 */
@Composable
fun TypeBadge(type: TargetType, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val dark = LocalDarkTheme.current
    val (background, text) = when (type) {
        TargetType.PHRASE -> colors.secondaryContainer to if (dark) colors.onSurface else colors.primary
        TargetType.WORD -> colors.surfaceVariant to colors.onSurfaceVariant
    }
    Surface(shape = CircleShape, color = background, modifier = modifier) {
        Text(
            text = if (type == TargetType.WORD) "palavra" else "expressão",
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
            color = text,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
        )
    }
}

@Composable
fun DuplicateNotice(entry: Entry, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = colors.secondaryContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(14.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(34.dp)
                    .background(colors.secondary, CircleShape),
            ) {
                Icon(
                    imageVector = AppIcons.Repeat,
                    contentDescription = null,
                    tint = colors.onSecondary,
                    modifier = Modifier.size(18.dp),
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
            ) {
                Text(
                    text = "Você já tem isso",
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.onSecondaryContainer,
                )
                Text(
                    text = duplicateDetail(entry),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSecondaryContainer.copy(alpha = 0.76f),
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

private fun duplicateDetail(entry: Entry): String = buildString {
    append(entry.title)
    append(" · ")
    append(duplicateStatusLabel(entry.status))
    append(" · ")
    append(relativeTime(entry.createdAt))
    entry.source?.takeIf { it.isNotBlank() }?.let { source ->
        append(" · ")
        append(source)
    }
}

private fun duplicateStatusLabel(status: EntryStatus): String = when (status) {
    EntryStatus.READY -> "ficha pronta"
    EntryStatus.GENERATING -> "gerando ficha"
    EntryStatus.PENDING -> "na fila"
    EntryStatus.ERROR -> "com erro"
}

/** "now", "5m ago", "2h ago", "yesterday", "3d ago". */
fun relativeTime(entao: Long, now: Long = System.currentTimeMillis()): String {
    val minutes = ((now - entao) / 60_000L).coerceAtLeast(0)
    return when {
        minutes < 1 -> "agora"
        minutes < 60 -> "há ${minutes}min"
        minutes < 60 * 24 -> "há ${minutes / 60}h"
        minutes < 60 * 48 -> "ontem"
        else -> "há ${minutes / (60 * 24)}d"
    }
}

/**
 * The forward-looking mirror of [relativeTime]: "now", "in 4h", "tomorrow".
 *
 * Takes a **duration**, not an instant, on purpose. [relativeTime] has a
 * `System.currentTimeMillis()` default — a UI-layer clock that ignores the time
 * source injected into the repository. Repeating that here would let the card's
 * bar and the home card disagree about what time it is.
 */
fun timeUntil(millis: Long): String {
    val minutes = (millis / 60_000L).coerceAtLeast(0)
    return when {
        minutes < 1 -> "agora"
        minutes < 60 -> "em ${minutes}min"
        minutes < 60 * 24 -> "em ${minutes / 60}h"
        minutes < 60 * 48 -> "amanhã"
        else -> "em ${minutes / (60 * 24)} dias"
    }
}

/** "0:12", "1:03:20" — an audio duration, from millis. */
fun formatDurationMs(durationMs: Long): String {
    val totalSeconds = (durationMs / 1_000).coerceAtLeast(0)
    val hours = totalSeconds / 3_600
    val minutes = (totalSeconds % 3_600) / 60
    val seconds = totalSeconds % 60
    val paddedSeconds = seconds.toString().padStart(2, '0')
    return if (hours > 0) "$hours:${minutes.toString().padStart(2, '0')}:$paddedSeconds" else "$minutes:$paddedSeconds"
}

/** The same duration from seconds — what the recording timer counts. */
fun formatDuration(seconds: Long): String = formatDurationMs(seconds * 1_000L)

/**
 * What to call a raw capture: "Audio · 0:12", "Photo from Kindle", "“tant pis”".
 *
 * Pending's list and the selection header need the same name — tapping a row has
 * to lead to a recognizable screen. It is also the only place `source` appears,
 * which is what makes it worth filling in.
 *
 * Pasted text shows its own snippet in quotes rather than the word "Text": in a
 * queue of five captures, "Text" three times distinguishes nothing.
 */
fun captureTitle(capture: Capture): String {
    val source = capture.source?.takeIf { it.isNotBlank() }
    return when (capture.format) {
        CaptureFormat.AUDIO -> capture.durationMs?.let { "Áudio · ${formatDurationMs(it)}" } ?: "Áudio"
        CaptureFormat.PHOTO -> source?.let { "Foto do $it" } ?: "Foto"
        CaptureFormat.TEXT -> capture.snippet?.takeIf { it.isNotBlank() }?.let { "“${summarize(it)}”" }
            ?: source?.let { "Texto do $it" }
            ?: "Texto"
    }
}

/** One line of snippet for a title, cut at the word rather than the letter. */
fun summarize(text: String, limit: Int = 38): String {
    val clean = text.trim().replace(Regex("\\s+"), " ")
    if (clean.length <= limit) return clean
    val cut = clean.take(limit).substringBeforeLast(' ', clean.take(limit))
    return "$cut…"
}

/** Plum identifies the brand and actions; mint is reserved for progress. */
@Composable
fun levelColor(level: MemoryLevel): Color = when (level) {
    MemoryLevel.NEW -> MaterialTheme.colorScheme.onSurfaceVariant
    MemoryLevel.LEARNING -> MaterialTheme.colorScheme.primary
    MemoryLevel.FAMILIAR -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.72f)
    MemoryLevel.MASTERED -> MaterialTheme.colorScheme.tertiary
}

/**
 * The level label only earns mint at "mastered".
 *
 * At the other levels it is supporting information: the bar beside it carries the
 * state, and two colored elements saying the same thing would make "familiar"
 * look like an achievement.
 */
@Composable
fun levelLabelColor(level: MemoryLevel): Color =
    if (level == MemoryLevel.MASTERED) MaterialTheme.colorScheme.tertiary
    else MaterialTheme.colorScheme.onSurfaceVariant

fun levelLabel(level: MemoryLevel): String = when (level) {
    MemoryLevel.NEW -> "nova"
    MemoryLevel.LEARNING -> "aprendendo"
    MemoryLevel.FAMILIAR -> "familiar"
    MemoryLevel.MASTERED -> "dominada"
}

/**
 * "review now" once past the threshold, otherwise "in 2d 4h".
 *
 * Null when there is no retention: with no card ready there is no scheduled
 * review, and "review now" would send someone to an empty queue.
 */
fun nextReviewText(retention: Retention?, now: Long): String? {
    val missing = retention?.nextReviewIn(now) ?: return null
    return if (missing <= 0L) "revisar agora" else timeUntil(missing)
}

/**
 * Memory strength as a bar.
 *
 * Usable short (Words reserves 84 dp beside the label) or full width (the card),
 * so it forces no width of its own — the caller decides with the modifier.
 */
@Composable
fun MemoryBar(
    points: Double,
    level: MemoryLevel,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp,
) {
    val fraction by animateFloatAsState(
        targetValue = (points / 100.0).toFloat().coerceIn(0f, 1f),
        animationSpec = tween(500),
        label = "fracaoMemoria",
    )

    Box(
        modifier = modifier
            .height(height)
            .background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(height / 2)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .fillMaxHeight()
                .background(levelColor(level), RoundedCornerShape(height / 2)),
        )
    }
}
