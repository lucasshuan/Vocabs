package com.jean.vocabs.media

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.jean.vocabs.shared.media.ArquivosDeMidia
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import kotlin.concurrent.thread

/** Grava PCM 16 kHz mono dentro de WAV, formato aceito pelo reconhecimento local. */
class GravadorDeAudio(private val context: Context) {

    private var audioRecord: AudioRecord? = null
    private var arquivo: File? = null
    private var trabalhador: Thread? = null

    @Volatile
    private var escrevendo = false

    val gravando: Boolean get() = escrevendo

    @Suppress("MissingPermission") // A permissão é concedida pelo launcher antes deste ponto.
    fun iniciar(): Boolean {
        if (gravando) return true
        val tamanho = maxOf(
            AudioRecord.getMinBufferSize(
                TAXA_AMOSTRAGEM,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
            ),
            4_096,
        )
        val gravador = runCatching {
            AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                TAXA_AMOSTRAGEM,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                tamanho * 2,
            )
        }.getOrNull() ?: return false
        if (gravador.state != AudioRecord.STATE_INITIALIZED) {
            gravador.release()
            return false
        }

        val destino = ArquivosDeMidia.novoAudio(context)
        return runCatching {
            FileOutputStream(destino).use { it.write(ByteArray(CABECALHO_WAV)) }
            gravador.startRecording()
            escrevendo = true
            audioRecord = gravador
            arquivo = destino
            trabalhador = thread(name = "tagarara-wav", isDaemon = true) {
                FileOutputStream(destino, true).use { saida ->
                    val buffer = ByteArray(tamanho)
                    while (escrevendo) {
                        val lidos = gravador.read(buffer, 0, buffer.size)
                        if (lidos > 0) saida.write(buffer, 0, lidos)
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
        val destino = arquivo
        escrevendo = false
        runCatching { gravador.stop() }
        trabalhador?.join(2_000)
        gravador.release()
        limparEstado()

        if (destino == null || destino.length() <= CABECALHO_WAV) {
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
        arquivo?.delete()
        limparEstado()
    }

    private fun limparEstado() {
        audioRecord = null
        arquivo = null
        trabalhador = null
    }

    private fun escreverCabecalho(destino: File) {
        val dados = destino.length() - CABECALHO_WAV
        RandomAccessFile(destino, "rw").use { wav ->
            wav.seek(0)
            wav.writeBytes("RIFF")
            wav.writeIntLe((dados + 36).toInt())
            wav.writeBytes("WAVEfmt ")
            wav.writeIntLe(16)
            wav.writeShortLe(1)
            wav.writeShortLe(1)
            wav.writeIntLe(TAXA_AMOSTRAGEM)
            wav.writeIntLe(TAXA_AMOSTRAGEM * BYTES_POR_AMOSTRA)
            wav.writeShortLe(BYTES_POR_AMOSTRA)
            wav.writeShortLe(16)
            wav.writeBytes("data")
            wav.writeIntLe(dados.toInt())
        }
    }

    private fun RandomAccessFile.writeIntLe(valor: Int) {
        write(valor and 0xff)
        write(valor ushr 8 and 0xff)
        write(valor ushr 16 and 0xff)
        write(valor ushr 24 and 0xff)
    }

    private fun RandomAccessFile.writeShortLe(valor: Int) {
        write(valor and 0xff)
        write(valor ushr 8 and 0xff)
    }

    companion object {
        const val TAXA_AMOSTRAGEM = 16_000
        const val CABECALHO_WAV = 44
        private const val BYTES_POR_AMOSTRA = 2
    }
}
