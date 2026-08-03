package com.jean.vocabs.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jean.vocabs.contracts.Idioma
import com.jean.vocabs.contracts.Idiomas
import com.jean.vocabs.ui.idiomas.bandeiraDe

/**
 * A bandeira como disco.
 *
 * O recorte é do Compose e não do desenho: o `clip` tem antialiasing de verdade,
 * enquanto um `clip-path` dentro do VectorDrawable serrilha a borda em algumas
 * versões do Android. O desenho em si é a arte original, quadrada, e o círculo
 * some com o que sobra.
 */
@Composable
fun BandeiraCircular(idioma: Idioma, modifier: Modifier = Modifier, tamanho: Dp = 20.dp) {
    Image(
        painter = painterResource(bandeiraDe(idioma)),
        contentDescription = idioma.nome,
        contentScale = ContentScale.Crop,
        modifier = modifier.size(tamanho).clip(CircleShape),
    )
}

/**
 * O par de idiomas das fichas: em que língua se lê, que língua se aprende.
 *
 * Aparece como pílula no cabeçalho do Início e como conteúdo de linha na tela
 * Você. Os dois idiomas vêm de fora porque agora eles mudam — quem os conhece é
 * o ViewModel, que lê a preferência.
 */
@Composable
fun ParDeIdiomas(
    nativo: Idioma,
    alvo: Idioma,
    modifier: Modifier = Modifier,
    comNomes: Boolean = false,
    tamanhoDaBandeira: Dp = 20.dp,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        modifier = modifier,
    ) {
        BandeiraCircular(nativo, tamanho = tamanhoDaBandeira)
        if (comNomes) Text(nativo.nome, style = MaterialTheme.typography.titleSmall)
        Text("→", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        BandeiraCircular(alvo, tamanho = tamanhoDaBandeira)
        if (comNomes) Text(alvo.nome, style = MaterialTheme.typography.titleSmall)
    }
}

/** A pílula do cabeçalho do Início: o par mais a chevron de "dá para trocar". */
@Composable
fun PilulaDeIdiomas(
    aoClicar: () -> Unit,
    modifier: Modifier = Modifier,
    nativo: Idioma = Idiomas.PORTUGUES,
    alvo: Idioma = Idiomas.INGLES,
) {
    Surface(
        onClick = aoClicar,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        border = contornoDeCartao(),
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 8.dp, end = 10.dp, top = 6.dp, bottom = 6.dp),
        ) {
            ParDeIdiomas(nativo = nativo, alvo = alvo)
            ChevronDeLinha()
        }
    }
}
