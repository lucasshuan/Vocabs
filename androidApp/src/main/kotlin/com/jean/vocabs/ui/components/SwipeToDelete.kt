package com.jean.vocabs.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlinx.coroutines.launch

/**
 * O limiar do descarte, como fração da largura do cartão.
 *
 * Um terço é o que separa "empurrei sem querer enquanto rolava a lista" de
 * "decidi jogar isto fora". Abaixo disso o cartão volta sozinho; acima, soltar
 * exclui.
 */
private const val THRESHOLD_FRACTION = 0.32f

/**
 * O piso do limiar, para cartões estreitos.
 *
 * A fração sozinha faria um cartão pequeno ser descartado por um arrasto de dois
 * centímetros — perto demais do deslize acidental.
 */
private val MIN_THRESHOLD = 96.dp

/**
 * O arremesso: a partir daqui a velocidade decide no lugar da distância.
 *
 * Quem joga o cartão para fora com um golpe rápido não espera que ele volte só
 * porque o dedo saiu da tela antes do terço. Exige metade do limiar percorrida
 * para que um roçar veloz na diagonal não exclua nada.
 */
private const val DISMISS_VELOCITY = 1_100f
private const val MIN_FLING_FRACTION = 0.5f

/**
 * O quanto o cartão pesa depois de passar do limiar.
 *
 * Ele não trava — travar faz o gesto parecer quebrado —, mas anda menos que o
 * dedo. É o sinal físico de que dali para a frente não falta mais nada: a
 * decisão já está tomada e o que resta é soltar.
 */
private const val RESISTANCE_PAST_THRESHOLD = 0.42f

/**
 * Arrastar o cartão para o lado para excluir o que está nele.
 *
 * O gesto é destrutivo, e por isso é o único do app construído inteiro em torno
 * de **avisar antes**. Três coisas acontecem em ordem, e cada uma existe para que
 * ninguém exclua nada sem ter visto que ia excluir:
 *
 * 1. O vermelho aparece **atrás** do cartão desde o primeiro milímetro, com a
 *    lixeira. O gesto se explica sozinho antes de qualquer decisão ser tomada.
 * 2. Passado [FRACAO_DO_LIMIAR], o fundo troca do vermelho suave para o vermelho
 *    cheio, a lixeira cresce, a frase "Solte para excluir" entra e o aparelho dá
 *    um toque. É o momento em que o gesto deixa de ser reversível pelo próprio
 *    dedo, e ele é anunciado por cor, texto e tato ao mesmo tempo.
 * 3. Voltar atrás é sempre possível **sem soltar**: arrastar de volta desarma
 *    tudo, e soltar antes do limiar devolve o cartão ao lugar com mola.
 *
 * Vale para os dois lados de propósito. Fixar uma direção só faria metade das
 * pessoas descobrir que o gesto existe, e não há segunda ação disputando o outro
 * lado — se um dia houver, esta é a hora de dividir.
 *
 * O que acontece **depois** de [aoExcluir] não é problema daqui: o cartão sai
 * voando e quem chama decide se aquilo vira uma exclusão imediata ou uma que
 * ainda pode ser desfeita. Em Pendentes é a segunda.
 */
@Composable
fun SwipeToDelete(
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
    armedLabel: String = "Solte para excluir",
    actionLabel: String = "Excluir",
    content: @Composable () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val thresholdFloor = with(LocalDensity.current) { MIN_THRESHOLD.toPx() }

    val offset = remember { Animatable(0f) }
    var width by remember { mutableIntStateOf(0) }
    // Depois que o cartão parte para fora da tela o gesto acabou: nenhum toque
    // novo o traz de volta, e `aoExcluir` não pode ser chamado duas vezes.
    var discarding by remember { mutableStateOf(false) }

    val threshold = if (width == 0) thresholdFloor else maxOf(width * THRESHOLD_FRACTION, thresholdFloor)

    // `derivedStateOf` porque o deslocamento muda a cada quadro e isto aqui só
    // muda duas vezes por gesto: sem ele, arrastar um cartão recomporia a linha
    // inteira sessenta vezes por segundo.
    val armed by remember(threshold) { derivedStateOf { abs(offset.value) >= threshold } }
    val toLeft by remember { derivedStateOf { offset.value <= 0f } }

    // Só na entrada. Um segundo pulso ao desarmar transformaria vaivém em
    // vibração contínua — a mesma regra que o leque de captura já segue.
    LaunchedEffect(armed) {
        if (armed) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    val background by animateColorAsState(
        targetValue = if (armed) colors.error else colors.errorContainer,
        animationSpec = tween(Motion.FAST),
        label = "fundoDoDescarte",
    )
    val tinta by animateColorAsState(
        targetValue = if (armed) colors.onError else colors.error,
        animationSpec = tween(Motion.FAST),
        label = "tintaDoDescarte",
    )
    val trashScale by animateFloatAsState(
        targetValue = if (armed) 1.15f else 0.92f,
        animationSpec = Motion.elasticSpring(),
        label = "escalaDaLixeira",
    )
    val labelOpacity by animateFloatAsState(
        targetValue = if (armed) 1f else 0f,
        animationSpec = tween(Motion.FAST),
        label = "opacidadeDoRotulo",
    )

    val drag = rememberDraggableState { step ->
        val current = offset.value
        val advancing = current == 0f || sign(step) == sign(current)
        val walked = if (advancing && abs(current) >= threshold) step * RESISTANCE_PAST_THRESHOLD else step
        scope.launch { offset.snapTo(current + walked) }
    }

    Box(
        modifier = modifier
            .onSizeChanged { width = it.width }
            .draggable(
                state = drag,
                orientation = Orientation.Horizontal,
                enabled = !discarding,
                onDragStopped = { speed ->
                    val walked = offset.value
                    val fling = abs(speed) > DISMISS_VELOCITY &&
                        sign(speed) == sign(walked) &&
                        abs(walked) >= threshold * MIN_FLING_FRACTION
                    if (abs(walked) >= threshold || fling) {
                        discarding = true
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        // Sai pela borda antes de a lista fechar o buraco: a
                        // pessoa vê para onde o cartão foi, e só então os de
                        // baixo sobem. As duas coisas ao mesmo tempo viram um
                        // piscar de tela em que nada é legível.
                        offset.animateTo(
                            targetValue = sign(walked) * (width.toFloat() + thresholdFloor),
                            animationSpec = tween(Motion.FAST, easing = FastOutLinearInEasing),
                        )
                        onDelete()
                    } else {
                        offset.animateTo(0f, Motion.standardSpring())
                    }
                },
            ),
    ) {
        // O fundo é decoração do gesto: para o leitor de tela ele não existe, e
        // a exclusão chega como ação do próprio cartão, logo abaixo.
        Box(
            modifier = Modifier
                .matchParentSize()
                .clearAndSetSemantics {}
                .clip(shape)
                .background(background),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .align(if (toLeft) Alignment.CenterEnd else Alignment.CenterStart)
                    .padding(horizontal = 18.dp),
            ) {
                // A lixeira fica sempre encostada na borda que o cartão
                // descobriu, e o rótulo cresce para dentro. Invertido, o rótulo
                // invisível empurraria a lixeira para debaixo do cartão e o
                // início do gesto não mostraria nada.
                if (toLeft) DiscardLabel(armedLabel, tinta, labelOpacity)
                Icon(
                    imageVector = AppIcons.Trash,
                    contentDescription = null,
                    tint = tinta,
                    modifier = Modifier
                        .graphicsLayer { scaleX = trashScale; scaleY = trashScale }
                        .size(24.dp),
                )
                if (!toLeft) DiscardLabel(armedLabel, tinta, labelOpacity)
            }
        }

        Box(
            modifier = Modifier
                // Lido dentro da lambda: mover o cartão custa uma passada de
                // layout por quadro, e nenhuma recomposição.
                .offset { IntOffset(offset.value.roundToInt(), 0) }
                // Quem navega por leitor de tela não tem como arrastar nada. A
                // exclusão vira uma ação do cartão, com o mesmo nome que o
                // gesto mostra escrito.
                .semantics(mergeDescendants = true) {
                    customActions = listOf(CustomAccessibilityAction(actionLabel) { onDelete(); true })
                },
        ) {
            content()
        }
    }
}

@Composable
private fun DiscardLabel(text: String, color: androidx.compose.ui.graphics.Color, opacity: Float) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Clip,
        modifier = Modifier.graphicsLayer { alpha = opacity },
    )
}
