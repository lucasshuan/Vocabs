package io.github.lucasshuan.vocabu.media

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import io.github.lucasshuan.vocabu.shared.media.MediaFiles
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import kotlin.concurrent.thread

/** 16kHz mono PCM in a WAV — what the local `SpeechRecognizer` accepts. */
class AudioRecorder(private val context: Context) {

    private var audioRecord: AudioRecord? = null
    private var file: File? = null
    private var worker: Thread? = null

    @Volatile
    private var writing = false

    /**
     * The last block's peak, 0 to 1 — the wave is real, because one that moves
     * the same through silence and speech answers nothing.
     *
     * Volatile: the WAV thread writes and the drawing thread reads. A torn read
     * costs one crooked frame.
     */
    @Volatile
    var level: Float = 0f
        private set

    val isRecording: Boolean get() = writing

    @Suppress("MissingPermission") // A permissão é granted pelo launcher antes deste point.
    fun begin(): Boolean {
        if (isRecording) return true
        val size = maxOf(
            AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
            ),
            4_096,
        )
        val recorder = runCatching {
            AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                size * 2,
            )
        }.getOrNull() ?: return false
        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            return false
        }

        val destination = MediaFiles.newAudio(context)
        return runCatching {
            FileOutputStream(destination).use { it.write(ByteArray(WAV_HEADER)) }
            recorder.startRecording()
            writing = true
            audioRecord = recorder
            file = destination
            level = 0f
            worker = thread(name = "Vocabu-wav", isDaemon = true) {
                FileOutputStream(destination, true).use { exit ->
                    val buffer = ByteArray(size)
                    while (writing) {
                        val seen = recorder.read(buffer, 0, buffer.size)
                        if (seen > 0) {
                            exit.write(buffer, 0, seen)
                            level = peakOf(buffer, seen)
                        }
                    }
                }
            }
            true
        }.getOrElse {
            writing = false
            recorder.release()
            destination.delete()
            false
        }
    }

    fun stop(): File? {
        val recorder = audioRecord ?: return null
        val destination = file
        writing = false
        runCatching { recorder.stop() }
        worker?.join(2_000)
        recorder.release()
        clearState()

        if (destination == null || destination.length() <= WAV_HEADER) {
            destination?.delete()
            return null
        }
        return runCatching {
            writeHeader(destination)
            destination
        }.getOrElse {
            destination.delete()
            null
        }
    }

    fun cancel() {
        writing = false
        audioRecord?.let { recorder ->
            runCatching { recorder.stop() }
            worker?.join(1_000)
            recorder.release()
        }
        file?.delete()
        clearState()
    }

    private fun clearState() {
        audioRecord = null
        file = null
        worker = null
        level = 0f
    }

    /**
     * Sampled, not scanned: a 128ms block's peak survives it, and what is lost
     * is thinner than the bar drawn from it.
     */
    private fun peakOf(buffer: ByteArray, seen: Int): Float {
        var peak = 0
        var i = 0
        while (i + 1 < seen) {
            val sample = ((buffer[i + 1].toInt() shl 8) or (buffer[i].toInt() and 0xFF)).toShort().toInt()
            val modulus = if (sample < 0) -sample else sample
            if (modulus > peak) peak = modulus
            i += SAMPLING_STEP * BYTES_PER_SAMPLE
        }
        return (peak / 32_767f).coerceIn(0f, 1f)
    }

    private fun writeHeader(destination: File) {
        val data = destination.length() - WAV_HEADER
        RandomAccessFile(destination, "rw").use { wav ->
            wav.seek(0)
            wav.writeBytes("RIFF")
            wav.writeIntLe((data + 36).toInt())
            wav.writeBytes("WAVEfmt ")
            wav.writeIntLe(16)
            wav.writeShortLe(1)
            wav.writeShortLe(1)
            wav.writeIntLe(SAMPLE_RATE)
            wav.writeIntLe(SAMPLE_RATE * BYTES_PER_SAMPLE)
            wav.writeShortLe(BYTES_PER_SAMPLE)
            wav.writeShortLe(16)
            wav.writeBytes("data")
            wav.writeIntLe(data.toInt())
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
