package io.github.lucasshuan.vocabu.media

import io.github.lucasshuan.vocabu.R
import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.annotation.RequiresApi
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.io.FileInputStream
import kotlin.concurrent.thread
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

data class TranscriptionResult(val text: String? = null, val error: String? = null)

class PhotoTranscriber(private val context: Context) {
    suspend fun transcribe(path: String): TranscriptionResult = withContext(Dispatchers.IO) {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try {
            val image = InputImage.fromFilePath(context, Uri.fromFile(File(path)))
            suspendCancellableCoroutine { continuation ->
                recognizer.process(image)
                    .addOnSuccessListener { result ->
                        if (continuation.isActive) {
                            val text = result.text.trim().ifBlank { null }
                            continuation.resume(
                                if (text == null) TranscriptionResult(error = context.getString(R.string.transcribe_no_text_in_photo))
                                else TranscriptionResult(text = text),
                            )
                        }
                    }
                    .addOnFailureListener { failure ->
                        if (continuation.isActive) {
                            continuation.resume(
                                TranscriptionResult(error = failure.message ?: context.getString(R.string.transcribe_cannot_read_photo)),
                            )
                        }
                    }
            }
        } catch (failure: Exception) {
            TranscriptionResult(error = failure.message ?: context.getString(R.string.transcribe_cannot_read_photo))
        } finally {
            recognizer.close()
        }
    }
}

class AudioTranscriber(private val context: Context) {
    suspend fun transcribe(path: String): TranscriptionResult =
        withContext(Dispatchers.Main.immediate) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                return@withContext TranscriptionResult(
                    error = context.getString(R.string.transcribe_needs_android_13),
                )
            }
            if (!SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
                return@withContext TranscriptionResult(
                    error = context.getString(R.string.transcribe_no_voice_model),
                )
            }
            recognize(path)
        }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private suspend fun recognize(path: String): TranscriptionResult =
        suspendCancellableCoroutine { continuation ->
            val descriptors = ParcelFileDescriptor.createPipe()
            val reading = descriptors[0]
            val written = descriptors[1]
            val recognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
            var finished = false

            fun finish(result: TranscriptionResult) {
                if (finished) return
                finished = true
                runCatching { reading.close() }
                runCatching { written.close() }
                recognizer.destroy()
                if (continuation.isActive) continuation.resume(result)
            }

            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) = Unit
                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() = Unit
                override fun onPartialResults(partialResults: Bundle?) = Unit
                override fun onEvent(eventType: Int, params: Bundle?) = Unit

                override fun onError(error: Int) {
                    finish(TranscriptionResult(error = errorMessage(error)))
                }

                override fun onResults(results: Bundle?) {
                    val text = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        ?.trim()
                        ?.ifBlank { null }
                    finish(
                        if (text == null) TranscriptionResult(error = context.getString(R.string.transcribe_no_speech))
                        else TranscriptionResult(text = text),
                    )
                }
            })

            continuation.invokeOnCancellation {
                recognizer.cancel()
                runCatching { reading.close() }
                runCatching { written.close() }
                recognizer.destroy()
            }

            thread(name = "Vocabu-stt", isDaemon = true) {
                runCatching {
                    FileInputStream(path).use { entry ->
                        entry.skip(AudioRecorder.WAV_HEADER.toLong())
                        ParcelFileDescriptor.AutoCloseOutputStream(written).use { exit ->
                            entry.copyTo(exit)
                        }
                    }
                }.onFailure {
                    runCatching { written.close() }
                }
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE, reading)
                putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_CHANNEL_COUNT, 1)
                putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_ENCODING, AudioFormat.ENCODING_PCM_16BIT)
                putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_SAMPLING_RATE, AudioRecorder.SAMPLE_RATE)
            }
            recognizer.startListening(intent)
        }

    private fun errorMessage(code: Int): String = when (code) {
        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
        SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE ->
            context.getString(R.string.transcribe_no_voice_model)
        SpeechRecognizer.ERROR_NO_MATCH,
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> context.getString(R.string.transcribe_no_speech_manual)
        else -> context.getString(R.string.transcribe_failed, code)
    }
}
