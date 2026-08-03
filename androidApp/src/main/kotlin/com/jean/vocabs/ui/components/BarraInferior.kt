package com.jean.vocabs.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Badge
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

data class Aba(val rota: String, val icone: ImageVector, val rotulo: String, val selo: Int = 0)

/**
 * Cinco lugares, só ícones.
 *
 * O handoff tira os rótulos de baixo dos ícones de propósito: quatro palavras
 * competindo com o botão de captura tiram dele a única coisa que ele precisa ter
 * — ser o alvo óbvio. O nome continua existindo como `contentDescription`, que é
 * o que o leitor de tela lê.
 */
@Composable
fun BarraInferior(
    abasEsquerda: List<Aba>,
    abasDireita: List<Aba>,
    rotaAtual: String?,
    aoNavegar: (String) -> Unit,
    aoCapturar: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround,
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().height(68.dp).padding(horizontal = 8.dp),
            ) {
                abasEsquerda.forEach { ItemAba(it, rotaAtual == it.rota, { aoNavegar(it.rota) }, Modifier.weight(1f)) }
                Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f)) {
                    Surface(
                        onClick = aoCapturar,
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        shadowElevation = 6.dp,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icones.Mais, "Capturar", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(26.dp))
                        }
                    }
                }
                abasDireita.forEach { ItemAba(it, rotaAtual == it.rota, { aoNavegar(it.rota) }, Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun ItemAba(aba: Aba, selecionada: Boolean, aoClicar: () -> Unit, modifier: Modifier) {
    Surface(onClick = aoClicar, color = Color.Transparent, shape = CircleShape, modifier = modifier.height(56.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Box {
                Icon(
                    imageVector = aba.icone,
                    contentDescription = aba.rotulo,
                    tint = if (selecionada) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(23.dp),
                )
                if (aba.selo > 0) {
                    Badge(Modifier.align(Alignment.TopEnd)) { Text(aba.selo.coerceAtMost(99).toString()) }
                }
            }
        }
    }
}
