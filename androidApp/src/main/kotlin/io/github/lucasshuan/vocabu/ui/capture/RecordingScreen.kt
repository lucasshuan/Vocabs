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
 * The recording, with its own screen and actions.
 *
 * The gesture ends when the finger lifts, so recording **starts** with the hand
 * already free — the phone can be set down, moved closer to whoever is talking,
 * or swapped between hands without risking the capture. Ending is a tap, never a
 * gesture.
 *
 * Opaque, and the last thing the hub draws: it sits above the `+`, which must not
 * stay clickable under a screen that is no longer it.
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

    // Back **saves**. Nothing is discarded without being asked for, and the ask
    // has one place: the discard button. Audio saved by mistake costs one tap in
    // Pending; audio discarded by mistake cannot be undone at all.
    BackHandler(enabled = isRecording) { save() }

    // On the light theme the system icons are dark and would vanish against this
    // background, so they flip to light for the duration.
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
                // There is no language detector in the app: the capture lands in
                // the course open in the hub. This sentence is the one that has
                // to change the day detection exists.
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
 * Blocks touch below without consuming anything.
 *
 * Being in the way is enough: Compose stops its hit test at the topmost child it
 * reaches. Consuming as well was actively harmful — Compose gestures give up when
 * they meet an already-consumed change.
 */
private fun Modifier.barTaps(): Modifier = pointerInput(Unit) {
    awaitEachGesture { awaitFirstDown(requireUnconsumed = false) }
}

/**
 * The screen is dark in both themes, so these are the light colors.
 * `colorScheme.tertiary` is the dark green on the light theme — legible on white
 * and nearly invisible here.
 *
 * Everywhere else in the app red means **category** (photo), never error or
 * action. This screen is the one exception and can afford it: no photo target
 * exists here, so the two senses never share a screen. Pulling in the theme's
 * `error` would create a second red almost identical to the first.
 */
private val MINT_AT_NIGHT = VocabuColors.Mint
private val SALMON_AT_NIGHT = VocabuColors.ParrotDark
private val FULL_GREEN = VocabuColors.MintDark

/**
 * The dot pulses because it is the only element proving the microphone is open
 * right now — the running clock says the same, but an eye glancing at a phone on
 * the table does not read numbers.
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
 * Discard on the left, save on the right, one tap each.
 *
 * **Deliberately different sizes.** Two equal targets at the base of a screen
 * read at a glance invite the wrong tap; what separates them is area, not color.
 * Save takes the remaining width and is the only filled one; discard is narrow,
 * outline only, in the corner a finger does not reach by accident.
 *
 * Neither asks for confirmation. Saving by mistake is undone in Pending;
 * discarding is itself the request to discard.
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
            // Less than the cards (0.97): at this size the same ratio reads as a
            // jolt rather than a touch.
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
 * The wave: the microphone's real peak, one bar every 70 ms.
 *
 * The square root on the peak is perceptual, not decorative — normal speech lives
 * at the bottom of the linear scale, and without it the wave would spend the
 * whole recording flat.
 *
 * History is a raw `FloatArray` with an observable cursor. Only the cursor is
 * Compose state, and it is read inside `drawBehind`.
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
