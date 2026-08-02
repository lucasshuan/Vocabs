package com.jean.vocabs.media

import android.media.MediaPlayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.State

/** Estado de reprodução de um memo de voz, já amarrado ao ciclo de vida da tela. */
class EstadoDoAudio internal constructor(private val caminho: String) {

    private var player: MediaPlayer? = null
    private var tocandoInterno by mutableStateOf(false)

    val tocando: Boolean get() = tocandoInterno

    fun alternar() {
        if (tocandoInterno) parar() else tocar()
    }

    private fun tocar() {
        val novo = MediaPlayer()
        val abriu = runCatching {
            novo.setDataSource(caminho)
            novo.prepare()
            novo.setOnCompletionListener { parar() }
            novo.start()
        }.isSuccess

        if (!abriu) {
            novo.release()
            return
        }
        player = novo
        tocandoInterno = true
    }

    fun parar() {
        player?.let { atual ->
            runCatching { atual.stop() }
            atual.release()
        }
        player = null
        tocandoInterno = false
    }
}

/**
 * O `DisposableEffect` não é detalhe: sem soltar o MediaPlayer ao sair da tela,
 * o áudio continuaria tocando por cima da próxima tela.
 */
@Composable
fun rememberReprodutor(caminho: String): State<EstadoDoAudio> {
    val estado = remember(caminho) { mutableStateOf(EstadoDoAudio(caminho)) }
    DisposableEffect(caminho) {
        onDispose { estado.value.parar() }
    }
    return estado
}
