package io.github.lucasshuan.vocabu.media

import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Loads the capture's photo already downsampled.
 *
 * Phone cameras return multi-megapixel images; putting one whole into a 1080px
 * screen would spend tens of MB of heap per photo. `inSampleSize` resolves it at
 * decode time, with no extra resize pass.
 *
 * No image library: it is one local photo at a time, and Coil or Glide would
 * bring more configuration than the problem asks for.
 */
@Composable
fun rememberPhoto(path: String, targetWidth: Int = 1080): State<ImageBitmap?> =
    produceState<ImageBitmap?>(initialValue = null, path, targetWidth) {
        value = withContext(Dispatchers.IO) { decode(path, targetWidth) }
    }

private fun decode(path: String, targetWidth: Int): ImageBitmap? {
    val measures = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, measures)
    if (measures.outWidth <= 0) return null

    var sample = 1
    while (measures.outWidth / sample > targetWidth) {
        sample *= 2
    }

    val options = BitmapFactory.Options().apply { inSampleSize = sample }
    return BitmapFactory.decodeFile(path, options)?.asImageBitmap()
}
