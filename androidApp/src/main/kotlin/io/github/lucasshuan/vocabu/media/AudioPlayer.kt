package io.github.lucasshuan.vocabu.media

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

private const val NEEDLE_INTERVAL = 60L

class AudioState internal constructor(private val path: String) {

    private var player: MediaPlayer? = null
    private var internalPlaying by mutableStateOf(false)
    private var internalPosition by mutableLongStateOf(0L)

    /**
     * Not observable: written before [internalPlaying] turns `true`, which is
     * what recomposes.
     */
    private var internalDuration = 0L

    val playing: Boolean get() = internalPlaying

    /** Resets on stop. */
    val positionMs: Long get() = internalPosition

    /** 0 to 1 — what the wave fills itself from. */
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
     * Sixteen reads a second, not one per frame: the wave's bars are 3dp, so the
     * needle moves a slot every handful of frames, and asking `MediaPlayer` at
     * 60Hz pays the native crossing for no extra pixel.
     */
    internal suspend fun follow() {
        while (internalPlaying) {
            internalPosition = player?.currentPosition?.toLong() ?: 0L
            delay(NEEDLE_INTERVAL)
        }
    }
}

/** Without the `DisposableEffect` the audio plays on over the next screen. */
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
