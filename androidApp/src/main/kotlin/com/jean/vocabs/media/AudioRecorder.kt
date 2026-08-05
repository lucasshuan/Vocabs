package com.jean.vocabs.media

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.jean.vocabs.shared.media.MediaFiles
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import kotlin.concurrent.thread

/** Grava PCM 16 kHz mono dentro de WAV, formato aceito pelo reconhecimento local. */
class AudioRecorder(private val context: Context) {

    private var audioRecord: AudioRecord? = null
    private var file: File? = null
    private var trabalhador: Thread? = null

    @Volatile
    private var escrevendo = false

    /**
     * O pico do último bloco lido, de 0 a 1.
     *
     * A onda da tela de gravação é desenhada a partir daqui, e não de alturas
     * decorativas: uma onda que se mexe igual no silêncio e na fala não diz se o
     * microfone está pegando alguma coisa — que é a única pergunta que alguém tem
     * enquanto segura o botão. Volátil porque quem escreve é a thread do WAV e
     * quem lê é a de desenho; a leitura suja de um float aqui custa um quadro
     * torto e nada mais.
     */
    @Volatile
    var level: Float = 0f
        private set

    val gravando: Boolean get() = escrevendo

    @Suppress("MissingPermission") // A permissão é concedida pelo launcher antes deste ponto.
    fun iniciar(): Boolean {
        if (gravando) return true
        val tamanho = maxOf(
            AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
            ),
            4_096,
        )
        val gravador = runCatching {
            AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                tamanho * 2,
            )
        }.getOrNull() ?: return false
        if (gravador.state != AudioRecord.STATE_INITIALIZED) {
            gravador.release()
            return false
        }

        val destino = MediaFiles.newAudio(context)
        return runCatching {
            FileOutputStream(destino).use { it.write(ByteArray(WAV_HEADER)) }
            gravador.startRecording()
            escrevendo = true
            audioRecord = gravador
            file = destino
            level = 0f
            trabalhador = thread(name = "Vocabu-wav", isDaemon = true) {
                FileOutputStream(destino, true).use { saida ->
                    val buffer = ByteArray(tamanho)
                    while (escrevendo) {
                        val lidos = gravador.read(buffer, 0, buffer.size)
                        if (lidos > 0) {
                            saida.write(buffer, 0, lidos)
                            level = picoDe(buffer, lidos)
                        }
                    }
                }
            }
            true
        }.getOrElse {
            escrevendo = false
            gravador.release()
            destino.delete()
            false
        }
    }

    fun parar(): File? {
        val gravador = audioRecord ?: return null
        val destino = file
        escrevendo = false
        runCatching { gravador.stop() }
        trabalhador?.join(2_000)
        gravador.release()
        limparEstado()

        if (destino == null || destino.length() <= WAV_HEADER) {
            destino?.delete()
            return null
        }
        return runCatching {
            escreverCabecalho(destino)
            destino
        }.getOrElse {
            destino.delete()
            null
        }
    }

    fun cancelar() {
        escrevendo = false
        audioRecord?.let { gravador ->
            runCatching { gravador.stop() }
            trabalhador?.join(1_000)
            gravador.release()
        }
        file?.delete()
        limparEstado()
    }

    private fun limparEstado() {
        audioRecord = null
        file = null
        trabalhador = null
        level = 0f
    }

    /**
     * O pico do bloco, amostrando de [PASSO_DA_AMOSTRAGEM] em [PASSO_DA_AMOSTRAGEM].
     *
     * Varrer as 2 mil amostras de cada bloco para achar um número que vira a
     * altura de uma barra é trabalho jogado fora: o pico de um bloco de 128 ms
     * sobrevive à amostragem, e o que se perde no meio do caminho é menor que a
     * espessura da barra desenhada.
     */
    private fun picoDe(buffer: ByteArray, lidos: Int): Float {
        var pico = 0
        var i = 0
        while (i + 1 < lidos) {
            val amostra = ((buffer[i + 1].toInt() shl 8) or (buffer[i].toInt() and 0xFF)).toShort().toInt()
            val modulo = if (amostra < 0) -amostra else amostra
            if (modulo > pico) pico = modulo
            i += SAMPLING_STEP * BYTES_PER_SAMPLE
        }
        return (pico / 32_767f).coerceIn(0f, 1f)
    }

    private fun escreverCabecalho(destino: File) {
        val dados = destino.length() - WAV_HEADER
        RandomAccessFile(destino, "rw").use { wav ->
            wav.seek(0)
            wav.writeBytes("RIFF")
            wav.writeIntLe((dados + 36).toInt())
            wav.writeBytes("WAVEfmt ")
            wav.writeIntLe(16)
            wav.writeShortLe(1)
            wav.writeShortLe(1)
            wav.writeIntLe(SAMPLE_RATE)
            wav.writeIntLe(SAMPLE_RATE * BYTES_PER_SAMPLE)
            wav.writeShortLe(BYTES_PER_SAMPLE)
            wav.writeShortLe(16)
            wav.writeBytes("data")
            wav.writeIntLe(dados.toInt())
        }
    }

    private fun RandomAccessFile.writeIntLe(value: Int) {
        write(value and 0xff)
        write(value ushr 8 and 0xff)
        write(value ushr 16 and 0xff)
        write(value ushr 24 and 0xff)
    }

    private fun RandomAccessFile.writeShortLe(value: Int) {
        write(value and 0xff)
        write(value ushr 8 and 0xff)
    }

    companion object {
        const val SAMPLE_RATE = 16_000
        const val WAV_HEADER = 44
        private const val BYTES_PER_SAMPLE = 2
        private const val SAMPLING_STEP = 8
    }
}
