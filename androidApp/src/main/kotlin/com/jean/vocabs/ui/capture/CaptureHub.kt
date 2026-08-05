package com.jean.vocabs.ui.capture

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.jean.vocabs.contracts.Language
import com.jean.vocabs.shared.domain.CaptureFormat
import com.jean.vocabs.ui.components.BAR_HEIGHT
import com.jean.vocabs.ui.components.AppIcons
import com.jean.vocabs.ui.components.Motion
import com.jean.vocabs.ui.components.formatColors
import com.jean.vocabs.ui.components.formatIcon
import com.jean.vocabs.ui.components.formatLabel
import com.jean.vocabs.ui.components.reducedMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

/**
 * The `+` and everything that comes out of it.
 *
 * Dragging from the `+` replaces two taps with one gesture: holding opens a fan
 * of three targets, the finger reaches one, releasing runs it.
 *
 * **Releasing is the only confirmation.** While the finger moves nothing has
 * started — no recording, no camera, no text field. A target that fired on being
 * touched would turn the path to the other two into a minefield.
 *
 * The language is never asked first: a capture goes to the course open in the
 * hub, and correcting it lives in Pending, where the mistake is visible and undo
 * costs one tap.
 *
 * The hub fills the screen because the fan and the veil draw over the bar and the
 * content. Outside a gesture nothing here intercepts touch: a `Box` without
 * `pointerInput` consumes nothing, so the bottom bar stays clickable through it.
 */
@Composable
fun CaptureHub(
    capture: QuickCapture,
    language: Language,
    onOpenText: () -> Unit,
    onPhaseChange: (inGesture: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val reduced = reducedMotion()
    val openText by rememberUpdatedState(onOpenText)

    var fanOpen by remember { mutableStateOf(false) }
    var target by remember { mutableStateOf<GestureTarget>(GestureTarget.Origin) }
    val isRecording = capture.isRecording

    // Applies to the recording too: that screen is opaque, but the content stays
    // composed beneath it, and a `+` TalkBack can still reach is a trap.
    val inGesture = fanOpen || isRecording
    LaunchedEffect(inGesture) { onPhaseChange(inGesture) }

    // The hub leaves the composition when a full screen opens, which cancels the
    // gesture coroutine without going through the normal finish. Without this, a
    // recording caught by a navigation keeps the microphone open with nothing on
    // screen saying so.
    DisposableEffect(Unit) {
        onDispose {
            if (capture.isRecording) capture.cancelAudio()
            onPhaseChange(false)
        }
    }

    // Fan and recording stay composed briefly after closing so the exit can
    // animate; otherwise the targets vanish in the frame the finger lifts.
    val drawingFan = stillOnScreen(fanOpen)
    val drawingRecording = stillOnScreen(isRecording)

    // The veil leaves the content legible behind it: the person is deciding, and
    // the app is the context of the decision.
    val veil = animateFloatAsState(
        targetValue = if (fanOpen) 0.46f else 0f,
        animationSpec = tween(Motion.FAST, easing = FastOutSlowInEasing),
        label = "veil",
    )

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .drawBehind { if (veil.value > 0.001f) drawRect(NIGHT, alpha = veil.value) },
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .fillMaxWidth()
                .height(ANCHOR_HEIGHT)
                // The `+` sits under the opaque recording screen; leaving it
                // reachable would offer "Capture" mid-recording.
                .then(if (isRecording) Modifier.clearAndSetSemantics {} else Modifier)
                // The anchor is tall enough to hold the whole fan, and drops
                // until its center meets the button's. From there every target
                // is a pure `offset` from the `+`.
                .offset(y = ANCHOR_HEIGHT / 2 - BAR_HEIGHT / 2 - BUTTON_ELEVATION),
        ) {
            if (drawingFan) {
                GuideToTarget(target, fanOpen)
                CaptureFormat.entries.forEach { format ->
                    FanTarget(
                        format = format,
                        marked = target == GestureTarget.Mode(format),
                        isVisible = fanOpen,
                        delay = ENTER_DELAY.getValue(format),
                        reduced = reduced,
                    )
                }
                GestureHint(target, fanOpen)
            }

            HubButton(
                fanOpen = fanOpen,
                isRecording = isRecording,
                reduced = reduced,
                modifier = Modifier
                    .offset(y = -BUTTON_ELEVATION)
                    .semantics {
                        contentDescription = "Capturar"
                        // The drag has no screen-reader equivalent, so the fan's
                        // three exits become actions on the button itself.
                        customActions = listOf(
                            CustomAccessibilityAction("Escrever") { openText(); true },
                            CustomAccessibilityAction("Fotografar") { capture.takePhoto(); true },
                            CustomAccessibilityAction("Gravar áudio") {
                                if (capture.hasAudioPermission) capture.recordAudio()
                                else capture.requestAudioPermission()
                                true
                            },
                        )
                    }
                    .pointerInput(Unit) {
                        val targets = targetsInPixels()
                        val targetRadiusPx = TARGET_RADIUS.toPx()
                        val originRadiusPx = ORIGIN_RADIUS.toPx()
                        val slack = viewConfiguration.touchSlop

                        awaitEachGesture {
                            val first = awaitFirstDown(requireUnconsumed = false)
                            first.consume()
                            val centro = Offset(size.width / 2f, size.height / 2f)

                            // Up to 180 ms held still is a tap; past that, or
                            // once the finger has moved, the fan opens. The
                            // system's 500 ms is too long for a gesture that has
                            // to land before the sentence scrolls past.
                            val releasedEarly = withTimeoutOrNull(FAN_OPEN_MS) {
                                var loose: PointerInputChange? = null
                                while (true) {
                                    val change = nextChange(first) ?: break
                                    if (!change.pressed) {
                                        loose = change
                                        break
                                    }
                                    if ((change.position - first.position).getDistance() > slack) break
                                }
                                loose
                            }

                            if (releasedEarly != null) {
                                openText()
                                return@awaitEachGesture
                            }

                            fanOpen = true
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                            var current: GestureTarget = GestureTarget.Origin
                            var released = false

                            while (true) {
                                val change = nextChange(first) ?: break
                                val new = targetFor(
                                    shift = change.position - centro,
                                    targets = targets,
                                    targetRadiusPx = targetRadiusPx,
                                    originRadiusPx = originRadiusPx,
                                )

                                if (new != current) {
                                    current = new
                                    target = new
                                    // Arriving at a target buzzes; leaving does
                                    // not. A second pulse on exit would make
                                    // crossing the fan one long vibration.
                                    if (new is GestureTarget.Mode) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                }

                                if (!change.pressed) {
                                    released = true
                                    break
                                }
                            }

                            fanOpen = false
                            target = GestureTarget.Origin
                            finish(capture, current, released, haptic, openText)
                        }
                    },
            )
        }

        // Last in the `Box`, so above everything including the `+`, which hides
        // behind it instead of staying clickable under an opaque screen.
        if (drawingRecording) {
            RecordingScreen(
                capture = capture,
                language = language,
                isRecording = isRecording,
            )
        }
    }
}

/**
 * Whether something already dismissed still needs to be composed to finish
 * leaving. Enters on the same frame [active] turns true, and only disappears
 * once the exit animation has had time to run.
 */
@Composable
private fun stillOnScreen(active: Boolean, leftover: Long = Motion.DEFAULT.toLong() + 80): Boolean {
    var present by remember { mutableStateOf(active) }
    LaunchedEffect(active) {
        if (active) {
            present = true
        } else {
            delay(leftover)
            present = false
        }
    }
    return present || active
}

/**
 * The next state of this same finger, or null once it is gone.
 *
 * Every event is consumed: the hub sits above scrollable lists, and an upward
 * drag leaking through would scroll the screen underneath the fan.
 */
private suspend fun androidx.compose.ui.input.pointer.AwaitPointerEventScope.nextChange(
    source: PointerInputChange,
): PointerInputChange? {
    val event = awaitPointerEvent()
    val change = event.changes.firstOrNull { it.id == source.id } ?: return null
    change.consume()
    return change
}

/**
 * What releasing means — the only place in the gesture where anything happens.
 *
 * [released] separates a finger that lifted from a gesture the system cancelled:
 * another pointer, the screen going dark, the app backgrounding. Treating a
 * cancellation as a release would open the camera by itself mid-call.
 */
private fun finish(
    capture: QuickCapture,
    target: GestureTarget,
    released: Boolean,
    haptic: HapticFeedback,
    openText: () -> Unit,
) {
    if (!released) return
    when (target) {
        is GestureTarget.Mode -> {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            when (target.format) {
                CaptureFormat.TEXT -> openText()
                CaptureFormat.PHOTO -> capture.takePhoto()
                CaptureFormat.AUDIO ->
                    if (capture.hasAudioPermission) capture.recordAudio()
                    else capture.requestAudioPermission()
            }
        }
        // The fan opened on time and the finger never left the `+`. That is a
        // slow tap, not a change of mind, so it lands on the text path rather
        // than on a gesture that did nothing.
        GestureTarget.Origin -> openText()
        // Releasing outside closes without capturing and without a notice.
        GestureTarget.Outward -> Unit
    }
}

/** One color for the veil and the recording background, in both themes. */
internal val NIGHT = Color(0xFF17111F)

/** How far the button rises above the center of the bar's icon row. */
private val BUTTON_ELEVATION = 10.dp

/**
 * The `+` diameter.
 *
 * The handoff asks for 84 and bleeds the button 8 px past the bottom edge of the
 * screen, which comes from an iPhone frame. On Android that strip is the
 * home-gesture area and the system intercepts touches inside it, so the button
 * stays whole in the safe area and gains height instead of depth: it rises
 * [BUTTON_ELEVATION] and breaks the bar's **top** edge, the one Android leaves
 * free.
 */
val BUTTON_DIAMETER = 76.dp

/**
 * Tall enough for the whole fan: 260 dp of half-height against 194 dp to the top
 * of the highest target and 238 dp to the hint above it.
 */
private val ANCHOR_HEIGHT = 520.dp

/** Audio leaves first: it is the likeliest target, and the eye needs it soonest. */
private val ENTER_DELAY = mapOf(
    CaptureFormat.AUDIO to 0L,
    CaptureFormat.TEXT to 20L,
    CaptureFormat.PHOTO to 40L,
)

/**
 * The `+`, in two states: a filled disc at rest, an outline once the fan is open
 * — it stops being a button and becomes the origin of the gesture, and a filled
 * disc there would compete with the targets.
 */
@Composable
private fun HubButton(
    fanOpen: Boolean,
    isRecording: Boolean,
    reduced: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme

    val background by animateColorAsState(
        targetValue = if (fanOpen) Color.Transparent else colors.primary,
        animationSpec = tween(Motion.FAST),
        label = "fundoDoBotao",
    )
    val outline by animateColorAsState(
        targetValue = if (fanOpen) Color.White.copy(alpha = 0.38f) else Color.Transparent,
        animationSpec = tween(Motion.FAST),
        label = "contornoDoBotao",
    )
    val tinta by animateColorAsState(
        targetValue = if (fanOpen) Color.White.copy(alpha = 0.5f) else colors.onPrimary,
        animationSpec = tween(Motion.FAST),
        label = "tintaDoBotao",
    )
    val scale = animateFloatAsState(
        targetValue = if (fanOpen) 0.94f else 1f,
        animationSpec = Motion.gestureSpring(),
        label = "escalaDoBotao",
    )
    // The shadow is for the solid button. Over the veil it separates nothing and
    // only blurs the dashed outline.
    val shadow = animateFloatAsState(
        targetValue = if (fanOpen) 0f else 1f,
        animationSpec = tween(Motion.FAST),
        label = "sombraDoBotao",
    )

    // Nothing breathes while the fan is open or behind the recording screen: an
    // infinite animation under an opaque surface is battery spent on no frame.
    val atRest = !fanOpen && !isRecording && !reduced

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                shadowElevation = 10.dp.toPx() * shadow.value
                shape = CircleShape
                ambientShadowColor = colors.primary
                spotShadowColor = colors.primary
            }
            .size(BUTTON_DIAMETER)
            .halo(active = atRest, color = colors.primary)
            .hubBreath(active = atRest)
            .background(background, CircleShape)
            .dashedCircleOutline { outline },
    ) {
        Icon(
            imageVector = AppIcons.Plus,
            contentDescription = null,
            tint = tinta,
            modifier = Modifier.size(30.dp),
        )
    }
}

/**
 * A 4 s wave leaving the button and fading before it gets far. One animation, no
 * new color, no blinking: 2.8 s growing and 1.2 s still, because a continuous
 * pulse would read as an alarm.
 *
 * The animated value is read **inside** `drawBehind`, so only the draw phase is
 * invalidated each frame rather than the whole button recomposing 60 times a
 * second to draw a circle.
 */
private fun Modifier.halo(active: Boolean, color: Color): Modifier = composed {
    if (!active) return@composed this
    val transition = rememberInfiniteTransition(label = "halo")
    val advance = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 4_000
                0f at 0 using LinearOutSlowInEasing
                1f at 2_800
                1f at 4_000
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "avancoDoHalo",
    )
    drawBehind {
        val f = advance.value
        drawCircle(
            color = color,
            radius = size.minDimension / 2f * (1f + 0.55f * f),
            alpha = 0.45f * (1f - f),
        )
    }
}

/** The 4.5% breath that accompanies the halo. */
private fun Modifier.hubBreath(active: Boolean): Modifier = composed {
    if (!active) return@composed this
    val transition = rememberInfiniteTransition(label = "respiro")
    val scale = transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.045f,
        animationSpec = infiniteRepeatable(
            animation = tween(2_000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "escalaDoRespiro",
    )
    graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
    }
}

/** The dashed outline of the `+` when it is an origin rather than a button. */
private fun Modifier.dashedCircleOutline(color: () -> Color): Modifier = drawBehind {
    val tinta = color()
    if (tinta.alpha < 0.01f) return@drawBehind
    val line = 1.6.dp.toPx()
    drawCircle(
        color = tinta,
        radius = size.minDimension / 2f - line / 2f,
        style = Stroke(
            width = line,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(line * 4.5f, line * 3.5f)),
        ),
    )
}

/** How much a target grows on being reached: 68 dp to 76 dp. */
private val HIGHLIGHT_GROWTH =
    MARKED_TARGET_DIAMETER.value / TARGET_DIAMETER.value - 1f

/**
 * One of the three targets, flying out of the `+` rather than appearing in place
 * — that is what says it came from here, and that the way back undoes it.
 *
 * Position is interpolated inside `graphicsLayer`, in the draw phase: three
 * targets animating position by layout would remeasure the whole anchor every
 * frame of the gesture. The highlight comes from **one** animated value read the
 * same way, so it chases the finger instead of arriving a frame late.
 */
@Composable
private fun FanTarget(
    format: CaptureFormat,
    marked: Boolean,
    isVisible: Boolean,
    delay: Long,
    reduced: Boolean,
) {
    val palette = formatColors(format)
    val destination: DpOffset = offsetOf(format)

    val advance = remember { Animatable(0f) }
    LaunchedEffect(isVisible) {
        if (isVisible) {
            if (delay > 0 && !reduced) delay(delay)
            advance.animateTo(1f, Motion.gestureSpring())
        } else {
            advance.animateTo(0f, tween(100, easing = FastOutSlowInEasing))
        }
    }

    val emphasis = animateFloatAsState(
        targetValue = if (marked) 1f else 0f,
        animationSpec = tween(TARGET_HIGHLIGHT_MS, easing = LinearOutSlowInEasing),
        label = "realceDoAlvo",
    )
    val tinta by animateColorAsState(
        targetValue = if (marked) Color.White else palette.color,
        animationSpec = tween(TARGET_HIGHLIGHT_MS),
        label = "tintaDoAlvo",
    )

    // The label hangs **outside** the disc's box rather than sharing a column
    // with it: in a column what centers on the destination is disc plus label,
    // and the disc would draw some 13 dp above the point the finger tests. With
    // proximity selection that gap is the distance between the target you see
    // and the target that exists.
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .requiredSize(TARGET_DIAMETER)
            .graphicsLayer {
                val f = advance.value
                translationX = destination.x.toPx() * f
                translationY = destination.y.toPx() * f
                alpha = f.coerceIn(0f, 1f)
                val growth = (0.62f + 0.38f * f) * (1f + HIGHLIGHT_GROWTH * emphasis.value)
                scaleX = growth
                scaleY = growth
            }
            .drawBehind {
                val r = emphasis.value
                val radius = size.minDimension / 2f
                // The ring sits outside the disc: it confirms the choice without
                // competing for space with the icon being pointed at.
                if (r > 0.01f) {
                    drawCircle(color = palette.color, radius = radius + 7.dp.toPx() * r, alpha = 0.26f * r)
                }
                drawCircle(color = lerp(palette.background, palette.color, r), radius = radius)
            },
    ) {
        Icon(
            imageVector = formatIcon(format),
            contentDescription = null,
            tint = tinta,
            modifier = Modifier.size(26.dp),
        )
        Text(
            text = formatLabel(format),
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            fontWeight = if (marked) FontWeight.Bold else FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .requiredWidth(112.dp)
                .offset(y = TARGET_DIAMETER + 9.dp)
                .graphicsLayer { alpha = 0.78f + 0.22f * emphasis.value },
        )
    }
}

/** Length of the stub pointing up while no target is marked. */
private val GUIDE_STUB = 46.dp

/**
 * The dotted line from the `+` to the target.
 *
 * Without a visible rail, three discs floating over a veil say neither that they
 * came from the button nor that the way back undoes it. With nothing marked it is
 * a stub pointing up — the one instruction missing right after the fan opens is
 * *where* — and it stretches to the target once the finger picks one.
 *
 * The tip is an `Animatable<Offset>` read in the draw phase, so the line follows
 * a change of target with the discs' own spring without recomposing anyone.
 */
@Composable
private fun GuideToTarget(target: GestureTarget, fanOpen: Boolean) {
    val density = LocalDensity.current
    val marked = (target as? GestureTarget.Mode)?.format
    val destination = with(density) {
        val point = marked?.let(::offsetOf) ?: DpOffset(0.dp, -(ORIGIN_RADIUS + GUIDE_STUB))
        Offset(point.x.toPx(), point.y.toPx())
    }

    val tip = remember { Animatable(destination, Offset.VectorConverter) }
    LaunchedEffect(destination) { tip.animateTo(destination, Motion.gestureSpring()) }

    val presence = animateFloatAsState(
        targetValue = if (fanOpen) 1f else 0f,
        animationSpec = tween(Motion.FAST),
        label = "guia",
    )
    val emphasis = animateFloatAsState(
        targetValue = if (marked != null) 1f else 0f,
        animationSpec = tween(TARGET_HIGHLIGHT_MS),
        label = "realceDaGuia",
    )

    Box(
        Modifier.requiredSize(1.dp).drawBehind {
            val strength = presence.value
            if (strength < 0.01f) return@drawBehind
            val end = tip.value
            val distance = end.getDistance()
            if (distance < 1f) return@drawBehind
            val start = end * (ORIGIN_RADIUS.toPx() / distance)
            drawLine(
                color = Color.White,
                start = center + start,
                end = center + end * strength,
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round,
                alpha = (0.26f + 0.18f * emphasis.value) * strength,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 7.dp.toPx())),
            )
        },
    )
}

/**
 * The sentence that changes as the finger moves.
 *
 * "drag and drop on a target" only holds while nothing is marked; once a target
 * is under the finger the useful sentence is what **releasing** will do — and the
 * pill wears that target's color, which is the second place the color-to-mode
 * association forms.
 */
@Composable
private fun GestureHint(target: GestureTarget, fanOpen: Boolean) {
    val marked = (target as? GestureTarget.Mode)?.format
    val text = when (marked) {
        CaptureFormat.TEXT -> "solte para escrever"
        CaptureFormat.AUDIO -> "solte para gravar"
        CaptureFormat.PHOTO -> "solte para fotografar"
        null -> "arraste e solte no alvo"
    }
    val background by animateColorAsState(
        targetValue = marked?.let { formatColors(it).color } ?: NIGHT.copy(alpha = 0.82f),
        animationSpec = tween(TARGET_HIGHLIGHT_MS),
        label = "fundoDaDica",
    )
    val presence = animateFloatAsState(
        targetValue = if (fanOpen) 1f else 0f,
        animationSpec = tween(Motion.FAST),
        label = "dica",
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .offset(y = (-238).dp)
            .graphicsLayer { alpha = presence.value }
            // The sentence resizes with the target; without this the pill jumps
            // in width every time the finger enters a disc.
            .animateContentSize(tween(TARGET_HIGHLIGHT_MS, easing = LinearOutSlowInEasing))
            .background(background, CircleShape)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            fontWeight = if (marked != null) FontWeight.Bold else FontWeight.Medium,
        )
    }
}
