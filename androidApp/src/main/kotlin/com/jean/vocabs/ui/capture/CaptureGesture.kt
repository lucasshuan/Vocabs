package com.jean.vocabs.ui.capture

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.jean.vocabs.shared.domain.CaptureFormat

/**
 * Where the finger is, as far as the capture gesture is concerned.
 *
 * The three positions are exhaustive, and that is what makes the gesture
 * reversible: there is no "almost chose" state and no target that stays marked
 * after the finger left it. Releasing always runs exactly what the screen is
 * showing at that instant — and **only** releasing runs anything.
 */
sealed interface GestureTarget {

    /** On the `+`, where the gesture started. */
    data object Origin : GestureTarget

    /** Neither origin nor target. Releasing here does nothing. */
    data object Outward : GestureTarget

    data class Mode(val format: CaptureFormat) : GestureTarget
}

/**
 * Where the three targets sit around the `+`, in dp from its center.
 *
 * A real arc: all three are the **same distance** from the `+` — 152 dp — with
 * audio on the axis as the likeliest and the other two at ±54°. None costs more
 * reach than another; the thumb sweeps one curve.
 *
 * The spread is wide on purpose. With the side targets higher and further in, the
 * discs crowded each other and the no-man's-land between them — what keeps
 * crossing the fan from marking anything by accident — was too narrow to feel.
 * Lowering them and pushing them to the edges leaves ~137 dp center to center.
 */
val AUDIO_OFFSET = DpOffset(0.dp, (-152).dp)
val TEXT_OFFSET = DpOffset((-122).dp, (-90).dp)
val PHOTO_OFFSET = DpOffset(122.dp, (-90).dp)

/**
 * The three targets are born identical.
 *
 * None is larger or filled before being reached: a target already painted before
 * the finger arrives promises something is underway, and nothing is.
 * [MARKED_TARGET_DIAMETER] is the size of what is under the finger, and it is the
 * only signal of choice that exists.
 */
val TARGET_DIAMETER = 68.dp
val MARKED_TARGET_DIAMETER = 76.dp

/**
 * How close the finger can pass and still count as being on a target.
 *
 * A 44 dp radius gives an 88 dp touch area — well over Material's 48 dp minimum
 * and larger than the 68 dp disc, so the target is easier to hit than to see.
 * Between two neighbours ~27 dp of no-man's-land is left, and that is where the
 * gesture chooses nothing: a target marked by relative proximity would mark
 * something across the whole top half of the screen.
 */
val TARGET_RADIUS = 44.dp

/**
 * The radius around the `+` where the gesture has chosen nothing — the "I only
 * touched it" zone. Releasing here opens text, which is what a slow tap on the
 * button was asking for.
 */
val ORIGIN_RADIUS = 56.dp

/** Past this the touch became a press and the fan opens even without movement. */
const val FAN_OPEN_MS = 180L

/**
 * The target highlight chases the finger, so it is shorter than any duration in
 * the app's motion vocabulary.
 *
 * `Motion.FAST` (150 ms) is a chip reacting to a touch that already ended. Here
 * the finger is still moving: 90 ms is the ceiling for the highlight to land
 * before the hand doubts which target is marked.
 */
const val TARGET_HIGHLIGHT_MS = 90

/** Each mode's offset, for whoever draws the targets. */
fun offsetOf(format: CaptureFormat): DpOffset = when (format) {
    CaptureFormat.TEXT -> TEXT_OFFSET
    CaptureFormat.AUDIO -> AUDIO_OFFSET
    CaptureFormat.PHOTO -> PHOTO_OFFSET
}

/** The same three offsets in pixels, ready to compare against the finger. */
fun Density.targetsInPixels(): List<Pair<CaptureFormat, Offset>> =
    CaptureFormat.entries.map { format ->
        val destination = offsetOf(format)
        format to Offset(destination.x.toPx(), destination.y.toPx())
    }

/**
 * Which target an offset from the `+` selects.
 *
 * **The target has to be reached.** The earlier version chose by angle — pointing
 * was enough, and the drawn disc was just the illustration of a sector. That
 * saved reach and charged for it elsewhere: with audio filling the whole central
 * sector, a short upward swipe already marked "record", and what was meant as a
 * pointer became a trigger. Now the target is a thing in a place.
 *
 * Every measure arrives in pixels because the caller is inside a
 * `PointerInputScope`, where positions are pixels and density is at hand.
 */
fun targetFor(
    shift: Offset,
    targets: List<Pair<CaptureFormat, Offset>>,
    targetRadiusPx: Float,
    originRadiusPx: Float,
): GestureTarget {
    val nearest = targets.minByOrNull { (_, centro) -> (shift - centro).getDistanceSquared() }
    if (nearest != null && (shift - nearest.second).getDistance() <= targetRadiusPx) {
        return GestureTarget.Mode(nearest.first)
    }
    return if (shift.getDistance() < originRadiusPx) GestureTarget.Origin else GestureTarget.Outward
}

