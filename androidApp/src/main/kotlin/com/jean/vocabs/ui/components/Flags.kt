package com.jean.vocabs.ui.components

import com.jean.vocabs.ui.displayName
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jean.vocabs.contracts.Language
import com.jean.vocabs.ui.languages.flagOf

/**
 * A bandeira como disco.
 *
 * O recorte é do Compose e não do desenho: o `clip` tem antialiasing de verdade,
 * enquanto um `clip-path` dentro do VectorDrawable serrilha a borda em algumas
 * versões do Android. O desenho em si é a arte original, quadrada, e o círculo
 * some com o que sobra.
 *
 * A pílula do par de idiomas que morava aqui saiu junto com o cabeçalho antigo
 * do Início: o par deixou de ser uma coisa só. O alvo agora é a página do
 * carrossel e o nativo é uma linha da tela Você — desenhá-los grudados sugeriria
 * que os dois se trocam no mesmo lugar, e não se trocam mais.
 */
@Composable
fun CircularFlag(language: Language, modifier: Modifier = Modifier, tamanho: Dp = 20.dp) {
    Image(
        painter = painterResource(flagOf(language)),
        contentDescription = language.displayName,
        contentScale = ContentScale.Crop,
        modifier = modifier.size(tamanho).clip(CircleShape),
    )
}
