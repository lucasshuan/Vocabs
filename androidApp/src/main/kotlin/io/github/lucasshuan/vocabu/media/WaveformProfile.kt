package io.github.lucasshuan.vocabu.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import java.io.DataInputStream
import java.io.File
import kotlin.math.max
import kotlin.math.pow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Always more points than the screen has bars. */
private const val PROFILE_POINTS = 180

private const val SAMPLES_PER_POINT = 48

private const val MAX_BLOCK = 1_000_000

/** A real WAV has two or three chunks before the data. */
private const val MAX_BLOCKS = 12

/**
 * Empty while reading, and for anything that is not readable 16-bit PCM. Both
 * draw the calm line rather than relief that was never measured.
 */
@Composable
fun rememberWaveformProfile(path: String): State<FloatArray> =
    produceState(initialValue = FloatArray(0), path) {
        value = withContext(Dispatchers.IO) { wavProfile(path) }
    }

/**
 * The largest point of the slice, not the first: the profile's resolution is
 * fixed and the bar count is not, and sampling erases the transients.
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

            // Walks to the data chunk: our recorder writes a 44-byte header, but
            // a foreign WAV may carry more.
            var bits = 0
            var channels = 0
            val header = ByteArray(8)
            repeat(MAX_BLOCKS) {
                flow.readFully(header)
                val id = mark(header, 0)
                val size = readInt(header, 4)
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
 * The step keeps cost off duration: a three-minute memo holds nearly three
 * million samples and draws the same at forty-eight per slice.
 *
 * [PEAK_FLOOR] keeps a whisper from becoming a shout — normalising by each
 * recording's own maximum pushes every one of them to the ceiling.
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

    // Covers a file ending before the header's declared size: a recording cut
    // short still draws up to where it got.
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

/** Below this the recording is a whisper, and a whisper should draw low. */
private const val PEAK_FLOOR = 0.22f

/** Normal speech lives at the bottom of the linear scale; the root lifts it. */
private const val PERCEPTUAL_CURVE = 0.55f

private fun mark(bytes: ByteArray, position: Int) = String(bytes, position, 4, Charsets.US_ASCII)

private fun readInt(bytes: ByteArray, position: Int): Int =
    (bytes[position].toInt() and 0xFF) or
        ((bytes[position + 1].toInt() and 0xFF) shl 8) or
        ((bytes[position + 2].toInt() and 0xFF) shl 16) or
        ((bytes[position + 3].toInt() and 0xFF) shl 24)

private fun readShort(bytes: ByteArray, position: Int): Int =
    (bytes[position].toInt() and 0xFF) or ((bytes[position + 1].toInt() and 0xFF) shl 8)
