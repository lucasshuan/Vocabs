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
private const val INTERVALO_DA_AGULHA = 60L

/** Estado de reprodução de um memo de voz, já amarrado ao ciclo de vida da tela. */
class EstadoDoAudio internal constructor(private val path: String) {

    private var player: MediaPlayer? = null
    private var tocandoInterno by mutableStateOf(false)
    private var posicaoInterna by mutableLongStateOf(0L)

    /**
     * A duração vem do player e não precisa ser estado observável: ela é escrita
     * antes de [tocandoInterno] virar `true`, e é essa virada que recompõe.
     */
    private var duracaoInterna = 0L

    val tocando: Boolean get() = tocandoInterno

    /** Quanto já tocou, em ms. Zera ao parar. */
    val posicaoMs: Long get() = posicaoInterna

    /** De 0 a 1: onde a agulha está. É o que a onda usa para se preencher. */
    val progresso: Float
        get() = if (duracaoInterna > 0L) (posicaoInterna.toFloat() / duracaoInterna).coerceIn(0f, 1f) else 0f

    fun alternar() {
        if (tocandoInterno) parar() else tocar()
    }

    private fun tocar() {
        val novo = MediaPlayer()
        val abriu = runCatching {
            novo.setDataSource(path)
            novo.prepare()
            novo.setOnCompletionListener { parar() }
            novo.start()
        }.isSuccess

        if (!abriu) {
            novo.release()
            return
        }
        player = novo
        duracaoInterna = novo.duration.toLong().coerceAtLeast(0L)
        posicaoInterna = 0L
        tocandoInterno = true
    }

    fun parar() {
        player?.let { current ->
            runCatching { current.stop() }
            current.release()
        }
        player = null
        posicaoInterna = 0L
        tocandoInterno = false
    }

    /**
     * Acompanha a agulha enquanto o áudio corre.
     *
     * Dezesseis leituras por segundo, e não uma por quadro: a onda é desenhada em
     * barras de 3 dp, então a agulha só muda de casa a cada punhado de quadros —
     * pedir a posição ao `MediaPlayer` sessenta vezes por segundo pagaria a
     * travessia para o nativo sem mexer um pixel a mais.
     */
    internal suspend fun acompanhar() {
        while (tocandoInterno) {
            posicaoInterna = player?.currentPosition?.toLong() ?: 0L
            delay(INTERVALO_DA_AGULHA)
        }
    }
}

/**
 * O `DisposableEffect` não é detalhe: sem soltar o MediaPlayer ao sair da tela,
 * o áudio continuaria tocando por cima da próxima tela.
 */
@Composable
fun rememberReprodutor(path: String): EstadoDoAudio {
    val estado = remember(path) { EstadoDoAudio(path) }
    LaunchedEffect(estado, estado.tocando) {
        if (estado.tocando) estado.acompanhar()
    }
    DisposableEffect(estado) {
        onDispose { estado.parar() }
    }
    return estado
}
