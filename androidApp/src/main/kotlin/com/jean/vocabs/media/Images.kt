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
fun rememberPhoto(path: String, larguraAlvo: Int = 1080): State<ImageBitmap?> =
    produceState<ImageBitmap?>(initialValue = null, path, larguraAlvo) {
        value = withContext(Dispatchers.IO) { decode(path, larguraAlvo) }
    }

private fun decode(path: String, larguraAlvo: Int): ImageBitmap? {
    val medidas = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, medidas)
    if (medidas.outWidth <= 0) return null

    var amostra = 1
    while (medidas.outWidth / amostra > larguraAlvo) {
        amostra *= 2
    }

    val opcoes = BitmapFactory.Options().apply { inSampleSize = amostra }
    return BitmapFactory.decodeFile(path, opcoes)?.asImageBitmap()
}
