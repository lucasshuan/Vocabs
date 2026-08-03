package com.jean.vocabs.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import com.jean.vocabs.shared.domain.FormatoCaptura

data class Aba(val rota: String, val icone: ImageVector, val rotulo: String, val selo: Int = 0)

/**
 * Cinco lugares, só ícones.
 *
 * O handoff tira os rótulos de baixo dos ícones de propósito: quatro palavras
 * competindo com o botão de captura tiram dele a única coisa que ele precisa ter
 * — ser o alvo óbvio. O nome continua existindo como `contentDescription`, que é
 * o que o leitor de tela lê.
 */
/**
 * A barra cuida das quatro abas e deixa o meio vago.
 *
 * O botão de captura **não** mora aqui, e a razão é técnica antes de ser de
 * desenho: `Surface` recorta o próprio conteúdo, e o leque precisa desenhar
 * acima da borda de cima da barra. Quem o compõe é a camada de cima, com a mesma
 * geometria — daí [ALTURA_DA_BARRA] ser público.
 */
@Composable
fun BarraInferior(
    abasEsquerda: List<Aba>,
    abasDireita: List<Aba>,
    rotaAtual: String?,
    aoNavegar: (String) -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .height(ALTURA_DA_BARRA)
                    .padding(horizontal = 8.dp),
            ) {
                abasEsquerda.forEach { ItemAba(it, rotaAtual == it.rota, { aoNavegar(it.rota) }, Modifier.weight(1f)) }
                Spacer(Modifier.weight(1f))
                abasDireita.forEach { ItemAba(it, rotaAtual == it.rota, { aoNavegar(it.rota) }, Modifier.weight(1f)) }
            }
        }
    }
}

/** A altura da fileira de ícones, sem os insets. O leque se alinha por ela. */
val ALTURA_DA_BARRA = 68.dp

/**
 * A ordem do leque é a mesma das abas da Captura.
 *
 * Texto primeiro porque é o caminho mais usado e o único que termina sem sair da
 * tela; foto e áudio existem para capturar em segundos e resolver depois.
 */
val OPCOES_DE_CAPTURA = listOf(
    OpcaoDeCaptura(FormatoCaptura.TEXTO, Icones.Lapis, "Texto"),
    OpcaoDeCaptura(FormatoCaptura.AUDIO, Icones.Microfone, "Áudio"),
    OpcaoDeCaptura(FormatoCaptura.FOTO, Icones.Camera, "Foto"),
)

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
