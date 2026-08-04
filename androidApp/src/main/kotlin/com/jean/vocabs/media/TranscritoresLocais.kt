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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

data class ResultadoTranscricao(val texto: String? = null, val erro: String? = null)

class TranscritorDeFoto(private val context: Context) {
    suspend fun transcrever(caminho: String): ResultadoTranscricao = withContext(Dispatchers.IO) {
        val reconhecedor = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try {
            val imagem = InputImage.fromFilePath(context, Uri.fromFile(File(caminho)))
            suspendCancellableCoroutine { continuacao ->
                reconhecedor.process(imagem)
                    .addOnSuccessListener { resultado ->
                        if (continuacao.isActive) {
                            val texto = resultado.text.trim().ifBlank { null }
                            continuacao.resume(
                                if (texto == null) ResultadoTranscricao(erro = "Nenhum texto foi encontrado na foto.")
                                else ResultadoTranscricao(texto = texto),
                            )
                        }
                    }
                    .addOnFailureListener { falha ->
                        if (continuacao.isActive) {
                            continuacao.resume(
                                ResultadoTranscricao(erro = falha.message ?: "Não foi possível ler a foto."),
                            )
                        }
                    }
            }
        } catch (falha: Exception) {
            ResultadoTranscricao(erro = falha.message ?: "Não foi possível ler a foto.")
        } finally {
            reconhecedor.close()
        }
    }
}

class TranscritorDeAudio(private val context: Context) {
    suspend fun transcrever(caminho: String): ResultadoTranscricao =
        withContext(Dispatchers.Main.immediate) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                return@withContext ResultadoTranscricao(
                    erro = "A transcrição automática de arquivos exige Android 13. Digite o trecho manualmente.",
                )
            }
            if (!SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
                return@withContext ResultadoTranscricao(
                    erro = "O modelo de voz local em inglês não está disponível. Digite o trecho manualmente.",
                )
            }
            reconhecer(caminho)
        }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private suspend fun reconhecer(caminho: String): ResultadoTranscricao =
        suspendCancellableCoroutine { continuacao ->
            val descritores = ParcelFileDescriptor.createPipe()
            val leitura = descritores[0]
            val escrita = descritores[1]
            val reconhecedor = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
            var terminou = false

            fun concluir(resultado: ResultadoTranscricao) {
                if (terminou) return
                terminou = true
                runCatching { leitura.close() }
                runCatching { escrita.close() }
                reconhecedor.destroy()
                if (continuacao.isActive) continuacao.resume(resultado)
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
                    concluir(ResultadoTranscricao(erro = mensagemDoErro(error)))
                }

                override fun onResults(results: Bundle?) {
                    val texto = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        ?.trim()
                        ?.ifBlank { null }
                    concluir(
                        if (texto == null) ResultadoTranscricao(erro = "Nenhuma fala foi reconhecida.")
                        else ResultadoTranscricao(texto = texto),
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
                    FileInputStream(caminho).use { entrada ->
                        entrada.skip(GravadorDeAudio.CABECALHO_WAV.toLong())
                        ParcelFileDescriptor.AutoCloseOutputStream(escrita).use { saida ->
                            entrada.copyTo(saida)
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
                putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_SAMPLING_RATE, GravadorDeAudio.TAXA_AMOSTRAGEM)
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
