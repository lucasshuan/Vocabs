package io.github.lucasshuan.vocabu.ui.capture

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import io.github.lucasshuan.vocabu.shared.domain.CaptureFormat

/**
 * Exhaustive, which is what makes the gesture reversible: no "almost chose"
 * state, no target still marked after the finger left it.
 */
sealed interface GestureTarget {

    data object Origin : GestureTarget

    /** Releasing here does nothing. */
    data object Outward : GestureTarget

    data class Mode(val format: CaptureFormat) : GestureTarget
}

/**
 * All three at 152dp from the `+`, audio on the axis and the others at ±54°, so
 * the thumb sweeps one curve. Closer in, the no-man's-land between them was too
 * narrow to feel; this leaves ~137dp centre to centre.
 */
val AUDIO_OFFSET = DpOffset(0.dp, (-152).dp)
val TEXT_OFFSET = DpOffset((-122).dp, (-90).dp)
val PHOTO_OFFSET = DpOffset(122.dp, (-90).dp)

/**
 * Born identical. A target pre-painted before the finger arrives promises
 * something is under way, and nothing is.
 */
val TARGET_DIAMETER = 68.dp
val MARKED_TARGET_DIAMETER = 76.dp

/**
 * An 88dp touch area — over Material's 48dp minimum and wider than the 68dp
 * disc, so a target is easier to hit than to see. ~27dp of no-man's-land is left
 * between neighbours; relative proximity instead would mark something across the
 * whole top half of the screen.
 */
val TARGET_RADIUS = 44.dp

/** Releasing here opens text, which is what a slow tap asked for. */
val ORIGIN_RADIUS = 56.dp

/** Past this the touch is a press and the fan opens without movement. */
const val FAN_OPEN_MS = 180L

/**
 * Not `Motion.FAST` (150ms): that is a chip reacting to a touch already over.
 * Here the finger is still moving, and 90ms is the ceiling before the hand
 * doubts which target is marked.
 */
const val TARGET_HIGHLIGHT_MS = 90

fun offsetOf(format: CaptureFormat): DpOffset = when (format) {
    CaptureFormat.TEXT -> TEXT_OFFSET
    CaptureFormat.AUDIO -> AUDIO_OFFSET
    CaptureFormat.PHOTO -> PHOTO_OFFSET
}

fun Density.targetsInPixels(): List<Pair<CaptureFormat, Offset>> =
    CaptureFormat.entries.map { format ->
        val destination = offsetOf(format)
        format to Offset(destination.x.toPx(), destination.y.toPx())
    }

/**
 * The target has to be reached. Choosing by angle — the earlier version — let a
 * short upward swipe mark "record", turning a pointer into a trigger.
 *
 * Measures arrive in pixels: the caller is inside a `PointerInputScope`.
 */
fun targetFor(
    shift: Offset,
    targets: List<Pair<CaptureFormat, Offset>>,
    targetRadiusPx: Float,
    originRadiusPx: Float,
): GestureTarget {
    val nearest = targets.minByOrNull { (_, center) -> (shift - center).getDistanceSquared() }
    if (nearest != null && (shift - nearest.second).getDistance() <= targetRadiusPx) {
        return GestureTarget.Mode(nearest.first)
    }
    return if (shift.getDistance() < originRadiusPx) GestureTarget.Origin else GestureTarget.Outward
}

