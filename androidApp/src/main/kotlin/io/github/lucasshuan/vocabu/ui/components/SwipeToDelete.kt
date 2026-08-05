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

/**
 * A third is what separates "nudged it while scrolling" from "decided to throw
 * this away".
 */
private const val THRESHOLD_FRACTION = 0.32f

/**
 * The fraction alone would let a small card be discarded by a two-centimeter
 * drag — too close to an accidental swipe.
 */
private val MIN_THRESHOLD = 96.dp

/**
 * Past this, speed decides instead of distance. Still requires half the threshold
 * travelled, so a fast diagonal graze deletes nothing.
 */
private const val DISMISS_VELOCITY = 1_100f
private const val MIN_FLING_FRACTION = 0.5f

/**
 * It does not lock — locking makes the gesture feel broken — but it moves less
 * than the finger, which is the physical signal that the decision is made.
 */
private const val RESISTANCE_PAST_THRESHOLD = 0.42f

/**
 * Drag the card sideways to delete what is on it.
 *
 * The only destructive gesture in the app, and it is built around **warning
 * first**: the red is behind the card from the first millimeter, and past
 * [THRESHOLD_FRACTION] the background fills, the label enters and the device
 * buzzes — the moment it stops being reversible by the finger alone is announced
 * by color, text and touch at once. Dragging back disarms everything.
 *
 * Both directions work on purpose: fixing one would leave half the people never
 * finding the gesture, and no second action competes for the other side.
 *
 * What happens **after** [onDelete] is the caller's: in Pending it is a deletion
 * that can still be undone.
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
    // Once the card has left the screen the gesture is over: `onDelete` must not
    // be called twice.
    var discarding by remember { mutableStateOf(false) }

    val threshold = if (width == 0) thresholdFloor else maxOf(width * THRESHOLD_FRACTION, thresholdFloor)

    // `derivedStateOf` because the offset changes every frame and this changes
    // twice per gesture: without it, dragging one card would recompose the whole
    // row sixty times a second.
    val armed by remember(threshold) { derivedStateOf { abs(offset.value) >= threshold } }
    val toLeft by remember { derivedStateOf { offset.value <= 0f } }

    // On arming only. A second pulse on disarming would turn a back-and-forth
    // into one continuous vibration.
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
                        // Leaves past the edge before the list closes the gap.
                        // Both at once is a flicker in which nothing is legible.
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
        // Decoration for the gesture: it does not exist for the screen reader,
        // which gets the deletion as an action on the card itself.
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
                // The trash stays against the edge the card uncovered and the
                // label grows inward. Inverted, the invisible label would push
                // the trash under the card and the gesture would start blank.
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
                // Read inside the lambda: one layout pass per frame and no
                // recomposition.
                .offset { IntOffset(offset.value.roundToInt(), 0) }
                // Screen-reader navigation cannot drag, so deletion becomes an
                // action on the card, with the same wording the gesture shows.
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
