package com.jean.vocabs.ui.capture

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
import com.jean.vocabs.shared.domain.CaptureFormat
import com.jean.vocabs.ui.components.AppIcons
import com.jean.vocabs.ui.components.CircularFlag
import com.jean.vocabs.ui.components.Motion
import com.jean.vocabs.ui.components.cardOutline
import com.jean.vocabs.ui.components.formatDurationMs
import com.jean.vocabs.ui.components.rememberHaptics
import com.jean.vocabs.ui.displayName
import com.jean.vocabs.ui.languages.languageOf

/**
 * What the bottom notice has to say.
 *
 * [key] exists so the notice is **replaceable**: captures in a row swap the
 * content and restart the countdown instead of stacking cards. Two identical
 * captures in a row have to count as different notices, and the key is what
 * guarantees that when every other field matches.
 */
sealed interface Notice {

    val key: Long

    /**
     * The capture entered the queue. [captureId] arrives a few milliseconds after
     * the rest, when the database returns the id — until then the "Select"
     * shortcut has nowhere to go and so is not drawn.
     */
    data class Saved(
        override val key: Long,
        val format: CaptureFormat,
        val durationMs: Long?,
        val target: String,
        val captureId: Long? = null,
    ) : Notice

    /** One sentence and nothing else: microphone denied, recording too short. */
    data class Message(override val key: Long, val text: String) : Notice
}

/** How long each notice lives. The message is shorter because it offers nothing. */
private const val SAVED_LIFETIME_MS = 5_000
private const val NOTICE_LIFETIME_MS = 3_500

/**
 * The passing notice.
 *
 * It returns to where the person was and moves nothing: it floats over the
 * content, disappears on its own in 5 s, and the bar at its base counts the time
 * left. Ignoring is the default exit and carries no penalty — "Select" is a
 * shortcut for whoever has time now, never the next step.
 *
 * It replaced the `Snackbar` because a flag, an audio duration, a named action
 * and a visible clock do not fit in a line of text — and the clock is the piece
 * that makes the notice ignorable without anxiety.
 */
@Composable
fun NoticeStrip(
    notice: Notice?,
    onSelect: (Long) -> Unit,
    onExpire: (key: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    // The last non-null notice stays drawn while the card leaves: without it the
    // content would vanish on the first frame of the exit and the card would
    // descend empty.
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
                            val toque = rememberHaptics()
                            Surface(
                                onClick = { onSelect(id) },
                                shape = CircleShape,
                                color = colors.primary,
                                interactionSource = toque,
                            ) {
                                Text(
                                    text = "Selecionar",
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

            // The notice's clock, drawn from the animated fraction read inside
            // `drawBehind`: five seconds of bar should not recompose the card
            // three hundred times.
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

private fun savedTitle(notice: Notice.Saved): String = when (notice.format) {
    CaptureFormat.AUDIO -> notice.durationMs?.let { "Áudio ${formatDurationMs(it)} guardado" } ?: "Áudio guardado"
    CaptureFormat.PHOTO -> "Foto guardada"
    CaptureFormat.TEXT -> "Trecho guardado"
}
