package com.jean.vocabs.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

data class Aba(val rota: String, val icone: ImageVector, val rotulo: String, val selo: Int = 0)

@Composable
fun BarraInferior(
    abasEsquerda: List<Aba>,
    abasDireita: List<Aba>,
    rotaAtual: String?,
    aoNavegar: (String) -> Unit,
    aoCapturar: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 10.dp,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround,
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().height(72.dp).padding(horizontal = 6.dp),
        ) {
            abasEsquerda.forEach { ItemAba(it, rotaAtual == it.rota, { aoNavegar(it.rota) }, Modifier.weight(1f)) }
            Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f)) {
                Surface(onClick = aoCapturar, shape = CircleShape, color = MaterialTheme.colorScheme.primary, shadowElevation = 6.dp, modifier = Modifier.size(58.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icones.Mais, "Capturar", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(26.dp))
                    }
                }
            }
            abasDireita.forEach { ItemAba(it, rotaAtual == it.rota, { aoNavegar(it.rota) }, Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun ItemAba(aba: Aba, selecionada: Boolean, aoClicar: () -> Unit, modifier: Modifier) {
    Surface(onClick = aoClicar, color = androidx.compose.ui.graphics.Color.Transparent, shape = MaterialTheme.shapes.medium, modifier = modifier.height(60.dp)) {
        androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Box {
                Icon(aba.icone, aba.rotulo, tint = if (selecionada) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(23.dp))
                if (aba.selo > 0) Badge(Modifier.align(Alignment.TopEnd)) { Text(aba.selo.coerceAtMost(99).toString()) }
            }
            Text(aba.rotulo, style = MaterialTheme.typography.labelSmall, color = if (selecionada) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 3.dp))
        }
    }
}
