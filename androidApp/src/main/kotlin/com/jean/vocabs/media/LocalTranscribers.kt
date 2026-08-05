package com.jean.vocabs.media

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
    suspend fun transcrever(path: String): TranscriptionResult = withContext(Dispatchers.IO) {
        val reconhecedor = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try {
            val imagem = InputImage.fromFilePath(context, Uri.fromFile(File(path)))
            suspendCancellableCoroutine { continuacao ->
                reconhecedor.process(imagem)
                    .addOnSuccessListener { result ->
                        if (continuacao.isActive) {
                            val text = result.text.trim().ifBlank { null }
                            continuacao.resume(
                                if (text == null) TranscriptionResult(error = "Nenhum texto foi encontrado na foto.")
                                else TranscriptionResult(text = text),
                            )
                        }
                    }
                    .addOnFailureListener { falha ->
                        if (continuacao.isActive) {
                            continuacao.resume(
                                TranscriptionResult(error = falha.message ?: "Não foi possível ler a foto."),
                            )
                        }
                    }
            }
        } catch (falha: Exception) {
            TranscriptionResult(error = falha.message ?: "Não foi possível ler a foto.")
        } finally {
            reconhecedor.close()
        }
    }
}

class AudioTranscriber(private val context: Context) {
    suspend fun transcrever(path: String): TranscriptionResult =
        withContext(Dispatchers.Main.immediate) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                return@withContext TranscriptionResult(
                    error = "A transcrição automática de arquivos exige Android 13. Digite o trecho manualmente.",
                )
            }
            if (!SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
                return@withContext TranscriptionResult(
                    error = "O modelo de voz local em inglês não está disponível. Digite o trecho manualmente.",
                )
            }
            reconhecer(path)
        }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private suspend fun reconhecer(path: String): TranscriptionResult =
        suspendCancellableCoroutine { continuacao ->
            val descritores = ParcelFileDescriptor.createPipe()
            val leitura = descritores[0]
            val escrita = descritores[1]
            val reconhecedor = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
            var terminou = false

            fun finish(result: TranscriptionResult) {
                if (terminou) return
                terminou = true
                runCatching { leitura.close() }
                runCatching { escrita.close() }
                reconhecedor.destroy()
                if (continuacao.isActive) continuacao.resume(result)
            }

            reconhecedor.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) = Unit
                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() = Unit
                override fun onPartialResults(partialResults: Bundle?) = Unit
                override fun onEvent(eventType: Int, params: Bundle?) = Unit

                override fun onError(error: Int) {
                    finish(TranscriptionResult(error = mensagemDoErro(error)))
                }

                override fun onResults(results: Bundle?) {
                    val text = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        ?.trim()
                        ?.ifBlank { null }
                    finish(
                        if (text == null) TranscriptionResult(error = "Nenhuma fala foi reconhecida.")
                        else TranscriptionResult(text = text),
                    )
                }
            })

            continuacao.invokeOnCancellation {
                reconhecedor.cancel()
                runCatching { leitura.close() }
                runCatching { escrita.close() }
                reconhecedor.destroy()
            }

            thread(name = "Vocabu-stt", isDaemon = true) {
                runCatching {
                    FileInputStream(path).use { entry ->
                        entry.skip(AudioRecorder.WAV_HEADER.toLong())
                        ParcelFileDescriptor.AutoCloseOutputStream(escrita).use { saida ->
                            entry.copyTo(saida)
                        }
                    }
                }.onFailure {
                    runCatching { escrita.close() }
                }
            }

            val intencao = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE, leitura)
                putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_CHANNEL_COUNT, 1)
                putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_ENCODING, AudioFormat.ENCODING_PCM_16BIT)
                putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_SAMPLING_RATE, AudioRecorder.SAMPLE_RATE)
            }
            reconhecedor.startListening(intencao)
        }

    private fun mensagemDoErro(codigo: Int): String = when (codigo) {
        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
        SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE ->
            "O modelo de voz em inglês não está disponível. Digite o trecho manualmente."
        SpeechRecognizer.ERROR_NO_MATCH,
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Nenhuma fala foi reconhecida. Digite o trecho manualmente."
        else -> "A transcrição local falhou (código $codigo). Digite o trecho manualmente."
    }
}
