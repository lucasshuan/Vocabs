package com.jean.vocabs.media

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import com.jean.vocabs.shared.media.ArquivosDeMidia
import java.io.File

/**
 * Gravador de memo de voz.
 *
 * Sem transcrição automática, por decisão do roadmap: gravar é o que precisa ser
 * instantâneo. Transformar em texto é trabalho de outro momento.
 */
class GravadorDeAudio(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var arquivo: File? = null

    val gravando: Boolean get() = recorder != null

    /** Começa a gravar imediatamente. Devolve false se o microfone não abriu. */
    fun iniciar(): Boolean {
        if (gravando) return true

        val destino = ArquivosDeMidia.novoAudio(context)
        val novo = criarRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(96_000)
            setAudioSamplingRate(44_100)
            setOutputFile(destino.absolutePath)
        }

        return runCatching {
            novo.prepare()
            novo.start()
            recorder = novo
            arquivo = destino
            true
        }.getOrElse {
            novo.release()
            destino.delete()
            false
        }
    }

    /**
     * Encerra e devolve o arquivo gravado, ou null se não deu para salvar.
     *
     * `stop()` lança se a gravação durou menos que alguns décimos de segundo —
     * um toque duplo sem querer, por exemplo. Nesse caso o arquivo sai corrompido
     * e é melhor descartar do que deixar um áudio mudo no inbox.
     */
    fun parar(): File? {
        val atual = recorder ?: return null
        val destino = arquivo

        val salvou = runCatching { atual.stop() }.isSuccess
        atual.release()
        recorder = null
        arquivo = null

        if (!salvou || destino == null) {
            destino?.delete()
            return null
        }
        return destino.takeIf { it.length() > 0 }
    }

    /** Aborta sem guardar nada — usado quando a tela morre no meio da gravação. */
    fun cancelar() {
        recorder?.let { atual ->
            runCatching { atual.stop() }
            atual.release()
        }
        recorder = null
        arquivo?.delete()
        arquivo = null
    }

    private fun criarRecorder(): MediaRecorder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
}
