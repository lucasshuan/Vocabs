package io.github.lucasshuan.vocabu.ui.components

import androidx.compose.ui.res.stringResource
import io.github.lucasshuan.vocabu.R
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlinx.coroutines.launch

/** Separates "nudged it while scrolling" from "decided to throw this away". */
private const val THRESHOLD_FRACTION = 0.32f

/** The fraction alone lets a small card go on a two-centimetre drag. */
private val MIN_THRESHOLD = 96.dp

/**
 * Past this speed decides instead of distance, still over half the threshold —
 * a fast diagonal graze deletes nothing.
 */
private const val DISMISS_VELOCITY = 1_100f
private const val MIN_FLING_FRACTION = 0.5f

/**
 * Moving less than the finger is the physical signal the decision is made.
 * Locking outright would make the gesture feel broken.
 */
private const val RESISTANCE_PAST_THRESHOLD = 0.42f

/**
 * The only destructive gesture in the app, and it warns first: red from the
 * first millimetre, then colour, text and a buzz at once past
 * [THRESHOLD_FRACTION]. Dragging back disarms all of it.
 *
 * Both directions, because no second action competes for the other side and
 * fixing one would leave half the people never finding the gesture.
 *
 * What follows [onDelete] is the caller's — in Pending it is still undoable.
 */
@Composable
fun SwipeToDelete(
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
    armedLabel: String = stringResource(R.string.swipe_release_to_delete),
    actionLabel: String = stringResource(R.string.swipe_delete),
    content: @Composable () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val thresholdFloor = with(LocalDensity.current) { MIN_THRESHOLD.toPx() }

    val offset = remember { Animatable(0f) }
    var width by remember { mutableIntStateOf(0) }
    // `onDelete` must not fire twice for one gesture.
    var discarding by remember { mutableStateOf(false) }

    val threshold = if (width == 0) thresholdFloor else maxOf(width * THRESHOLD_FRACTION, thresholdFloor)

    // `derivedStateOf`: the offset changes every frame, this twice per gesture.
    // Without it, dragging one card recomposes the row sixty times a second.
    val armed by remember(threshold) { derivedStateOf { abs(offset.value) >= threshold } }
    val toLeft by remember { derivedStateOf { offset.value <= 0f } }

    // Arming only: pulsing on disarm too makes a back-and-forth one long buzz.
    LaunchedEffect(armed) {
        if (armed) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    val background by animateColorAsState(
        targetValue = if (armed) colors.error else colors.errorContainer,
        animationSpec = tween(Motion.FAST),
        label = "discardBackground",
    )
    val tint by animateColorAsState(
        targetValue = if (armed) colors.onError else colors.error,
        animationSpec = tween(Motion.FAST),
        label = "discardTint",
    )
    val trashScale by animateFloatAsState(
        targetValue = if (armed) 1.15f else 0.92f,
        animationSpec = Motion.elasticSpring(),
        label = "trashScale",
    )
    val labelOpacity by animateFloatAsState(
        targetValue = if (armed) 1f else 0f,
        animationSpec = tween(Motion.FAST),
        label = "labelOpacity",
    )

    val drag = rememberDraggableState { step ->
        val current = offset.value
        val advancing = current == 0f || sign(step) == sign(current)
        val walked = if (advancing && abs(current) >= threshold) step * RESISTANCE_PAST_THRESHOLD else step
        scope.launch { offset.snapTo(current + walked) }
    }

    Box(
        modifier = modifier
            .onSizeChanged { width = it.width }
            .draggable(
                state = drag,
                orientation = Orientation.Horizontal,
                enabled = !discarding,
                onDragStopped = { speed ->
                    val walked = offset.value
                    val fling = abs(speed) > DISMISS_VELOCITY &&
                        sign(speed) == sign(walked) &&
                        abs(walked) >= threshold * MIN_FLING_FRACTION
                    if (abs(walked) >= threshold || fling) {
                        discarding = true
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        // Past the edge before the list closes the gap: both at
                        // once is a flicker in which nothing is legible.
                        offset.animateTo(
                            targetValue = sign(walked) * (width.toFloat() + thresholdFloor),
                            animationSpec = tween(Motion.FAST, easing = FastOutLinearInEasing),
                        )
                        onDelete()
                    } else {
                        offset.animateTo(0f, Motion.standardSpring())
                    }
                },
            ),
    ) {
        // Decoration only. The screen reader gets deletion as a card action.
        Box(
            modifier = Modifier
                .matchParentSize()
                .clearAndSetSemantics {}
                .clip(shape)
                .background(background),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .align(if (toLeft) Alignment.CenterEnd else Alignment.CenterStart)
                    .padding(horizontal = 18.dp),
            ) {
                // Trash against the uncovered edge, label growing inward.
                // Inverted, the invisible label pushes the trash under the card
                // and the gesture starts blank.
                if (toLeft) DiscardLabel(armedLabel, tint, labelOpacity)
                Icon(
                    imageVector = AppIcons.Trash,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier
                        .graphicsLayer { scaleX = trashScale; scaleY = trashScale }
                        .size(24.dp),
                )
                if (!toLeft) DiscardLabel(armedLabel, tint, labelOpacity)
            }
        }

        Box(
            modifier = Modifier
                // Read inside the lambda: layout per frame, no recomposition.
                .offset { IntOffset(offset.value.roundToInt(), 0) }
                // Screen-reader navigation cannot drag, so deletion is an action
                // on the card, worded as the gesture words it.
                .semantics(mergeDescendants = true) {
                    customActions = listOf(CustomAccessibilityAction(actionLabel) { onDelete(); true })
                },
        ) {
            content()
        }
    }
}

@Composable
private fun DiscardLabel(text: String, color: androidx.compose.ui.graphics.Color, opacity: Float) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Clip,
        modifier = Modifier.graphicsLayer { alpha = opacity },
    )
}
