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
 * Downsampled at decode time via `inSampleSize`: a multi-megapixel camera photo
 * on a 1080px screen costs tens of MB of heap, and this needs no resize pass.
 *
 * No Coil or Glide — one local photo at a time.
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
