package io.github.lucasshuan.vocabu.ui.components

import android.content.res.Resources
import androidx.annotation.StringRes
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
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lucasshuan.vocabu.R
import io.github.lucasshuan.vocabu.contracts.TargetType
import io.github.lucasshuan.vocabu.shared.domain.Capture
import io.github.lucasshuan.vocabu.shared.domain.CaptureFormat
import io.github.lucasshuan.vocabu.shared.domain.Entry
import io.github.lucasshuan.vocabu.shared.domain.EntryStatus
import io.github.lucasshuan.vocabu.shared.domain.MemoryLevel
import io.github.lucasshuan.vocabu.shared.domain.Retention
import io.github.lucasshuan.vocabu.ui.theme.LocalDarkTheme

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
            text = stringResource(
                if (type == TargetType.WORD) R.string.type_word else R.string.type_phrase
            ),
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
                    text = stringResource(R.string.duplicate_title),
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

@Composable
private fun duplicateDetail(entry: Entry): String = buildString {
    append(entryTitle(entry))
    append(" · ")
    append(stringResource(duplicateStatusRes(entry.status)))
    append(" · ")
    append(relativeTime(entry.createdAt))
    entry.source?.takeIf { it.isNotBlank() }?.let { source ->
        append(" · ")
        append(source)
    }
}

@StringRes
private fun duplicateStatusRes(status: EntryStatus): Int = when (status) {
    EntryStatus.READY -> R.string.duplicate_status_ready
    EntryStatus.GENERATING -> R.string.duplicate_status_generating
    EntryStatus.PENDING -> R.string.duplicate_status_pending
    EntryStatus.ERROR -> R.string.duplicate_status_error
}

/**
 * "now", "5m ago", "2h ago", "yesterday", "3d ago".
 *
 * Written against `Resources` rather than `Context` throughout this file:
 * `LocalContext.current` does not invalidate when the configuration changes, so
 * a composable reading through it keeps the previous language's text after the
 * in-app picker switches. `LocalResources.current` does invalidate.
 */
fun Resources.relativeTime(instant: Long, now: Long = System.currentTimeMillis()): String {
    val minutes = ((now - instant) / 60_000L).coerceAtLeast(0)
    return when {
        minutes < 1 -> getString(R.string.time_now)
        minutes < 60 -> getString(R.string.time_minutes_ago, minutes)
        minutes < 60 * 24 -> getString(R.string.time_hours_ago, minutes / 60)
        minutes < 60 * 48 -> getString(R.string.time_yesterday)
        else -> getString(R.string.time_days_ago, minutes / (60 * 24))
    }
}

@Composable
fun relativeTime(instant: Long, now: Long = System.currentTimeMillis()): String =
    LocalResources.current.relativeTime(instant, now)

/**
 * The forward-looking mirror of [relativeTime]: "now", "in 4h", "tomorrow".
 *
 * Takes a **duration**, not an instant, on purpose. [relativeTime] has a
 * `System.currentTimeMillis()` default — a UI-layer clock that ignores the time
 * source injected into the repository. Repeating that here would let the card's
 * bar and the home card disagree about what time it is.
 */
fun Resources.timeUntil(millis: Long): String {
    val minutes = (millis / 60_000L).coerceAtLeast(0)
    val days = (minutes / (60 * 24)).toInt()
    return when {
        minutes < 1 -> getString(R.string.time_now)
        minutes < 60 -> getString(R.string.time_in_minutes, minutes)
        minutes < 60 * 24 -> getString(R.string.time_in_hours, minutes / 60)
        minutes < 60 * 48 -> getString(R.string.time_tomorrow)
        else -> getQuantityString(R.plurals.time_in_days, days, days)
    }
}

@Composable
fun timeUntil(millis: Long): String = LocalResources.current.timeUntil(millis)

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
fun Resources.captureTitle(capture: Capture): String {
    val source = capture.source?.takeIf { it.isNotBlank() }
    return when (capture.format) {
        CaptureFormat.AUDIO -> capture.durationMs
            ?.let { getString(R.string.capture_audio_with_duration, formatDurationMs(it)) }
            ?: getString(R.string.capture_audio)
        CaptureFormat.PHOTO -> source
            ?.let { getString(R.string.capture_photo_from, it) }
            ?: getString(R.string.capture_photo)
        CaptureFormat.TEXT -> capture.snippet?.takeIf { it.isNotBlank() }
            ?.let { getString(R.string.capture_text_quoted, summarize(it)) }
            ?: source?.let { getString(R.string.capture_text_from, it) }
            ?: getString(R.string.capture_text)
    }
}

@Composable
fun captureTitle(capture: Capture): String = LocalResources.current.captureTitle(capture)

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

@StringRes
fun levelLabelRes(level: MemoryLevel): Int = when (level) {
    MemoryLevel.NEW -> R.string.level_new
    MemoryLevel.LEARNING -> R.string.level_learning
    MemoryLevel.FAMILIAR -> R.string.level_familiar
    MemoryLevel.MASTERED -> R.string.level_mastered
}

@Composable
fun levelLabel(level: MemoryLevel): String = stringResource(levelLabelRes(level))

fun Resources.levelLabel(level: MemoryLevel): String = getString(levelLabelRes(level))

/**
 * "review now" once past the threshold, otherwise "in 2d 4h".
 *
 * Null when there is no retention: with no card ready there is no scheduled
 * review, and "review now" would send someone to an empty queue.
 */
@Composable
fun nextReviewText(retention: Retention?, now: Long): String? {
    val missing = retention?.nextReviewIn(now) ?: return null
    return if (missing <= 0L) stringResource(R.string.review_now) else timeUntil(missing)
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
        label = "memoryFraction",
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

/**
 * What to name an entry before a target has been typed.
 *
 * This was a property on `Entry`, in `:shared`. It lives here because nothing
 * below `:androidApp` should produce display text: a domain module has no
 * resources, so a title decided there can only ever exist in one language.
 */
@Composable
fun entryTitle(entry: Entry): String =
    entry.target?.takeIf { it.isNotBlank() } ?: stringResource(untitledResOf(entry.format))

/** The same, for the view models that have to name an entry outside composition. */
fun Resources.entryTitle(entry: Entry): String =
    entry.target?.takeIf { it.isNotBlank() } ?: getString(untitledResOf(entry.format))

@StringRes
private fun untitledResOf(format: CaptureFormat): Int = when (format) {
    CaptureFormat.PHOTO -> R.string.entry_untitled_photo
    CaptureFormat.AUDIO -> R.string.entry_untitled_audio
    CaptureFormat.TEXT -> R.string.entry_untitled_text
}
