package com.jean.vocabs.media

import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Carrega a foto da captura já reduzida.
 *
 * A câmera do celular devolve imagens de vários megapixels; jogar isso inteiro
 * numa tela de 1080px de largura gastaria dezenas de MB de heap por foto. O
 * `inSampleSize` resolve na decodificação, sem passo extra de redimensionamento.
 *
 * Sem biblioteca de imagem: é uma foto local por vez, e Coil/Glide trariam mais
 * configuração do que o problema pede.
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
