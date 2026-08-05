package com.jean.vocabs.ui.components

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
 * The discard threshold, as a fraction of the card's width.
 *
 * A third is what separates "nudged it while scrolling" from "decided to throw
 * this away". Below it the card returns on its own; above it, releasing deletes.
 */
private const val THRESHOLD_FRACTION = 0.32f

/**
 * The threshold's floor, for narrow cards. The fraction alone would let a small
 * card be discarded by a two-centimeter drag — too close to an accidental swipe.
 */
private val MIN_THRESHOLD = 96.dp

/**
 * The fling: past this, speed decides instead of distance. Still requires half
 * the threshold travelled, so a fast diagonal graze deletes nothing.
 */
private const val DISMISS_VELOCITY = 1_100f
private const val MIN_FLING_FRACTION = 0.5f

/**
 * How much the card weighs once past the threshold.
 *
 * It does not lock — locking makes the gesture feel broken — but it moves less
 * than the finger. That is the physical signal that the decision is made and all
 * that is left is to let go.
 */
private const val RESISTANCE_PAST_THRESHOLD = 0.42f

/**
 * Drag the card sideways to delete what is on it.
 *
 * The gesture is destructive, and is the only one in the app built entirely
 * around **warning first**. Three things happen in order:
 *
 * 1. The red appears **behind** the card from the first millimeter, with the
 *    trash icon, so the gesture explains itself before any decision is made.
 * 2. Past [THRESHOLD_FRACTION] the background goes from soft red to full, the
 *    trash grows, "Release to delete" enters and the device buzzes. The moment
 *    the gesture stops being reversible by the finger alone is announced by
 *    color, text and touch at once.
 * 3. Backing out is always possible **without releasing**: dragging back disarms
 *    everything, and releasing before the threshold springs the card home.
 *
 * Both directions work on purpose. Fixing one direction would leave half the
 * people never discovering the gesture, and no second action is competing for the
 * other side — if one ever is, that is the time to split it.
 *
 * What happens **after** [onDelete] is not this file's problem: the card flies
 * off and the caller decides whether that is an immediate deletion or one that
 * can still be undone. In Pending it is the second.
 */
@Composable
fun SwipeToDelete(
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
    armedLabel: String = "Solte para excluir",
    actionLabel: String = "Excluir",
    content: @Composable () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val thresholdFloor = with(LocalDensity.current) { MIN_THRESHOLD.toPx() }

    val offset = remember { Animatable(0f) }
    var width by remember { mutableIntStateOf(0) }
    // Once the card has left the screen the gesture is over: no new touch brings
    // it back, and `onDelete` must not be called twice.
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
        label = "fundoDoDescarte",
    )
    val tinta by animateColorAsState(
        targetValue = if (armed) colors.onError else colors.error,
        animationSpec = tween(Motion.FAST),
        label = "tintaDoDescarte",
    )
    val trashScale by animateFloatAsState(
        targetValue = if (armed) 1.15f else 0.92f,
        animationSpec = Motion.elasticSpring(),
        label = "escalaDaLixeira",
    )
    val labelOpacity by animateFloatAsState(
        targetValue = if (armed) 1f else 0f,
        animationSpec = tween(Motion.FAST),
        label = "opacidadeDoRotulo",
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
                        // Leaves past the edge before the list closes the gap:
                        // you see where the card went, and only then do the ones
                        // below rise. Both at once is a flicker in which nothing
                        // is legible.
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
        // The background is decoration for the gesture: it does not exist for the
        // screen reader, which gets the deletion as an action on the card itself.
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
                // the trash under the card and the start of the gesture would
                // show nothing.
                if (toLeft) DiscardLabel(armedLabel, tinta, labelOpacity)
                Icon(
                    imageVector = AppIcons.Trash,
                    contentDescription = null,
                    tint = tinta,
                    modifier = Modifier
                        .graphicsLayer { scaleX = trashScale; scaleY = trashScale }
                        .size(24.dp),
                )
                if (!toLeft) DiscardLabel(armedLabel, tinta, labelOpacity)
            }
        }

        Box(
            modifier = Modifier
                // Read inside the lambda: moving the card costs one layout pass
                // per frame and no recomposition.
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
