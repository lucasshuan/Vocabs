package com.jean.vocabs.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import java.io.DataInputStream
import java.io.File
import kotlin.math.max
import kotlin.math.pow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Quantos pontos o perfil guarda — sempre mais barras do que a tela desenha. */
private const val PROFILE_POINTS = 180

/** Quantas amostras cada ponto olha antes de dar o pico por bom. */
private const val SAMPLES_PER_POINT = 48

/** O maior tamanho aceito para um bloco que não é o de dados. */
private const val MAX_BLOCK = 1_000_000

/** Um WAV são de verdade tem dois ou três blocos antes dos dados; isto é a guarda. */
private const val MAX_BLOCKS = 12

/**
 * O desenho da fala guardada, lido do próprio arquivo.
 *
 * A onda anterior era uma lista de dez alturas fixas repetida ao longo da
 * largura: o mesmo serrote em toda captura, e nenhuma relação com o que foi
 * dito. Aqui o pico sai do WAV — o gravador grava PCM 16 bits, então basta
 * varrer as amostras e guardar o maior módulo de cada fatia.
 *
 * Devolve vetor vazio enquanto lê e também quando o arquivo não é um PCM de 16
 * bits que a gente saiba ler; nos dois casos a tela desenha a linha calma, que
 * é honesta — não inventa relevo que não foi medido.
 */
@Composable
fun rememberWaveformProfile(path: String): State<FloatArray> =
    produceState(initialValue = FloatArray(0), path) {
        value = withContext(Dispatchers.IO) { wavProfile(path) }
    }

/**
 * O pico da fatia do perfil que cabe nesta barra.
 *
 * O perfil tem resolução fixa e a quantidade de barras vem da largura da tela,
 * então a barra pega o **maior** ponto do pedaço que lhe cabe, e não o primeiro:
 * reduzir por amostragem apagaria justamente os estalos que dão o relevo.
 */
fun FloatArray.picoDaBarra(index: Int, bars: Int): Float {
    if (isEmpty() || bars <= 0) return 0f
    val start = (index.toLong() * size / bars).toInt().coerceIn(0, size - 1)
    val end = ((index + 1).toLong() * size / bars).toInt().coerceIn(start + 1, size)
    var peak = 0f
    for (i in start until end) peak = max(peak, this[i])
    return peak
}

private fun wavProfile(path: String): FloatArray {
    val file = File(path)
    if (!file.isFile || file.length() <= AudioRecorder.WAV_HEADER) return FloatArray(0)

    return runCatching {
        file.inputStream().buffered().use { entry ->
            val flow = DataInputStream(entry)
            val riff = ByteArray(12)
            flow.readFully(riff)
            if (mark(riff, 0) != "RIFF" || mark(riff, 8) != "WAVE") return@use FloatArray(0)

            // Percorre os blocos até o de dados: o cabeçalho de 44 bytes é o que
            // o nosso gravador escreve, mas um WAV de fora pode trazer outros.
            var bits = 0
            var channels = 0
            val topo = ByteArray(8)
            repeat(MAX_BLOCKS) {
                flow.readFully(topo)
                val id = mark(topo, 0)
                val size = readInt(topo, 4)
                if (id == "data") return@use peaksOf(flow, size, bits, channels)
                if (size !in 0..MAX_BLOCK) return@use FloatArray(0)
                val body = ByteArray(size + (size and 1))
                flow.readFully(body)
                if (id == "fmt " && body.size >= 16) {
                    channels = readShort(body, 2)
                    bits = readShort(body, 14)
                }
            }
            FloatArray(0)
        }
    }.getOrDefault(FloatArray(0))
}

/**
 * Os picos, um por fatia, já normalizados.
 *
 * O passo existe para o custo não acompanhar a duração: um memo de três minutos
 * tem quase três milhões de amostras, e olhar quarenta e oito por fatia já
 * entrega o mesmo desenho. O piso na normalização é o que impede um sussurro de
 * virar grito — sem ele, dividir pelo próprio máximo faria toda gravação, alta
 * ou baixa, encostar no teto.
 */
private fun peaksOf(flow: DataInputStream, dataBytes: Int, bits: Int, channels: Int): FloatArray {
    if (bits != 16 || channels !in 1..2 || dataBytes <= 0) return FloatArray(0)

    val bytesPerFrame = 2 * channels
    val frames = dataBytes / bytesPerFrame
    if (frames <= 0) return FloatArray(0)

    val profile = FloatArray(PROFILE_POINTS)
    val step = max(1, frames / (PROFILE_POINTS * SAMPLES_PER_POINT))
    val buffer = ByteArray(16 * 1_024)
    var remaining = dataBytes
    var frame = 0

    // O `runCatching` cobre o arquivo que acaba antes do tamanho declarado no
    // cabeçalho: uma gravação interrompida no meio ainda tem onda até onde foi.
    runCatching {
        while (remaining >= bytesPerFrame) {
            val chunk = minOf(buffer.size, remaining) / bytesPerFrame * bytesPerFrame
            flow.readFully(buffer, 0, chunk)
            remaining -= chunk
            var i = 0
            while (i + 1 < chunk) {
                val sample = ((buffer[i + 1].toInt() shl 8) or (buffer[i].toInt() and 0xFF)).toShort().toInt()
                val modulus = (if (sample < 0) -sample else sample) / 32_767f
                val point = (frame.toLong() * PROFILE_POINTS / frames).toInt().coerceIn(0, PROFILE_POINTS - 1)
                if (modulus > profile[point]) profile[point] = modulus
                frame += step
                i += step * bytesPerFrame
            }
        }
    }

    val largest = profile.max()
    if (largest <= 0f) return FloatArray(0)
    val scale = 1f / max(largest, PEAK_FLOOR)
    for (i in profile.indices) profile[i] = (profile[i] * scale).coerceAtMost(1f).pow(PERCEPTUAL_CURVE)
    return profile
}

/** Abaixo disto a gravação é sussurro, e sussurro desenha baixo mesmo. */
private const val PEAK_FLOOR = 0.22f

/** A fala normal vive na parte de baixo da escala linear; a raiz a traz para cima. */
private const val PERCEPTUAL_CURVE = 0.55f

private fun mark(bytes: ByteArray, position: Int) = String(bytes, position, 4, Charsets.US_ASCII)

private fun readInt(bytes: ByteArray, position: Int): Int =
    (bytes[position].toInt() and 0xFF) or
        ((bytes[position + 1].toInt() and 0xFF) shl 8) or
        ((bytes[position + 2].toInt() and 0xFF) shl 16) or
        ((bytes[position + 3].toInt() and 0xFF) shl 24)

private fun readShort(bytes: ByteArray, position: Int): Int =
    (bytes[position].toInt() and 0xFF) or ((bytes[position + 1].toInt() and 0xFF) shl 8)
