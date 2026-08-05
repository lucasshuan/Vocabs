package io.github.lucasshuan.vocabu.ui.components

import android.provider.Settings
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * The app's motion vocabulary. Nothing the person *waits on* exceeds [DEFAULT];
 * only what can be read while it runs reaches [WIDE].
 */
object Motion {
    const val FAST = 150

    const val DEFAULT = 240

/** Only for what is read while it runs. Never for what blocks a tap. */
    const val WIDE = 620

    /**
     * Short on purpose. At 34 ms with the [STAGGERED_ITEMS] cap the last item
     * leaves 170 ms after the first — below what reads as a delay.
     */
    const val STAGGER_STEP = 34L

    /**
     * Without the cap the tenth card in a list would wait half a second, making
     * the stagger the bottleneck it exists to avoid.
     */
    const val STAGGERED_ITEMS = 5

    fun <T> standardSpring() = spring<T>(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow)

    fun <T> elasticSpring() = spring<T>(dampingRatio = 0.52f, stiffness = Spring.StiffnessMediumLow)

    /**
     * Stiffer than [standardSpring] because the fan's targets move far while the
     * gesture is still running: a target taking 300 ms to arrive is still
     * travelling when the finger has already chosen.
     */
    fun <T> gestureSpring() = spring<T>(dampingRatio = 0.72f, stiffness = Spring.StiffnessMedium)
}

/**
 * "Reduce motion" on Android is the animator duration scale at zero. Read once
 * per composition and not observed: the switch lives in system Settings, and
 * returning to the app recomposes everything anyway.
 */
@Composable
fun reducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
    }
}

/**
 * Costs nothing: `graphicsLayer` scales in the draw phase, with no remeasure.
 *
 * Takes the same [source] passed to the `Surface`/`clickable` — whoever draws the
 * ripple and whoever shrinks must react to the same touch.
 */
@Composable
fun Modifier.shrinkOnTouch(
    source: MutableInteractionSource,
    minimum: Float = 0.97f,
): Modifier {
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) minimum else 1f,
        animationSpec = Motion.standardSpring(),
        label = "touchScale",
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

@Composable
fun rememberHaptics(): MutableInteractionSource = remember { MutableInteractionSource() }

/**
 * **Not for `LazyColumn`/`LazyRow`.** There an item is recomposed every time it
 * re-enters the viewport, so the animation would restart on every scroll. Those
 * use `Modifier.animateItem()`.
 */
@Composable
fun Modifier.smoothEntrance(
    index: Int = 0,
    offset: Dp = 12.dp,
): Modifier {
    var arrived by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val wait = index.coerceIn(0, Motion.STAGGERED_ITEMS) * Motion.STAGGER_STEP
        if (wait > 0) delay(wait)
        arrived = true
    }
    val progress by animateFloatAsState(
        targetValue = if (arrived) 1f else 0f,
        animationSpec = tween(Motion.DEFAULT, easing = FastOutSlowInEasing),
        label = "smoothEntrance",
    )
    val heightInPx = with(LocalDensity.current) { offset.toPx() }
    return graphicsLayer {
        alpha = progress
        translationY = (1f - progress) * heightInPx
    }
}

/**
 * Returns the `State` rather than the `Float` so a `Canvas` or `drawBehind`
 * caller reads `.value` *inside* the draw lambda and invalidates only the draw
 * phase. Returning the number would recompose every frame.
 */
@Composable
fun animatedFraction(target: Float, label: String = "fraction"): State<Float> {
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    return animateFloatAsState(
        targetValue = if (started) target.coerceIn(0f, 1f) else 0f,
        animationSpec = tween(Motion.WIDE, easing = FastOutSlowInEasing),
        label = label,
    )
}

/**
 * For accumulated achievement, never for a queue or a debt — watching "12 due"
 * count up from zero would celebrate falling behind.
 */
@Composable
fun animatedCount(target: Int, label: String = "count"): Int {
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    val value by animateIntAsState(
        targetValue = if (started) target else 0,
        animationSpec = tween(Motion.WIDE, easing = FastOutSlowInEasing),
        label = label,
    )
    return value
}

/**
 * Always conditioned on the state that justifies it: an infinite animation
 * outliving its work keeps Compose recomposing forever.
 */
@Composable
fun Modifier.breathing(active: Boolean, minimum: Float = 0.45f): Modifier {
    if (!active) return this
    val transition = rememberInfiniteTransition(label = "breath")
    val opacity by transition.animateFloat(
        initialValue = 1f,
        targetValue = minimum,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breathOpacity",
    )
    return graphicsLayer { alpha = opacity }
}
