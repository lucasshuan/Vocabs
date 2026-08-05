package com.jean.vocabs.media

import android.media.MediaPlayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

/** How often the needle is read from the player while playing. */
private const val NEEDLE_INTERVAL = 60L

/** Playback state for a voice memo, already tied to the screen's lifecycle. */
class AudioState internal constructor(private val path: String) {

    private var player: MediaPlayer? = null
    private var internalPlaying by mutableStateOf(false)
    private var internalPosition by mutableLongStateOf(0L)

    /**
     * The duration comes from the player and need not be observable state: it is
     * written before [internalPlaying] turns `true`, and that is what recomposes.
     */
    private var internalDuration = 0L

    val playing: Boolean get() = internalPlaying

    /** How much has played, in ms. Resets on stop. */
    val positionMs: Long get() = internalPosition

    /** 0 to 1: where the needle is. What the wave uses to fill itself. */
    val progress: Float
        get() = if (internalDuration > 0L) (internalPosition.toFloat() / internalDuration).coerceIn(0f, 1f) else 0f

    fun alternar() {
        if (internalPlaying) stop() else play()
    }

    private fun play() {
        val new = MediaPlayer()
        val didOpen = runCatching {
            new.setDataSource(path)
            new.prepare()
            new.setOnCompletionListener { stop() }
            new.start()
        }.isSuccess

        if (!didOpen) {
            new.release()
            return
        }
        player = new
        internalDuration = new.duration.toLong().coerceAtLeast(0L)
        internalPosition = 0L
        internalPlaying = true
    }

    fun stop() {
        player?.let { current ->
            runCatching { current.stop() }
            current.release()
        }
        player = null
        internalPosition = 0L
        internalPlaying = false
    }

    /**
     * Follows the needle while the audio runs.
     *
     * Sixteen reads a second rather than one per frame: the wave is drawn in 3 dp
     * bars, so the needle only moves a slot every handful of frames. Asking
     * `MediaPlayer` sixty times a second would pay the native crossing without
     * moving one extra pixel.
     */
    internal suspend fun follow() {
        while (internalPlaying) {
            internalPosition = player?.currentPosition?.toLong() ?: 0L
            delay(NEEDLE_INTERVAL)
        }
    }
}

/**
 * The `DisposableEffect` is not a detail: without releasing the MediaPlayer on
 * leaving, the audio would keep playing over the next screen.
 */
@Composable
fun rememberPlayer(path: String): AudioState {
    val state = remember(path) { AudioState(path) }
    LaunchedEffect(state, state.playing) {
        if (state.playing) state.follow()
    }
    DisposableEffect(state) {
        onDispose { state.stop() }
    }
    return state
}
