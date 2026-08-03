package com.jean.vocabs.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.jean.vocabs.shared.domain.FormatoCaptura
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** Uma opção do leque: formato, ícone e o nome que aparece ao lado. */
data class OpcaoDeCaptura(
    val formato: FormatoCaptura,
    val icone: ImageVector,
    val rotulo: String,
)

/**
 * O botão central que se abre em leque.
 *
 * Três opções em arco, e não uma coluna: a coluna cresceria por cima da lista
 * atrás dela e a última opção acabaria perto do topo da tela, longe do polegar.
 * O arco mantém as três à mesma distância do botão — o mesmo esforço para
 * qualquer uma — e é o que dá a elas a diferença de posição que o olho usa para
 * distingui-las antes de ler os rótulos.
 *
 * A animação carrega informação em vez de enfeitar. O `+` vira `×` girando 45°,
 * então o mesmo alvo desfaz o que ele fez. As opções saem **escalonadas**, do
 * centro para fora, o que mostra de onde elas vieram; o escurecimento por trás
 * aparece junto para dizer que o resto da tela está suspenso. A mola tem quique
 * na abertura e o fechamento é curto e sem quique — desfazer não é um momento
 * para se demorar.
 */
@Composable
fun LequeDeCaptura(
    opcoes: List<OpcaoDeCaptura>,
    aberto: Boolean,
    aoAlternar: (Boolean) -> Unit,
    aoEscolher: (FormatoCaptura) -> Unit,
    modifier: Modifier = Modifier,
) {
    val progresso = remember { Animatable(0f) }

    LaunchedEffect(aberto) {
        if (aberto) {
            progresso.animateTo(1f, spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessLow))
        } else {
            progresso.animateTo(0f, tween(durationMillis = 160))
        }
    }

    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        opcoes.forEachIndexed { indice, opcao ->
            OpcaoEmArco(
                opcao = opcao,
                indice = indice,
                total = opcoes.size,
                progresso = progresso.value,
                aoClicar = {
                    // Fecha antes de navegar para que a volta não encontre o
                    // leque ainda aberto por cima da tela anterior.
                    aoAlternar(false)
                    aoEscolher(opcao.formato)
                },
            )
        }

        Surface(
            onClick = { aoAlternar(!aberto) },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            shadowElevation = 6.dp,
            modifier = Modifier.size(48.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icones.Mais,
                    contentDescription = if (aberto) "Fechar opções de captura" else "Capturar",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(26.dp).rotate(progresso.value * 45f),
                )
            }
        }
    }
}

/**
 * Uma opção, posicionada no arco conforme [progresso].
 *
 * O deslocamento é calculado, e não animado por opção: um `Animatable` por item
 * daria três animações capazes de divergir entre si, e o que precisa ficar óbvio
 * é que as três saem do mesmo lugar.
 */
@Composable
private fun OpcaoEmArco(
    opcao: OpcaoDeCaptura,
    indice: Int,
    total: Int,
    progresso: Float,
    aoClicar: () -> Unit,
) {
    val densidade = LocalDensity.current
    val raio = with(densidade) { RAIO_DO_ARCO.toPx() }

    // Escalonado: a primeira sai na frente, a última fica um pouco para trás. O
    // atraso é fração do progresso, e não tempo, para que o fechamento espelhe a
    // abertura sem uma segunda tabela de tempos.
    val atraso = indice * ATRASO_POR_OPCAO
    val meu = ((progresso - atraso) / (1f - atraso)).coerceIn(0f, 1f)

    // Da esquerda para a direita, sempre acima do botão, com 270° apontando para
    // cima. O arco é estreito de propósito: aberto demais, os discos das pontas
    // ficam quase na horizontal e o rótulo de cada um cai dentro da barra.
    val angulo = Math.toRadians((ANGULO_INICIAL + (ANGULO_FINAL - ANGULO_INICIAL) * indice / (total - 1).coerceAtLeast(1)).toDouble())
    val x = (cos(angulo) * raio * meu).toFloat()
    val y = (sin(angulo) * raio * meu).toFloat()

    if (meu <= 0f) return

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
            .alpha(meu)
            .scale(0.6f + 0.4f * meu),
    ) {
        Surface(
            onClick = aoClicar,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            border = contornoDeCartao(),
            shadowElevation = 4.dp,
            modifier = Modifier.size(TAMANHO_DO_DISCO).semantics { contentDescription = opcao.rotulo },
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = opcao.icone,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        // O rótulo fica sob o disco, fora do alvo de toque: dentro dele, o texto
        // roubaria espaço do ícone e os três discos precisariam crescer. E vem
        // numa pílula opaca porque o véu escurece pouco — texto solto sobre um
        // fundo claro qualquer fica na sorte de o que está atrás ser liso.
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            border = contornoDeCartao(),
            modifier = Modifier.offset(y = DISTANCIA_DO_ROTULO),
        ) {
            Text(
                text = opcao.rotulo,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            )
        }
    }
}

/**
 * O escurecimento por trás do leque.
 *
 * Fica fora de [LequeDeCaptura] porque precisa cobrir a tela inteira, e o leque
 * mora dentro da barra de baixo — um véu desenhado ali seria recortado pela
 * própria barra. Ele também é o alvo de fechar: tocar em qualquer lugar desfaz,
 * que é o gesto que as pessoas tentam antes de procurar o botão.
 */
@Composable
fun VeuDoLeque(aberto: Boolean, aoFechar: () -> Unit, modifier: Modifier = Modifier) {
    val opacidade = remember { Animatable(0f) }
    LaunchedEffect(aberto) {
        if (aberto) opacidade.animateTo(1f, tween(220)) else opacidade.animateTo(0f, tween(160))
    }
    if (opacidade.value <= 0f) return

    val interacao = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f * opacidade.value))
            // Sem indicação: o véu não é um botão, é a área que desfaz.
            .clickable(interactionSource = interacao, indication = null, onClick = aoFechar),
    )
}

private val TAMANHO_DO_DISCO = 56.dp
private val DISTANCIA_DO_ROTULO = 40.dp

/**
 * Raio e abertura do arco, calibrados contra a barra.
 *
 * O centro do botão fica a 34 dp do fim da barra, então tudo abaixo de 34 dp de
 * altura invade a fileira de ícones. Com 130 dp de raio e o arco de 230° a 310°,
 * o disco mais baixo sobe 100 dp e o rótulo dele ainda sobra 30 dp acima da
 * borda — folga suficiente para a fonte ampliada do sistema.
 */
private val RAIO_DO_ARCO = 130.dp
private const val ANGULO_INICIAL = 230f
private const val ANGULO_FINAL = 310f

/** Fração do progresso que cada opção espera antes de começar a sair. */
private const val ATRASO_POR_OPCAO = 0.12f
