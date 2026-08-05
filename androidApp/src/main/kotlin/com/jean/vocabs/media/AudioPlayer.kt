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

/** De quanto em quanto tempo a agulha é lida do player enquanto toca. */
private const val NEEDLE_INTERVAL = 60L

/** Estado de reprodução de um memo de voz, já amarrado ao ciclo de vida da tela. */
class AudioState internal constructor(private val path: String) {

    private var player: MediaPlayer? = null
    private var internalPlaying by mutableStateOf(false)
    private var internalPosition by mutableLongStateOf(0L)

    /**
     * A duração vem do player e não precisa ser estado observável: ela é escrita
     * antes de [tocandoInterno] virar `true`, e é essa virada que recompõe.
     */
    private var internalDuration = 0L

    val playing: Boolean get() = internalPlaying

    /** Quanto já tocou, em ms. Zera ao parar. */
    val positionMs: Long get() = internalPosition

    /** De 0 a 1: onde a agulha está. É o que a onda usa para se preencher. */
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
     * Acompanha a agulha enquanto o áudio corre.
     *
     * Dezesseis leituras por segundo, e não uma por quadro: a onda é desenhada em
     * barras de 3 dp, então a agulha só muda de casa a cada punhado de quadros —
     * pedir a posição ao `MediaPlayer` sessenta vezes por segundo pagaria a
     * travessia para o nativo sem mexer um pixel a mais.
     */
    internal suspend fun follow() {
        while (internalPlaying) {
            internalPosition = player?.currentPosition?.toLong() ?: 0L
            delay(NEEDLE_INTERVAL)
        }
    }
}

/**
 * O `DisposableEffect` não é detalhe: sem soltar o MediaPlayer ao sair da tela,
 * o áudio continuaria tocando por cima da próxima tela.
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
