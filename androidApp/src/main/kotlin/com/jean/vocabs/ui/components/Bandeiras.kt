package com.jean.vocabs.ui.components

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jean.vocabs.ui.idiomas.IdiomaUi
import com.jean.vocabs.ui.idiomas.Idiomas

/**
 * As bandeiras desenhadas, e não em emoji.
 *
 * O handoff mostra discos recortados; o emoji de indicador regional vira um
 * retângulo (ou dois quadradinhos com as letras, em aparelho sem a fonte) e some
 * dentro da pílula. Desenhar as duas que existem custa menos que depender da
 * fonte do sistema para um elemento de marca.
 */
@Composable
fun BandeiraCircular(idioma: IdiomaUi, modifier: Modifier = Modifier, tamanho: Dp = 20.dp) {
    Canvas(
        modifier = modifier
            .size(tamanho)
            .clip(CircleShape)
            .semantics { contentDescription = idioma.nome },
    ) {
        when (idioma.codigo) {
            Idiomas.PORTUGUES.codigo -> {
                val meio = size.width / 2f
                drawRect(Color(0xFF00923F))
                drawPath(
                    path = Path().apply {
                        moveTo(meio, size.height * 0.16f)
                        lineTo(size.width * 0.87f, meio)
                        lineTo(meio, size.height * 0.84f)
                        lineTo(size.width * 0.13f, meio)
                        close()
                    },
                    color = Color(0xFFFFDF00),
                )
                drawCircle(Color(0xFF0A2C8E), radius = size.width * 0.17f)
            }
            Idiomas.INGLES.codigo -> {
                drawRect(Color(0xFFF5F5F5))
                val faixa = size.height / 13f
                repeat(7) { indice ->
                    drawRect(
                        color = Color(0xFFC4243C),
                        topLeft = Offset(0f, faixa * 2 * indice),
                        size = Size(size.width, faixa),
                    )
                }
                drawRect(
                    color = Color(0xFF2A3B78),
                    size = Size(size.width * 0.45f, faixa * 5),
                )
            }
            else -> drawRect(Color(0xFF9A8FA8))
        }
    }
}

/**
 * O par de idiomas das fichas, como pílula (Início) ou como conteúdo de linha
 * (Perfil). Hoje só há um par; o componente já lê de [Idiomas] para que ligar a
 * escolha de verdade seja mexer numa lista, não em duas telas.
 */
@Composable
fun ParDeIdiomas(
    modifier: Modifier = Modifier,
    nativo: IdiomaUi = Idiomas.nativoAtual,
    alvo: IdiomaUi = Idiomas.alvoAtual,
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
fun PilulaDeIdiomas(aoClicar: () -> Unit, modifier: Modifier = Modifier) {
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
            ParDeIdiomas()
            ChevronDeLinha()
        }
    }
}
