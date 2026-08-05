package com.jean.vocabs.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

data class Tab(val route: String, val icon: ImageVector, val label: String, val badge: Int = 0)

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
fun BottomBar(
    leftTabs: List<Tab>,
    rightTabs: List<Tab>,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
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
                    .height(BAR_HEIGHT)
                    .padding(horizontal = 8.dp),
            ) {
                leftTabs.forEach { TabItem(it, currentRoute == it.route, { onNavigate(it.route) }, Modifier.weight(1f)) }
                Spacer(Modifier.weight(1f))
                rightTabs.forEach { TabItem(it, currentRoute == it.route, { onNavigate(it.route) }, Modifier.weight(1f)) }
            }
        }
    }
}

/** A altura da fileira de ícones, sem os insets. O botão de captura se alinha por ela. */
val BAR_HEIGHT = 68.dp

/**
 * Um dos quatro lugares.
 *
 * Sem rótulo escrito, o ícone é a única coisa que responde ao toque — e uma troca
 * seca de cor num desenho de 23 dp é quase invisível no canto do olho. O ícone da
 * aba que acaba de ser aberta cresce 12% e volta, com a mola de [Movimento]: é o
 * mesmo "cedeu e voltou" dos cartões, ao contrário, e é o que confirma o toque
 * antes mesmo de a tela nova ter desenhado.
 */
@Composable
private fun TabItem(tab: Tab, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val tinta by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(Motion.DEFAULT),
        label = "tintaDaAba",
    )
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.12f else 1f,
        animationSpec = Motion.elasticSpring(),
        label = "escalaDaAba",
    )
    val toque = rememberHaptics()

    Surface(
        onClick = onClick,
        color = Color.Transparent,
        shape = CircleShape,
        interactionSource = toque,
        modifier = modifier.height(56.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(Modifier.graphicsLayer { scaleX = scale; scaleY = scale }) {
                Icon(
                    imageVector = tab.icon,
                    contentDescription = tab.label,
                    tint = tinta,
                    modifier = Modifier.size(23.dp),
                )
                // O selo entra com mola e sai encolhendo: a fila de Pendentes
                // muda sozinha, ao fundo, enquanto a pessoa está noutra aba, e
                // um número que aparece do nada no canto do ícone não se lê como
                // "chegou algo" — se lê como um defeito de desenho.
                AnimatedVisibility(
                    visible = tab.badge > 0,
                    enter = scaleIn(Motion.elasticSpring()) + fadeIn(tween(Motion.FAST)),
                    exit = scaleOut(tween(Motion.FAST)) + fadeOut(tween(Motion.FAST)),
                    modifier = Modifier.align(Alignment.TopEnd),
                ) {
                    Badge { Text(tab.badge.coerceAtMost(99).toString()) }
                }
            }
        }
    }
}
