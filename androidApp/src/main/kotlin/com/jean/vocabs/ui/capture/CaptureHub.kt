package com.jean.vocabs.ui.capture

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.jean.vocabs.contracts.Language
import com.jean.vocabs.shared.domain.CaptureFormat
import com.jean.vocabs.ui.components.BAR_HEIGHT
import com.jean.vocabs.ui.components.AppIcons
import com.jean.vocabs.ui.components.Motion
import com.jean.vocabs.ui.components.formatColors
import com.jean.vocabs.ui.components.formatIcon
import com.jean.vocabs.ui.components.formatLabel
import com.jean.vocabs.ui.components.reducedMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

/**
 * O `+` e tudo que sai dele.
 *
 * O handoff troca dois toques por um gesto: **arrastar do `+`**. Segurar abre um
 * leque de três alvos, o dedo alcança um e soltar executa.
 *
 * Uma regra só governa os três alvos: **soltar é a única confirmação**. Enquanto o
 * dedo se move nada foi escolhido e nada foi iniciado — nem gravação, nem câmera,
 * nem campo de texto. Os três nascem iguais e neutros, no tom claro da própria
 * ação, e cada um ganha a cor cheia apenas quando o dedo chega nele. Foi o áudio
 * quem mais mudou: ele deixou de ser o alvo grande e verde que já estava gravando
 * quando o dedo passou por cima. Um alvo que dispara ao ser tocado transforma o
 * caminho até os outros dois num campo minado.
 *
 * Três decisões sustentam o fluxo, e as três estão aqui:
 *
 * 1. **O idioma não é perguntado antes.** Sumiu a folha "Capturar" com a fileira
 *    de bandeiras: a captura vai para o curso aberto no hub, e a correção mora em
 *    Pendentes, onde o erro é visível e desfazer custa um toque. Perguntar antes
 *    cobra a pergunta em toda captura para acertar as poucas em que a resposta
 *    não era a óbvia.
 * 2. **A captura entra em Pendentes no instante em que termina**, antes de
 *    qualquer decisão. Transcrição e ficha correm em segundo plano.
 * 3. **Selecionar agora é atalho, nunca etapa** — o que o aviso de 5 s oferece.
 *
 * O hub ocupa a tela inteira porque o leque e o véu precisam desenhar por cima da
 * barra e do conteúdo. Fora de um gesto nada aqui intercepta toque além do próprio
 * botão: um `Box` sem `pointerInput` não consome nada, e a barra de baixo
 * continua clicável através dele. A gravação, essa sim, tem tela própria —
 * [TelaDeGravacao] —, e é ela quem toma a tela quando o áudio começa.
 */
@Composable
fun CaptureHub(
    capture: QuickCapture,
    language: Language,
    onOpenText: () -> Unit,
    onPhaseChange: (inGesture: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val reduced = reducedMotion()
    val openText by rememberUpdatedState(onOpenText)

    var activePair by remember { mutableStateOf(false) }
    var target by remember { mutableStateOf<GestureTarget>(GestureTarget.Origin) }
    val isRecording = capture.isRecording

    // "O hub tomou a tela": o que está atrás desfoca e sai do alcance do leitor de
    // tela. Vale para o leque e para a gravação — a tela de gravação é opaca, mas
    // o conteúdo continua composto por baixo dela, e um `+` alcançável pelo
    // TalkBack debaixo de uma tela que já não é aquela é uma armadilha.
    val inGesture = activePair || isRecording
    LaunchedEffect(inGesture) { onPhaseChange(inGesture) }

    // Rede de segurança: o hub sai de cena quando uma tela cheia abre, e sair de
    // cena cancela a corrotina do gesto sem passar por `concluir`. Sem isto, uma
    // gravação apanhada por uma navegação continuaria correndo com o microfone
    // aberto e sem nada na tela dizendo isso.
    DisposableEffect(Unit) {
        onDispose {
            if (capture.isRecording) capture.cancelAudio()
            onPhaseChange(false)
        }
    }

    // Leque e gravação continuam compostos por um instante depois de fechar, para
    // a saída poder ser animada. Sem isto os alvos desapareceriam no quadro em que
    // o dedo sobe, e o gesto terminaria num corte seco.
    val drawingFan = stillOnScreen(activePair)
    val drawingRecording = stillOnScreen(isRecording)

    // O véu do leque deixa o conteúdo legível por trás: a pessoa está decidindo, e
    // o app é o contexto da decisão.
    val veil = animateFloatAsState(
        targetValue = if (activePair) 0.46f else 0f,
        animationSpec = tween(Motion.FAST, easing = FastOutSlowInEasing),
        label = "veu",
    )

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .drawBehind { if (veil.value > 0.001f) drawRect(NIGHT, alpha = veil.value) },
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .fillMaxWidth()
                .height(ANCHOR_HEIGHT)
                // O `+` fica debaixo da tela de gravação, que é opaca. Deixá-lo
                // no alcance do leitor de tela ofereceria "Capturar" no meio de
                // uma gravação em curso.
                .then(if (isRecording) Modifier.clearAndSetSemantics {} else Modifier)
                // A âncora é alta para caber o leque inteiro sem apertar as
                // medidas de ninguém, e desce até o centro dela coincidir com o
                // centro do botão. Daí em diante todo alvo é um `offset` puro a
                // partir do `+`, que é como o handoff descreve as posições.
                .offset(y = ANCHOR_HEIGHT / 2 - BAR_HEIGHT / 2 - BUTTON_ELEVATION),
        ) {
            if (drawingFan) {
                GuideToTarget(target, activePair)
                CaptureFormat.entries.forEach { format ->
                    FanTarget(
                        format = format,
                        marked = target == GestureTarget.Mode(format),
                        isVisible = activePair,
                        delay = ENTER_DELAY.getValue(format),
                        reduced = reduced,
                    )
                }
                GestureHint(target, activePair)
            }

            HubButton(
                activePair = activePair,
                isRecording = isRecording,
                reduced = reduced,
                modifier = Modifier
                    .offset(y = -BUTTON_ELEVATION)
                    .semantics {
                        contentDescription = "Capturar"
                        // O arrasto não tem equivalente para quem navega por
                        // leitor de tela: as três saídas do leque viram ações do
                        // próprio botão, e a gravação aberta por aqui é encerrada
                        // pelas ações da tela de gravação.
                        customActions = listOf(
                            CustomAccessibilityAction("Escrever") { openText(); true },
                            CustomAccessibilityAction("Fotografar") { capture.takePhoto(); true },
                            CustomAccessibilityAction("Gravar áudio") {
                                if (capture.hasAudioPermission) capture.recordAudio()
                                else capture.requestAudioPermission()
                                true
                            },
                        )
                    }
                    .pointerInput(Unit) {
                        val targets = targetsInPixels()
                        val targetRadiusPx = TARGET_RADIUS.toPx()
                        val originRadiusPx = ORIGIN_RADIUS.toPx()
                        val slack = viewConfiguration.touchSlop

                        awaitEachGesture {
                            val first = awaitFirstDown(requireUnconsumed = false)
                            first.consume()
                            val centro = Offset(size.width / 2f, size.height / 2f)

                            // Até 180 ms parado ainda é toque; a partir daí, ou
                            // a partir de um dedo que andou, é pressão e o leque
                            // abre. O limiar do sistema (500 ms) é longo demais
                            // para um gesto que precisa render antes de a frase
                            // terminar de passar.
                            val releasedEarly = withTimeoutOrNull(FAN_OPEN_MS) {
                                var loose: PointerInputChange? = null
                                while (true) {
                                    val change = nextChange(first) ?: break
                                    if (!change.pressed) {
                                        loose = change
                                        break
                                    }
                                    if ((change.position - first.position).getDistance() > slack) break
                                }
                                loose
                            }

                            if (releasedEarly != null) {
                                openText()
                                return@awaitEachGesture
                            }

                            activePair = true
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                            var current: GestureTarget = GestureTarget.Origin
                            var released = false

                            while (true) {
                                val change = nextChange(first) ?: break
                                val new = targetFor(
                                    shift = change.position - centro,
                                    targets = targets,
                                    targetRadiusPx = targetRadiusPx,
                                    originRadiusPx = originRadiusPx,
                                )

                                if (new != current) {
                                    current = new
                                    target = new
                                    // Chegar num alvo tem toque tátil; sair dele
                                    // não. O que a mão precisa sentir é o encaixe,
                                    // e um segundo pulso na saída transformaria
                                    // atravessar o leque numa vibração contínua.
                                    if (new is GestureTarget.Mode) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                }

                                if (!change.pressed) {
                                    released = true
                                    break
                                }
                            }

                            activePair = false
                            target = GestureTarget.Origin
                            finish(capture, current, released, haptic, openText)
                        }
                    },
            )
        }

        // Por último no `Box`, portanto por cima de tudo — inclusive do `+`, que
        // some atrás dela em vez de continuar clicável debaixo de uma tela opaca.
        if (drawingRecording) {
            RecordingScreen(
                capture = capture,
                language = language,
                isRecording = isRecording,
            )
        }
    }
}

/**
 * Se algo que já saiu ainda precisa estar composto para terminar de sair.
 *
 * Entra no mesmo quadro em que [ativo] vira verdadeiro e só desaparece depois de
 * a animação de saída ter tido tempo de correr.
 */
@Composable
private fun stillOnScreen(ativo: Boolean, leftover: Long = Motion.DEFAULT.toLong() + 80): Boolean {
    var presente by remember { mutableStateOf(ativo) }
    LaunchedEffect(ativo) {
        if (ativo) {
            presente = true
        } else {
            delay(leftover)
            presente = false
        }
    }
    return presente || ativo
}

/**
 * O próximo estado deste mesmo dedo, ou nulo quando ele deixou de existir.
 *
 * Todo evento é consumido: o hub fica por cima de listas roláveis, e um arrasto
 * para cima que vazasse para o conteúdo rolaria a tela debaixo do leque.
 */
private suspend fun androidx.compose.ui.input.pointer.AwaitPointerEventScope.nextChange(
    source: PointerInputChange,
): PointerInputChange? {
    val event = awaitPointerEvent()
    val change = event.changes.firstOrNull { it.id == source.id } ?: return null
    change.consume()
    return change
}

/**
 * O que soltar o dedo significa — e é o único lugar do gesto onde alguma coisa
 * acontece.
 *
 * [soltou] separa o dedo que subiu do gesto que o sistema cancelou — outro
 * ponteiro, a tela apagando, o app indo para trás. Um cancelamento tratado como
 * soltura abriria a câmera sozinho no meio de uma ligação.
 */
private fun finish(
    capture: QuickCapture,
    target: GestureTarget,
    released: Boolean,
    haptic: HapticFeedback,
    openText: () -> Unit,
) {
    if (!released) return
    when (target) {
        is GestureTarget.Mode -> {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            when (target.format) {
                CaptureFormat.TEXT -> openText()
                CaptureFormat.PHOTO -> capture.takePhoto()
                CaptureFormat.AUDIO ->
                    if (capture.hasAudioPermission) capture.recordAudio()
                    else capture.requestAudioPermission()
            }
        }
        // O leque abriu por tempo e o dedo nunca saiu do `+`. Isso é um toque
        // devagar, não uma desistência: quem só encostou no botão fica com o
        // caminho do texto em vez de com um gesto que não fez nada.
        GestureTarget.Origin -> openText()
        // Soltar fora fecha sem capturar nada e sem aviso.
        GestureTarget.Outward -> Unit
    }
}

/** Cor única do véu e do fundo da gravação: ameixa quase preta, nos dois temas. */
internal val NIGHT = Color(0xFF17111F)

/** O quanto o botão sobe em relação ao centro da fileira de ícones da barra. */
private val BUTTON_ELEVATION = 10.dp

/**
 * O diâmetro do `+`.
 *
 * O handoff pede 84 e desenha o botão sangrando 8 px abaixo da borda de baixo da
 * tela. Isso vem de uma moldura de iPhone: no Android essa faixa é a área do
 * gesto de home, e alvo de toque dentro dela é alvo que o sistema intercepta.
 * Aqui o botão fica inteiro na área segura e ganha altura em vez de profundidade
 * — sobe [ELEVACAO_DO_BOTAO] e rompe a **borda de cima** da barra, que no Android
 * é a borda que está livre.
 */
val BUTTON_DIAMETER = 76.dp

/**
 * Alta o bastante para o leque inteiro caber dentro dela.
 *
 * Meia-altura de 260 dp contra 194 dp do topo do alvo mais alto e 238 dp da dica
 * que vai acima dele.
 */
private val ANCHOR_HEIGHT = 520.dp

/** O áudio parte primeiro: é o alvo mais provável, e é dele que o olho precisa antes. */
private val ENTER_DELAY = mapOf(
    CaptureFormat.AUDIO to 0L,
    CaptureFormat.TEXT to 20L,
    CaptureFormat.PHOTO to 40L,
)

/**
 * O `+`, em dois estados.
 *
 * Em repouso é um disco cheio de ameixa com o halo respirando. Com o leque aberto
 * vira contorno: deixou de ser botão e passou a ser a origem do gesto, e um disco
 * cheio ali competiria com os alvos.
 *
 * Não há mais um terceiro estado vermelho: descartar a gravação virou o lado
 * esquerdo do controle da tela de gravação, e o `+` não precisa mais dizer duas
 * coisas.
 */
@Composable
private fun HubButton(
    activePair: Boolean,
    isRecording: Boolean,
    reduced: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme

    val background by animateColorAsState(
        targetValue = if (activePair) Color.Transparent else colors.primary,
        animationSpec = tween(Motion.FAST),
        label = "fundoDoBotao",
    )
    val outline by animateColorAsState(
        targetValue = if (activePair) Color.White.copy(alpha = 0.38f) else Color.Transparent,
        animationSpec = tween(Motion.FAST),
        label = "contornoDoBotao",
    )
    val tinta by animateColorAsState(
        targetValue = if (activePair) Color.White.copy(alpha = 0.5f) else colors.onPrimary,
        animationSpec = tween(Motion.FAST),
        label = "tintaDoBotao",
    )
    val scale = animateFloatAsState(
        targetValue = if (activePair) 0.94f else 1f,
        animationSpec = Motion.gestureSpring(),
        label = "escalaDoBotao",
    )
    // A sombra vale para o botão sólido. Sobre o véu ela não separa nada de nada
    // e só borra o contorno tracejado.
    val shadow = animateFloatAsState(
        targetValue = if (activePair) 0f else 1f,
        animationSpec = tween(Motion.FAST),
        label = "sombraDoBotao",
    )

    // Nada respira enquanto o leque está aberto nem por trás da tela de gravação:
    // uma animação infinita rodando debaixo de uma superfície opaca é bateria
    // gasta em quadro nenhum.
    val atRest = !activePair && !isRecording && !reduced

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                shadowElevation = 10.dp.toPx() * shadow.value
                shape = CircleShape
                ambientShadowColor = colors.primary
                spotShadowColor = colors.primary
            }
            .size(BUTTON_DIAMETER)
            .halo(ativo = atRest, color = colors.primary)
            .hubBreath(ativo = atRest)
            .background(background, CircleShape)
            .dashedCircleOutline { outline },
    ) {
        Icon(
            imageVector = AppIcons.Plus,
            contentDescription = null,
            tint = tinta,
            modifier = Modifier.size(30.dp),
        )
    }
}

/**
 * O halo: uma onda de 4 s que sai do botão e se apaga antes de chegar longe.
 *
 * Uma animação só, sem cor nova e sem piscar. Existe para o botão não sumir no
 * canto do olho de quem abriu o app para outra coisa, e por isso gasta 2,8 s
 * crescendo e 1,2 s parada — um pulso contínuo viraria alarme.
 *
 * O valor animado é lido **dentro** do `drawBehind`, e não na composição: assim
 * só a fase de desenho é invalidada a cada quadro, em vez de o botão inteiro
 * recompor 60 vezes por segundo para desenhar um círculo.
 */
private fun Modifier.halo(ativo: Boolean, color: Color): Modifier = composed {
    if (!ativo) return@composed this
    val transition = rememberInfiniteTransition(label = "halo")
    val advance = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 4_000
                0f at 0 using LinearOutSlowInEasing
                1f at 2_800
                1f at 4_000
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "avancoDoHalo",
    )
    drawBehind {
        val f = advance.value
        drawCircle(
            color = color,
            radius = size.minDimension / 2f * (1f + 0.55f * f),
            alpha = 0.45f * (1f - f),
        )
    }
}

/** O respiro de 4,5% que acompanha o halo. */
private fun Modifier.hubBreath(ativo: Boolean): Modifier = composed {
    if (!ativo) return@composed this
    val transition = rememberInfiniteTransition(label = "respiro")
    val scale = transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.045f,
        animationSpec = infiniteRepeatable(
            animation = tween(2_000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "escalaDoRespiro",
    )
    graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
    }
}

/** O contorno tracejado do `+` quando ele é origem e não botão. */
private fun Modifier.dashedCircleOutline(color: () -> Color): Modifier = drawBehind {
    val tinta = color()
    if (tinta.alpha < 0.01f) return@drawBehind
    val line = 1.6.dp.toPx()
    drawCircle(
        color = tinta,
        radius = size.minDimension / 2f - line / 2f,
        style = Stroke(
            width = line,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(line * 4.5f, line * 3.5f)),
        ),
    )
}

/** O quanto o alvo cresce ao ser alcançado: de 68 dp para 76 dp. */
private val HIGHLIGHT_GROWTH =
    MARKED_TARGET_DIAMETER.value / TARGET_DIAMETER.value - 1f

/**
 * Um dos três alvos, saindo de dentro do `+`.
 *
 * O alvo **voa** do botão até o lugar dele em vez de aparecer lá: é o que conta
 * que aquilo saiu daqui e que o caminho de volta desfaz. A posição é interpolada
 * dentro do `graphicsLayer`, na fase de desenho — três alvos animando posição por
 * layout obrigariam a âncora inteira a remedir a cada quadro do gesto.
 *
 * Em repouso o disco é o tom claro da própria ação com o ícone na cor cheia; sob
 * o dedo ele inverte — enche de cor, ganha o anel por fora, cresce 12% e engrossa
 * o rótulo. Tudo isso sai de **um** valor animado, lido na fase de desenho: o
 * realce persegue o dedo, e um alvo que recompusesse a cada quadro para trocar
 * quatro propriedades chegaria atrasado.
 */
@Composable
private fun FanTarget(
    format: CaptureFormat,
    marked: Boolean,
    isVisible: Boolean,
    delay: Long,
    reduced: Boolean,
) {
    val palette = formatColors(format)
    val destination: DpOffset = offsetOf(format)

    val advance = remember { Animatable(0f) }
    LaunchedEffect(isVisible) {
        if (isVisible) {
            if (delay > 0 && !reduced) delay(delay)
            advance.animateTo(1f, Motion.gestureSpring())
        } else {
            advance.animateTo(0f, tween(100, easing = FastOutSlowInEasing))
        }
    }

    val emphasis = animateFloatAsState(
        targetValue = if (marked) 1f else 0f,
        animationSpec = tween(TARGET_HIGHLIGHT_MS, easing = LinearOutSlowInEasing),
        label = "realceDoAlvo",
    )
    val tinta by animateColorAsState(
        targetValue = if (marked) Color.White else palette.color,
        animationSpec = tween(TARGET_HIGHLIGHT_MS),
        label = "tintaDoAlvo",
    )

    // O rótulo pendura **fora** da caixa do disco, e não dentro de uma coluna com
    // ele: numa coluna o que se centraliza no destino é o conjunto disco+rótulo, e
    // o disco desenhado subiria uns 13 dp acima do ponto que o dedo testa. Com
    // seleção por proximidade essa diferença é a distância entre o alvo que se vê
    // e o alvo que existe.
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .requiredSize(TARGET_DIAMETER)
            .graphicsLayer {
                val f = advance.value
                translationX = destination.x.toPx() * f
                translationY = destination.y.toPx() * f
                alpha = f.coerceIn(0f, 1f)
                val growth = (0.62f + 0.38f * f) * (1f + HIGHLIGHT_GROWTH * emphasis.value)
                scaleX = growth
                scaleY = growth
            }
            .drawBehind {
                val r = emphasis.value
                val radius = size.minDimension / 2f
                // O anel fica por fora do disco: confirma a escolha sem disputar
                // espaço com o ícone que está sendo apontado.
                if (r > 0.01f) {
                    drawCircle(color = palette.color, radius = radius + 7.dp.toPx() * r, alpha = 0.26f * r)
                }
                drawCircle(color = lerp(palette.background, palette.color, r), radius = radius)
            },
    ) {
        Icon(
            imageVector = formatIcon(format),
            contentDescription = null,
            tint = tinta,
            modifier = Modifier.size(26.dp),
        )
        Text(
            text = formatLabel(format),
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            fontWeight = if (marked) FontWeight.Bold else FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .requiredWidth(112.dp)
                .offset(y = TARGET_DIAMETER + 9.dp)
                .graphicsLayer { alpha = 0.78f + 0.22f * emphasis.value },
        )
    }
}

/** O comprimento do toco que aponta para cima enquanto nenhum alvo está marcado. */
private val GUIDE_STUB = 46.dp

/**
 * A linha pontilhada do `+` até o alvo.
 *
 * O gesto precisa de um trilho visível: sem nada ligando as pontas, três discos
 * flutuando sobre um véu não dizem que saíram do botão nem que o caminho de volta
 * desfaz. Com nada marcado ela é um toco apontando para cima — a única instrução
 * que falta a quem acabou de abrir o leque é *para onde* —, e ela se estica até o
 * alvo quando o dedo escolhe um.
 *
 * A ponta é um `Animatable<Offset>` lido na fase de desenho: a linha acompanha a
 * troca de alvo com a mesma mola dos discos, sem recompor ninguém.
 */
@Composable
private fun GuideToTarget(target: GestureTarget, activePair: Boolean) {
    val density = LocalDensity.current
    val marked = (target as? GestureTarget.Mode)?.format
    val destination = with(density) {
        val point = marked?.let(::offsetOf) ?: DpOffset(0.dp, -(ORIGIN_RADIUS + GUIDE_STUB))
        Offset(point.x.toPx(), point.y.toPx())
    }

    val tip = remember { Animatable(destination, Offset.VectorConverter) }
    LaunchedEffect(destination) { tip.animateTo(destination, Motion.gestureSpring()) }

    val presence = animateFloatAsState(
        targetValue = if (activePair) 1f else 0f,
        animationSpec = tween(Motion.FAST),
        label = "guia",
    )
    val emphasis = animateFloatAsState(
        targetValue = if (marked != null) 1f else 0f,
        animationSpec = tween(TARGET_HIGHLIGHT_MS),
        label = "realceDaGuia",
    )

    Box(
        Modifier.requiredSize(1.dp).drawBehind {
            val strength = presence.value
            if (strength < 0.01f) return@drawBehind
            val end = tip.value
            val distance = end.getDistance()
            if (distance < 1f) return@drawBehind
            val start = end * (ORIGIN_RADIUS.toPx() / distance)
            drawLine(
                color = Color.White,
                start = center + start,
                end = center + end * strength,
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round,
                alpha = (0.26f + 0.18f * emphasis.value) * strength,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 7.dp.toPx())),
            )
        },
    )
}

/**
 * A frase que muda conforme o dedo anda.
 *
 * "arraste e solte no alvo" só serve enquanto nada está marcado; assim que há um
 * alvo sob o dedo, a frase útil é o que **soltar** vai fazer — e a pílula veste a
 * cor daquele alvo, que é o segundo lugar em que a associação cor→modo se forma.
 * É o que ensina o leque na primeira vez sem ninguém explicar.
 */
@Composable
private fun GestureHint(target: GestureTarget, activePair: Boolean) {
    val marked = (target as? GestureTarget.Mode)?.format
    val text = when (marked) {
        CaptureFormat.TEXT -> "solte para escrever"
        CaptureFormat.AUDIO -> "solte para gravar"
        CaptureFormat.PHOTO -> "solte para fotografar"
        null -> "arraste e solte no alvo"
    }
    val background by animateColorAsState(
        targetValue = marked?.let { formatColors(it).color } ?: NIGHT.copy(alpha = 0.82f),
        animationSpec = tween(TARGET_HIGHLIGHT_MS),
        label = "fundoDaDica",
    )
    val presence = animateFloatAsState(
        targetValue = if (activePair) 1f else 0f,
        animationSpec = tween(Motion.FAST),
        label = "dica",
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .offset(y = (-238).dp)
            .graphicsLayer { alpha = presence.value }
            // A frase troca de tamanho junto com o alvo: sem isto a pílula daria
            // um salto de largura a cada vez que o dedo entra num disco.
            .animateContentSize(tween(TARGET_HIGHLIGHT_MS, easing = LinearOutSlowInEasing))
            .background(background, CircleShape)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            fontWeight = if (marked != null) FontWeight.Bold else FontWeight.Medium,
        )
    }
}
