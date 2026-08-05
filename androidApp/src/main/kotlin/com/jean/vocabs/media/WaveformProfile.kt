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
fun FloatArray.picoDaBarra(index: Int, barras: Int): Float {
    if (isEmpty() || barras <= 0) return 0f
    val start = (index.toLong() * size / barras).toInt().coerceIn(0, size - 1)
    val end = ((index + 1).toLong() * size / barras).toInt().coerceIn(start + 1, size)
    var pico = 0f
    for (i in start until end) pico = max(pico, this[i])
    return pico
}

private fun wavProfile(path: String): FloatArray {
    val file = File(path)
    if (!file.isFile || file.length() <= AudioRecorder.WAV_HEADER) return FloatArray(0)

    return runCatching {
        file.inputStream().buffered().use { entry ->
            val fluxo = DataInputStream(entry)
            val riff = ByteArray(12)
            fluxo.readFully(riff)
            if (mark(riff, 0) != "RIFF" || mark(riff, 8) != "WAVE") return@use FloatArray(0)

            // Percorre os blocos até o de dados: o cabeçalho de 44 bytes é o que
            // o nosso gravador escreve, mas um WAV de fora pode trazer outros.
            var bits = 0
            var canais = 0
            val topo = ByteArray(8)
            repeat(MAX_BLOCKS) {
                fluxo.readFully(topo)
                val id = mark(topo, 0)
                val tamanho = readInt(topo, 4)
                if (id == "data") return@use peaksOf(fluxo, tamanho, bits, canais)
                if (tamanho !in 0..MAX_BLOCK) return@use FloatArray(0)
                val body = ByteArray(tamanho + (tamanho and 1))
                fluxo.readFully(body)
                if (id == "fmt " && body.size >= 16) {
                    canais = readShort(body, 2)
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
private fun peaksOf(fluxo: DataInputStream, bytesDeDados: Int, bits: Int, canais: Int): FloatArray {
    if (bits != 16 || canais !in 1..2 || bytesDeDados <= 0) return FloatArray(0)

    val bytesPorQuadro = 2 * canais
    val quadros = bytesDeDados / bytesPorQuadro
    if (quadros <= 0) return FloatArray(0)

    val perfil = FloatArray(PROFILE_POINTS)
    val passo = max(1, quadros / (PROFILE_POINTS * SAMPLES_PER_POINT))
    val buffer = ByteArray(16 * 1_024)
    var restante = bytesDeDados
    var quadro = 0

    // O `runCatching` cobre o arquivo que acaba antes do tamanho declarado no
    // cabeçalho: uma gravação interrompida no meio ainda tem onda até onde foi.
    runCatching {
        while (restante >= bytesPorQuadro) {
            val pedaco = minOf(buffer.size, restante) / bytesPorQuadro * bytesPorQuadro
            fluxo.readFully(buffer, 0, pedaco)
            restante -= pedaco
            var i = 0
            while (i + 1 < pedaco) {
                val amostra = ((buffer[i + 1].toInt() shl 8) or (buffer[i].toInt() and 0xFF)).toShort().toInt()
                val modulo = (if (amostra < 0) -amostra else amostra) / 32_767f
                val ponto = (quadro.toLong() * PROFILE_POINTS / quadros).toInt().coerceIn(0, PROFILE_POINTS - 1)
                if (modulo > perfil[ponto]) perfil[ponto] = modulo
                quadro += passo
                i += passo * bytesPorQuadro
            }
        }
    }

    val maior = perfil.max()
    if (maior <= 0f) return FloatArray(0)
    val escala = 1f / max(maior, PEAK_FLOOR)
    for (i in perfil.indices) perfil[i] = (perfil[i] * escala).coerceAtMost(1f).pow(PERCEPTUAL_CURVE)
    return perfil
}

/** Abaixo disto a gravação é sussurro, e sussurro desenha baixo mesmo. */
private const val PEAK_FLOOR = 0.22f

/** A fala normal vive na parte de baixo da escala linear; a raiz a traz para cima. */
private const val PERCEPTUAL_CURVE = 0.55f

private fun mark(bytes: ByteArray, posicao: Int) = String(bytes, posicao, 4, Charsets.US_ASCII)

private fun readInt(bytes: ByteArray, posicao: Int): Int =
    (bytes[posicao].toInt() and 0xFF) or
        ((bytes[posicao + 1].toInt() and 0xFF) shl 8) or
        ((bytes[posicao + 2].toInt() and 0xFF) shl 16) or
        ((bytes[posicao + 3].toInt() and 0xFF) shl 24)

private fun readShort(bytes: ByteArray, posicao: Int): Int =
    (bytes[posicao].toInt() and 0xFF) or ((bytes[posicao + 1].toInt() and 0xFF) shl 8)
