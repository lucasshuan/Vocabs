package io.github.lucasshuan.vocabu.ui.pending

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
import io.github.lucasshuan.vocabu.ui.components.AppIcons
import io.github.lucasshuan.vocabu.ui.components.Motion
import io.github.lucasshuan.vocabu.ui.components.cardOutline
import io.github.lucasshuan.vocabu.ui.components.rememberHaptics

/**
 * The other half of the delete gesture: dragging is fast, and fast makes
 * mistakes. Without it a distracted swipe erases an audio recorded in the street
 * and the file with it, unasked.
 *
 * Names what disappeared — "1 item deleted" would force an undo out of caution
 * just to find out what it was.
 */
@Composable
fun UndoStrip(
    deletion: PendingDeletion?,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Kept while the card descends, or the strip leaves empty on the first frame.
    var last by remember { mutableStateOf<PendingDeletion?>(null) }
    LaunchedEffect(deletion) { if (deletion != null) last = deletion }

    val remaining = remember { Animatable(1f) }
    LaunchedEffect(deletion?.key) {
        if (deletion == null) return@LaunchedEffect
        remaining.snapTo(1f)
        // The bar only draws time passing; the ViewModel erases at the end of the
        // same window. Two clocks would drift apart.
        remaining.animateTo(0f, tween(VISIBLE_WINDOW_MS, easing = LinearEasing))
    }

    AnimatedVisibility(
        visible = deletion != null,
        enter = slideInVertically(Motion.standardSpring()) { it / 2 } +
            fadeIn(tween(Motion.DEFAULT)) +
            scaleIn(Motion.standardSpring(), initialScale = 0.94f),
        exit = fadeOut(tween(Motion.FAST)) + slideOutVertically(tween(Motion.FAST)) { it / 3 },
        modifier = modifier,
    ) {
        last?.let { content ->
            CardSurface(deletion = content, remaining = { remaining.value }, onUndo = onUndo)
        }
    }
}

/** The ViewModel's window; whoever draws the clock does not decide the time. */
private const val VISIBLE_WINDOW_MS = 5_000

@Composable
private fun CardSurface(
    deletion: PendingDeletion,
    remaining: () -> Float,
    onUndo: () -> Unit,
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
                modifier = Modifier.padding(horizontal = 15.dp, vertical = 14.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(colors.error),
                ) {
                    Icon(AppIcons.Trash, null, tint = colors.onError, modifier = Modifier.size(19.dp))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = stringResource(if (deletion.isCapture) R.string.undo_deleted_capture else R.string.undo_deleted_card),
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                    )
                    Text(
                        text = deletion.title,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                val touch = rememberHaptics()
                Surface(
                    onClick = onUndo,
                    shape = CircleShape,
                    color = colors.primary,
                    interactionSource = touch,
                ) {
                    Text(
                        text = stringResource(R.string.undo),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onPrimary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                    )
                }
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(colors.outlineVariant)
                    .drawBehind {
                        drawRect(
                            color = colors.error.copy(alpha = 0.55f),
                            size = size.copy(width = size.width * remaining().coerceIn(0f, 1f)),
                        )
                    },
            )
        }
    }
}
