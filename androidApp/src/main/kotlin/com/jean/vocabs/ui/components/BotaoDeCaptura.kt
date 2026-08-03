package com.jean.vocabs.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.gestures.detectTapGestures
import kotlinx.coroutines.withTimeoutOrNull

/**
 * O `+` central da barra — dois caminhos no mesmo alvo.
 *
 * **Tocar** abre a folha "Capturar", onde o idioma vem antes do modo e o último
 * usado já vem marcado: um toque a mais no caso comum, e a garantia de que nada
 * chega a Pendentes sem idioma.
 *
 * **Segurar** grava direto no idioma marcado, sem passar pela folha. É o caminho
 * de um gesto só, para o momento em que a frase está passando e não dá para
 * escolher nada — soltar encerra e guarda. Um toque acidental não vira áudio: o
 * gesto só começa a gravar depois do limiar de toque longo, e uma gravação mais
 * curta que [MINIMO_DE_GRAVACAO_MS] é descartada por quem recebe o áudio.
 *
 * O botão não mora dentro da [BarraInferior] porque `Surface` recorta o próprio
 * conteúdo, e tanto a sombra quanto o rótulo de gravação passam da borda de cima
 * da barra.
 */
@Composable
fun BotaoDeCaptura(
    gravando: Boolean,
    segundos: Long,
    aoTocar: () -> Unit,
    aoComecarAGravar: () -> Unit,
    aoTerminarDeGravar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cores = MaterialTheme.colorScheme
    var pressionado by remember { mutableStateOf(false) }

    val escala by animateFloatAsState(
        targetValue = if (gravando) 1.12f else if (pressionado) 0.9f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium),
        label = "escalaDoBotao",
    )
    val fundo by animateColorAsState(
        targetValue = if (gravando) cores.tertiary else cores.primary,
        animationSpec = tween(200),
        label = "fundoDoBotao",
    )
    // O `+` gira 45° e vira `×` quando a gravação começa: o mesmo alvo desfaz o
    // que ele fez, e o giro é o que liga uma coisa à outra.
    val giro by animateFloatAsState(
        targetValue = if (gravando) 45f else 0f,
        animationSpec = tween(220),
        label = "giroDoBotao",
    )

    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        if (gravando) PulsoDeGravacao(cores.tertiary)

        AnimatedVisibility(
            visible = gravando,
            enter = fadeIn(tween(160)) + scaleIn(initialScale = 0.8f),
            exit = fadeOut(tween(120)) + scaleOut(targetScale = 0.8f),
            modifier = Modifier.offset(y = (-46).dp),
        ) {
            Surface(shape = CircleShape, color = cores.tertiary, shadowElevation = 4.dp) {
                Text(
                    text = formatarDuracao(segundos),
                    style = MaterialTheme.typography.labelMedium,
                    color = cores.onTertiary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                )
            }
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .scale(escala)
                .size(52.dp)
                .shadow(6.dp, CircleShape)
                .background(fundo, CircleShape)
                .pointerInput(Unit) {
                    detectTapGestures(
                        // Precisa existir para que soltar depois do limiar não
                        // conte também como toque e abra a folha por cima da
                        // gravação que acabou de terminar.
                        onLongPress = {},
                        onPress = {
                            pressionado = true
                            val soltouCedo = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                                tryAwaitRelease()
                            }
                            if (soltouCedo == null) {
                                aoComecarAGravar()
                                tryAwaitRelease()
                                aoTerminarDeGravar()
                            }
                            pressionado = false
                        },
                        onTap = { aoTocar() },
                    )
                }
                .semantics {
                    contentDescription = if (gravando) "Gravando, solte para guardar" else "Capturar"
                },
        ) {
            Icon(
                imageVector = if (gravando) Icones.Microfone else Icones.Mais,
                contentDescription = null,
                tint = if (gravando) cores.onTertiary else cores.onPrimary,
                modifier = Modifier
                    .size(if (gravando) 24.dp else 27.dp)
                    .rotate(if (gravando) 0f else giro),
            )
        }
    }
}

/** O anel que respira enquanto grava — o único sinal de que o dedo ainda importa. */
@Composable
private fun PulsoDeGravacao(cor: androidx.compose.ui.graphics.Color) {
    val transicao = rememberInfiniteTransition(label = "pulso")
    val escala by transicao.animateFloat(
        initialValue = 1f,
        targetValue = 1.55f,
        animationSpec = infiniteRepeatable(tween(1_100), RepeatMode.Restart),
        label = "escalaDoPulso",
    )
    val opacidade by transicao.animateFloat(
        initialValue = 0.34f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1_100), RepeatMode.Restart),
        label = "opacidadeDoPulso",
    )
    Box(
        Modifier
            .scale(escala)
            .size(52.dp)
            .background(cor.copy(alpha = opacidade), CircleShape),
    )
}

/** Abaixo disto a gravação foi um deslize do dedo, e não uma captura. */
const val MINIMO_DE_GRAVACAO_MS = 700L
