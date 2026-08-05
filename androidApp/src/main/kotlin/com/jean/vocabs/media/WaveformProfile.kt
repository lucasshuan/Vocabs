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

/** How many points the profile keeps — always more bars than the screen draws. */
private const val PROFILE_POINTS = 180

/** How many samples each point looks at before calling the peak good. */
private const val SAMPLES_PER_POINT = 48

/** The largest accepted size for a chunk that is not the data chunk. */
private const val MAX_BLOCK = 1_000_000

/** A real WAV has two or three chunks before the data; this is the guard. */
private const val MAX_BLOCKS = 12

/**
 * The shape of the stored speech, read from the file itself.
 *
 * The recorder writes 16-bit PCM, so the peak comes from scanning the samples and
 * keeping the largest magnitude of each slice.
 *
 * Returns an empty array while reading and when the file is not a 16-bit PCM we
 * can read. In both cases the screen draws the calm line, which is honest — it
 * invents no relief that was never measured.
 */
@Composable
fun rememberWaveformProfile(path: String): State<FloatArray> =
    produceState(initialValue = FloatArray(0), path) {
        value = withContext(Dispatchers.IO) { wavProfile(path) }
    }

/**
 * The peak of the profile slice that fits this bar.
 *
 * The profile has fixed resolution and the bar count comes from screen width, so
 * a bar takes the **largest** point of its slice rather than the first: reducing
 * by sampling would erase exactly the transients that give the relief.
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

            // Walks the chunks to the data one: the 44-byte header is what our
            // recorder writes, but a foreign WAV may carry others.
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
 * The peaks, one per slice, already normalized.
 *
 * The step keeps cost from following duration: a three-minute memo has nearly
 * three million samples, and forty-eight per slice already gives the same
 * drawing. The floor in the normalization is what keeps a whisper from becoming a
 * shout — without it, dividing by its own maximum would push every recording,
 * loud or quiet, to the ceiling.
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

    // `runCatching` covers a file that ends before the size declared in the
    // header: a recording interrupted midway still has a wave up to where it got.
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
