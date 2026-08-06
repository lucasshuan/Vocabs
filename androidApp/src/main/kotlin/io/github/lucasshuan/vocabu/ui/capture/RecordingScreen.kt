package io.github.lucasshuan.vocabu.ui.capture

import androidx.compose.ui.res.stringResource
import io.github.lucasshuan.vocabu.R
import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import io.github.lucasshuan.vocabu.contracts.Language
import io.github.lucasshuan.vocabu.ui.components.AppIcons
import io.github.lucasshuan.vocabu.ui.components.CircularFlag
import io.github.lucasshuan.vocabu.ui.components.Motion
import io.github.lucasshuan.vocabu.ui.components.shrinkOnTouch
import io.github.lucasshuan.vocabu.ui.components.formatDuration
import io.github.lucasshuan.vocabu.ui.components.rememberHaptics
import io.github.lucasshuan.vocabu.ui.components.breathing
import io.github.lucasshuan.vocabu.ui.languages.displayName
import io.github.lucasshuan.vocabu.ui.theme.LocalDarkTheme
import io.github.lucasshuan.vocabu.ui.theme.VocabuColors
import kotlin.math.pow
import kotlinx.coroutines.delay

/**
 * Recording starts with the hand already free — the phone can be set down or
 * moved closer to whoever is talking. Ending is a tap, never a gesture.
 *
 * Opaque, and the last thing the hub draws, so the `+` underneath is not
 * clickable through it.
 */
@Composable
internal fun RecordingScreen(
    capture: QuickCapture,
    language: Language,
    isRecording: Boolean,
    modifier: Modifier = Modifier,
) {
    val save: () -> Unit = { capture.saveAudio() }
    val cancel: () -> Unit = { capture.cancelAudio() }

    // Back saves. A mistaken save costs one tap in Pending; a mistaken discard
    // cannot be undone at all.
    BackHandler(enabled = isRecording) { save() }

    // The light theme's dark system icons would vanish against this background.
    val view = LocalView.current
    val darkTheme = LocalDarkTheme.current
    DisposableEffect(darkTheme) {
        val controller = (view.context as? Activity)
            ?.window
            ?.let { WindowCompat.getInsetsController(it, view) }
        controller?.isAppearanceLightStatusBars = false
        onDispose { controller?.isAppearanceLightStatusBars = !darkTheme }
    }

    val presence = animateFloatAsState(
        targetValue = if (isRecording) 1f else 0f,
        animationSpec = tween(
            if (isRecording) Motion.DEFAULT else Motion.FAST,
            easing = FastOutSlowInEasing,
        ),
        label = "recordingScreen",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { alpha = presence.value }
            .drawBehind { drawRect(NIGHT) }
            .then(if (isRecording) Modifier.barTaps() else Modifier),
    ) {
        RecordingBadge(
            isRecording = isRecording,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 14.dp)
                .graphicsLayer { alpha = presence.value },
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(19.dp),
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-56).dp)
                .graphicsLayer { translationY = 18.dp.toPx() * (1f - presence.value) },
        ) {
            Text(
                text = formatDuration(capture.seconds),
                style = MaterialTheme.typography.displaySmall.copy(fontSize = 46.sp, lineHeight = 50.sp),
                color = Color.White,
            )
            MicWave(isRecording) { capture.levelNow() }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                CircularFlag(language, size = 16.dp)
                // No language detector: the capture lands in the course open in
                // the hub, and this sentence says so. Revisit if detection lands.
                Text(
                    text = stringResource(R.string.recording_goes_to_course, language.displayName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.72f),
                )
            }
        }

        RecordingActions(
            onDiscard = cancel,
            onSave = save,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
                .graphicsLayer { translationY = 26.dp.toPx() * (1f - presence.value) },
        )
    }
}

/**
 * Blocks touch below without consuming: Compose stops its hit test at the
 * topmost child, and consuming as well makes other gestures give up.
 */
private fun Modifier.barTaps(): Modifier = pointerInput(Unit) {
    awaitEachGesture { awaitFirstDown(requireUnconsumed = false) }
}

/**
 * Dark in both themes, so the light values are hard-coded: `colorScheme.tertiary`
 * is the light theme's dark green, nearly invisible here.
 *
 * Salmon is the one place red does not mean the photo category. It is affordable
 * only because no photo target exists on this screen.
 */
private val MINT_AT_NIGHT = VocabuColors.Mint
private val SALMON_AT_NIGHT = VocabuColors.ParrotDark
private val FULL_GREEN = VocabuColors.MintDark

/**
 * The dot pulses: it is the only proof the microphone is open that an eye
 * glancing at a phone on the table reads without reading numbers.
 */
@Composable
private fun RecordingBadge(isRecording: Boolean, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .darkPill()
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Box(
            Modifier
                .requiredSize(8.dp)
                .breathing(active = isRecording, minimum = 0.25f)
                .drawBehind { drawCircle(SALMON_AT_NIGHT) },
        )
        Text(
            text = stringResource(R.string.recording_recording),
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.86f),
        )
    }
}

private fun Modifier.darkPill(): Modifier = drawBehind {
    drawRoundRect(Color.White, alpha = 0.08f, cornerRadius = CornerRadius(size.height / 2f))
}

private val ACTION_HEIGHT = 68.dp
private val ACTION_CORNER = 22.dp

/**
 * Different sizes on purpose: two equal targets at the base of a screen read at
 * a glance invite the wrong tap, and area separates them, not colour.
 *
 * Neither confirms. A mistaken save is undone in Pending; discarding is itself
 * the request to discard.
 */
@Composable
private fun RecordingActions(
    onDiscard: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxWidth().height(ACTION_HEIGHT),
    ) {
        DiscardButton(onDiscard)
        BotaoDeGuardar(onSave)
    }
}

@Composable
private fun DiscardButton(onClick: () -> Unit) {
    val touch = rememberHaptics()
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(ACTION_CORNER),
        color = Color.Transparent,
        contentColor = SALMON_AT_NIGHT,
        border = BorderStroke(1.dp, SALMON_AT_NIGHT.copy(alpha = 0.45f)),
        interactionSource = touch,
        modifier = Modifier
            .width(86.dp)
            .fillMaxHeight()
            .shrinkOnTouch(touch, minimum = 0.94f),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterVertically),
            modifier = Modifier.fillMaxSize(),
        ) {
            Icon(AppIcons.Trash, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(
                text = stringResource(R.string.recording_discard),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun RowScope.BotaoDeGuardar(onClick: () -> Unit) {
    val touch = rememberHaptics()
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(ACTION_CORNER),
        color = FULL_GREEN,
        contentColor = Color.White,
        interactionSource = touch,
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            // Less than the cards' 0.97: at this size that ratio reads as a jolt.
            .shrinkOnTouch(touch, minimum = 0.985f),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp, Alignment.CenterHorizontally),
            modifier = Modifier.fillMaxSize(),
        ) {
            Icon(AppIcons.Check, contentDescription = null, modifier = Modifier.size(22.dp))
            Text(text = stringResource(R.string.save), style = MaterialTheme.typography.titleMedium)
        }
    }
}

private const val WAVE_BARS = 22
private const val WAVE_INTERVAL = 70L

/**
 * The microphone's real peak, one bar every 70ms. The `pow` is perceptual, not
 * decorative: normal speech sits at the bottom of the linear scale, and the wave
 * would be flat for the whole recording without it.
 *
 * A raw `FloatArray` with only the cursor as Compose state, read in `drawBehind`.
 */
@Composable
private fun MicWave(isRecording: Boolean, level: () -> Float) {
    val history = remember { FloatArray(WAVE_BARS) }
    var cursor by remember { mutableIntStateOf(0) }

    LaunchedEffect(isRecording) {
        if (!isRecording) return@LaunchedEffect
        history.fill(0f)
        cursor = 0
        while (true) {
            history[cursor % WAVE_BARS] = level()
            cursor++
            delay(WAVE_INTERVAL)
        }
    }

    Box(
        Modifier
            .requiredSize(width = 208.dp, height = 52.dp)
            .drawBehind {
                val position = cursor
                val width = 4.dp.toPx()
                val gap = 5.5.dp.toPx()
                val smallest = 4.dp.toPx()
                for (i in 0 until WAVE_BARS) {
                    val index = ((position - 1 - i) % WAVE_BARS + WAVE_BARS) % WAVE_BARS
                    val height = smallest + (size.height - smallest) * history[index].coerceIn(0f, 1f).pow(0.42f)
                    val x = size.width - width - i * gap
                    if (x < 0) break
                    drawRoundRect(
                        color = MINT_AT_NIGHT,
                        topLeft = Offset(x, (size.height - height) / 2f),
                        size = Size(width, height),
                        cornerRadius = CornerRadius(width / 2f),
                        alpha = 1f - 0.5f * (i.toFloat() / WAVE_BARS),
                    )
                }
            },
    )
}
