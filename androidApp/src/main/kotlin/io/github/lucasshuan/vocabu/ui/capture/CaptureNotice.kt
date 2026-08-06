package io.github.lucasshuan.vocabu.ui.capture

import androidx.compose.ui.res.stringResource
import io.github.lucasshuan.vocabu.R
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.lucasshuan.vocabu.shared.domain.CaptureFormat
import io.github.lucasshuan.vocabu.ui.components.AppIcons
import io.github.lucasshuan.vocabu.ui.components.CircularFlag
import io.github.lucasshuan.vocabu.ui.components.Motion
import io.github.lucasshuan.vocabu.ui.components.cardOutline
import io.github.lucasshuan.vocabu.ui.components.formatDurationMs
import io.github.lucasshuan.vocabu.ui.components.rememberHaptics
import io.github.lucasshuan.vocabu.ui.languages.displayName
import io.github.lucasshuan.vocabu.ui.languages.languageOf

/**
 * [key] makes the notice replaceable: captures in a row swap the content and
 * restart the countdown instead of stacking. It is the only thing separating two
 * identical captures in a row.
 */
sealed interface Notice {

    val key: Long

    /**
     * [captureId] lands a few millis after the rest, when the database returns
     * it. Until then "Select" has nowhere to go and is not drawn.
     */
    data class Saved(
        override val key: Long,
        val format: CaptureFormat,
        val durationMs: Long?,
        val target: String,
        val captureId: Long? = null,
    ) : Notice

    /** Microphone denied, recording too short — one sentence, no action. */
    data class Message(override val key: Long, val text: String) : Notice
}

/** The message is shorter because it offers nothing to act on. */
private const val SAVED_LIFETIME_MS = 5_000
private const val NOTICE_LIFETIME_MS = 3_500

/**
 * Floats over the content and moves nothing. Ignoring is the default exit —
 * "Select" is a shortcut for whoever has time now, never the next step.
 *
 * Not a `Snackbar`: a flag, a duration, a named action and a visible clock do
 * not fit in a line of text, and the clock is what makes it ignorable.
 */
@Composable
fun NoticeStrip(
    notice: Notice?,
    onSelect: (Long) -> Unit,
    onExpire: (key: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Kept while the card leaves, or the content vanishes on the exit's first
    // frame and the card descends empty.
    var last by remember { mutableStateOf<Notice?>(null) }
    LaunchedEffect(notice) { if (notice != null) last = notice }

    val remaining = remember { Animatable(1f) }
    LaunchedEffect(notice?.key) {
        val current = notice ?: return@LaunchedEffect
        val life = if (current is Notice.Message) NOTICE_LIFETIME_MS else SAVED_LIFETIME_MS
        remaining.snapTo(1f)
        remaining.animateTo(0f, tween(life, easing = LinearEasing))
        onExpire(current.key)
    }

    AnimatedVisibility(
        visible = notice != null,
        enter = slideInVertically(Motion.standardSpring()) { it / 2 } +
            fadeIn(tween(Motion.DEFAULT)) +
            scaleIn(Motion.standardSpring(), initialScale = 0.94f),
        exit = fadeOut(tween(Motion.FAST)) + slideOutVertically(tween(Motion.FAST)) { it / 3 },
        modifier = modifier,
    ) {
        last?.let { content ->
            NoticeCard(
                notice = content,
                remaining = { remaining.value },
                onSelect = onSelect,
            )
        }
    }
}

@Composable
private fun NoticeCard(
    notice: Notice,
    remaining: () -> Float,
    onSelect: (Long) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.surface,
        border = cardOutline(),
        shadowElevation = 10.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(start = 15.dp, end = 15.dp, top = 14.dp, bottom = 14.dp),
            ) {
                when (notice) {
                    is Notice.Saved -> {
                        Disc(colors.tertiary, AppIcons.Check)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                text = savedTitle(notice),
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                val language = languageOf(notice.target)
                                CircularFlag(language, size = 15.dp)
                                Text(
                                    text = language.displayName.lowercase(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.onSurfaceVariant,
                                )
                            }
                        }
                        notice.captureId?.let { id ->
                            val touch = rememberHaptics()
                            Surface(
                                onClick = { onSelect(id) },
                                shape = CircleShape,
                                color = colors.primary,
                                interactionSource = touch,
                            ) {
                                Text(
                                    text = stringResource(R.string.capture_select),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colors.onPrimary,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                                )
                            }
                        }
                    }

                    is Notice.Message -> {
                        Disc(colors.secondary, AppIcons.Clock)
                        Text(
                            text = notice.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            // The fraction is read inside `drawBehind`: five seconds of bar
            // should not recompose the card three hundred times.
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(colors.outlineVariant)
                    .drawBehind {
                        drawRect(
                            color = colors.onSurfaceVariant.copy(alpha = 0.45f),
                            size = size.copy(width = size.width * remaining().coerceIn(0f, 1f)),
                        )
                    },
            )
        }
    }
}

@Composable
private fun Disc(color: androidx.compose.ui.graphics.Color, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(36.dp).clip(CircleShape).background(color),
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.surface, modifier = Modifier.size(19.dp))
    }
}

@Composable
private fun savedTitle(notice: Notice.Saved): String = when (notice.format) {
    CaptureFormat.AUDIO -> notice.durationMs
        ?.let { stringResource(R.string.capture_saved_audio_with_duration, formatDurationMs(it)) }
        ?: stringResource(R.string.capture_saved_audio)
    CaptureFormat.PHOTO -> stringResource(R.string.capture_saved_photo)
    CaptureFormat.TEXT -> stringResource(R.string.capture_saved_text)
}
