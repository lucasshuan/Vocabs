package com.jean.vocabs.ui.captura

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
import com.jean.vocabs.contracts.Idioma
import com.jean.vocabs.shared.domain.CaptureFormat
import com.jean.vocabs.ui.components.ALTURA_DA_BARRA
import com.jean.vocabs.ui.components.Icones
import com.jean.vocabs.ui.components.Movimento
import com.jean.vocabs.ui.components.coresDoFormato
import com.jean.vocabs.ui.components.iconeDoFormato
import com.jean.vocabs.ui.components.movimentoReduzido
import com.jean.vocabs.ui.components.rotuloDoFormato
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
fun HubDeCaptura(
    captura: CapturaRapida,
    idioma: Idioma,
    aoAbrirTexto: () -> Unit,
    aoMudarDeFase: (emGesto: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptico = LocalHapticFeedback.current
    val reduzido = movimentoReduzido()
    val abrirTexto by rememberUpdatedState(aoAbrirTexto)

    var aberto by remember { mutableStateOf(false) }
    var alvo by remember { mutableStateOf<AlvoDoGesto>(AlvoDoGesto.Origem) }
    val gravando = captura.gravando

    // "O hub tomou a tela": o que está atrás desfoca e sai do alcance do leitor de
    // tela. Vale para o leque e para a gravação — a tela de gravação é opaca, mas
    // o conteúdo continua composto por baixo dela, e um `+` alcançável pelo
    // TalkBack debaixo de uma tela que já não é aquela é uma armadilha.
    val emGesto = aberto || gravando
    LaunchedEffect(emGesto) { aoMudarDeFase(emGesto) }

    // Rede de segurança: o hub sai de cena quando uma tela cheia abre, e sair de
    // cena cancela a corrotina do gesto sem passar por `concluir`. Sem isto, uma
    // gravação apanhada por uma navegação continuaria correndo com o microfone
    // aberto e sem nada na tela dizendo isso.
    DisposableEffect(Unit) {
        onDispose {
            if (captura.gravando) captura.cancelarAudio()
            aoMudarDeFase(false)
        }
    }

    // Leque e gravação continuam compostos por um instante depois de fechar, para
    // a saída poder ser animada. Sem isto os alvos desapareceriam no quadro em que
    // o dedo sobe, e o gesto terminaria num corte seco.
    val desenhandoLeque = aindaNaTela(aberto)
    val desenhandoGravacao = aindaNaTela(gravando)

    // O véu do leque deixa o conteúdo legível por trás: a pessoa está decidindo, e
    // o app é o contexto da decisão.
    val veu = animateFloatAsState(
        targetValue = if (aberto) 0.46f else 0f,
        animationSpec = tween(Movimento.RAPIDO, easing = FastOutSlowInEasing),
        label = "veu",
    )

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .drawBehind { if (veu.value > 0.001f) drawRect(NOITE, alpha = veu.value) },
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .fillMaxWidth()
                .height(ALTURA_DA_ANCORA)
                // O `+` fica debaixo da tela de gravação, que é opaca. Deixá-lo
                // no alcance do leitor de tela ofereceria "Capturar" no meio de
                // uma gravação em curso.
                .then(if (gravando) Modifier.clearAndSetSemantics {} else Modifier)
                // A âncora é alta para caber o leque inteiro sem apertar as
                // medidas de ninguém, e desce até o centro dela coincidir com o
                // centro do botão. Daí em diante todo alvo é um `offset` puro a
                // partir do `+`, que é como o handoff descreve as posições.
                .offset(y = ALTURA_DA_ANCORA / 2 - ALTURA_DA_BARRA / 2 - ELEVACAO_DO_BOTAO),
        ) {
            if (desenhandoLeque) {
                GuiaAteOAlvo(alvo, aberto)
                CaptureFormat.entries.forEach { formato ->
                    AlvoDoLeque(
                        formato = formato,
                        marcado = alvo == AlvoDoGesto.Modo(formato),
                        visivel = aberto,
                        atraso = ATRASO_DE_ENTRADA.getValue(formato),
                        reduzido = reduzido,
                    )
                }
                DicaDoGesto(alvo, aberto)
            }

            BotaoDoHub(
                aberto = aberto,
                gravando = gravando,
                reduzido = reduzido,
                modifier = Modifier
                    .offset(y = -ELEVACAO_DO_BOTAO)
                    .semantics {
                        contentDescription = "Capturar"
                        // O arrasto não tem equivalente para quem navega por
                        // leitor de tela: as três saídas do leque viram ações do
                        // próprio botão, e a gravação aberta por aqui é encerrada
                        // pelas ações da tela de gravação.
                        customActions = listOf(
                            CustomAccessibilityAction("Escrever") { abrirTexto(); true },
                            CustomAccessibilityAction("Fotografar") { captura.tirarFoto(); true },
                            CustomAccessibilityAction("Gravar áudio") {
                                if (captura.temPermissaoDeAudio) captura.gravarAudio()
                                else captura.pedirPermissaoDeAudio()
                                true
                            },
                        )
                    }
                    .pointerInput(Unit) {
                        val alvos = alvosEmPixels()
                        val raioDoAlvoPx = RAIO_DO_ALVO.toPx()
                        val raioDeOrigemPx = RAIO_DE_ORIGEM.toPx()
                        val folga = viewConfiguration.touchSlop

                        awaitEachGesture {
                            val primeiro = awaitFirstDown(requireUnconsumed = false)
                            primeiro.consume()
                            val centro = Offset(size.width / 2f, size.height / 2f)

                            // Até 180 ms parado ainda é toque; a partir daí, ou
                            // a partir de um dedo que andou, é pressão e o leque
                            // abre. O limiar do sistema (500 ms) é longo demais
                            // para um gesto que precisa render antes de a frase
                            // terminar de passar.
                            val soltouCedo = withTimeoutOrNull(ABERTURA_DO_LEQUE_MS) {
                                var solto: PointerInputChange? = null
                                while (true) {
                                    val mudanca = proximaMudanca(primeiro) ?: break
                                    if (!mudanca.pressed) {
                                        solto = mudanca
                                        break
                                    }
                                    if ((mudanca.position - primeiro.position).getDistance() > folga) break
                                }
                                solto
                            }

                            if (soltouCedo != null) {
                                abrirTexto()
                                return@awaitEachGesture
                            }

                            aberto = true
                            haptico.performHapticFeedback(HapticFeedbackType.LongPress)

                            var atual: AlvoDoGesto = AlvoDoGesto.Origem
                            var soltou = false

                            while (true) {
                                val mudanca = proximaMudanca(primeiro) ?: break
                                val novo = alvoPara(
                                    desloc = mudanca.position - centro,
                                    alvos = alvos,
                                    raioDoAlvoPx = raioDoAlvoPx,
                                    raioDeOrigemPx = raioDeOrigemPx,
                                )

                                if (novo != atual) {
                                    atual = novo
                                    alvo = novo
                                    // Chegar num alvo tem toque tátil; sair dele
                                    // não. O que a mão precisa sentir é o encaixe,
                                    // e um segundo pulso na saída transformaria
                                    // atravessar o leque numa vibração contínua.
                                    if (novo is AlvoDoGesto.Modo) {
                                        haptico.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                }

                                if (!mudanca.pressed) {
                                    soltou = true
                                    break
                                }
                            }

                            aberto = false
                            alvo = AlvoDoGesto.Origem
                            concluir(captura, atual, soltou, haptico, abrirTexto)
                        }
                    },
            )
        }

        // Por último no `Box`, portanto por cima de tudo — inclusive do `+`, que
        // some atrás dela em vez de continuar clicável debaixo de uma tela opaca.
        if (desenhandoGravacao) {
            TelaDeGravacao(
                captura = captura,
                idioma = idioma,
                gravando = gravando,
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
private fun aindaNaTela(ativo: Boolean, sobra: Long = Movimento.PADRAO.toLong() + 80): Boolean {
    var presente by remember { mutableStateOf(ativo) }
    LaunchedEffect(ativo) {
        if (ativo) {
            presente = true
        } else {
            delay(sobra)
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
private suspend fun androidx.compose.ui.input.pointer.AwaitPointerEventScope.proximaMudanca(
    origem: PointerInputChange,
): PointerInputChange? {
    val evento = awaitPointerEvent()
    val mudanca = evento.changes.firstOrNull { it.id == origem.id } ?: return null
    mudanca.consume()
    return mudanca
}

/**
 * O que soltar o dedo significa — e é o único lugar do gesto onde alguma coisa
 * acontece.
 *
 * [soltou] separa o dedo que subiu do gesto que o sistema cancelou — outro
 * ponteiro, a tela apagando, o app indo para trás. Um cancelamento tratado como
 * soltura abriria a câmera sozinho no meio de uma ligação.
 */
private fun concluir(
    captura: CapturaRapida,
    alvo: AlvoDoGesto,
    soltou: Boolean,
    haptico: HapticFeedback,
    abrirTexto: () -> Unit,
) {
    if (!soltou) return
    when (alvo) {
        is AlvoDoGesto.Modo -> {
            haptico.performHapticFeedback(HapticFeedbackType.LongPress)
            when (alvo.formato) {
                CaptureFormat.TEXT -> abrirTexto()
                CaptureFormat.PHOTO -> captura.tirarFoto()
                CaptureFormat.AUDIO ->
                    if (captura.temPermissaoDeAudio) captura.gravarAudio()
                    else captura.pedirPermissaoDeAudio()
            }
        }
        // O leque abriu por tempo e o dedo nunca saiu do `+`. Isso é um toque
        // devagar, não uma desistência: quem só encostou no botão fica com o
        // caminho do texto em vez de com um gesto que não fez nada.
        AlvoDoGesto.Origem -> abrirTexto()
        // Soltar fora fecha sem capturar nada e sem aviso.
        AlvoDoGesto.Fora -> Unit
    }
}

/** Cor única do véu e do fundo da gravação: ameixa quase preta, nos dois temas. */
internal val NOITE = Color(0xFF17111F)

/** O quanto o botão sobe em relação ao centro da fileira de ícones da barra. */
private val ELEVACAO_DO_BOTAO = 10.dp

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
val DIAMETRO_DO_BOTAO = 76.dp

/**
 * Alta o bastante para o leque inteiro caber dentro dela.
 *
 * Meia-altura de 260 dp contra 194 dp do topo do alvo mais alto e 238 dp da dica
 * que vai acima dele.
 */
private val ALTURA_DA_ANCORA = 520.dp

/** O áudio parte primeiro: é o alvo mais provável, e é dele que o olho precisa antes. */
private val ATRASO_DE_ENTRADA = mapOf(
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
private fun BotaoDoHub(
    aberto: Boolean,
    gravando: Boolean,
    reduzido: Boolean,
    modifier: Modifier = Modifier,
) {
    val cores = MaterialTheme.colorScheme

    val fundo by animateColorAsState(
        targetValue = if (aberto) Color.Transparent else cores.primary,
        animationSpec = tween(Movimento.RAPIDO),
        label = "fundoDoBotao",
    )
    val contorno by animateColorAsState(
        targetValue = if (aberto) Color.White.copy(alpha = 0.38f) else Color.Transparent,
        animationSpec = tween(Movimento.RAPIDO),
        label = "contornoDoBotao",
    )
    val tinta by animateColorAsState(
        targetValue = if (aberto) Color.White.copy(alpha = 0.5f) else cores.onPrimary,
        animationSpec = tween(Movimento.RAPIDO),
        label = "tintaDoBotao",
    )
    val escala = animateFloatAsState(
        targetValue = if (aberto) 0.94f else 1f,
        animationSpec = Movimento.molaDeGesto(),
        label = "escalaDoBotao",
    )
    // A sombra vale para o botão sólido. Sobre o véu ela não separa nada de nada
    // e só borra o contorno tracejado.
    val sombra = animateFloatAsState(
        targetValue = if (aberto) 0f else 1f,
        animationSpec = tween(Movimento.RAPIDO),
        label = "sombraDoBotao",
    )

    // Nada respira enquanto o leque está aberto nem por trás da tela de gravação:
    // uma animação infinita rodando debaixo de uma superfície opaca é bateria
    // gasta em quadro nenhum.
    val emRepouso = !aberto && !gravando && !reduzido

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .graphicsLayer {
                scaleX = escala.value
                scaleY = escala.value
                shadowElevation = 10.dp.toPx() * sombra.value
                shape = CircleShape
                ambientShadowColor = cores.primary
                spotShadowColor = cores.primary
            }
            .size(DIAMETRO_DO_BOTAO)
            .halo(ativo = emRepouso, cor = cores.primary)
            .respiroDoHub(ativo = emRepouso)
            .background(fundo, CircleShape)
            .contornoCircularTracejado { contorno },
    ) {
        Icon(
            imageVector = Icones.Mais,
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
private fun Modifier.halo(ativo: Boolean, cor: Color): Modifier = composed {
    if (!ativo) return@composed this
    val transicao = rememberInfiniteTransition(label = "halo")
    val avanco = transicao.animateFloat(
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
        val f = avanco.value
        drawCircle(
            color = cor,
            radius = size.minDimension / 2f * (1f + 0.55f * f),
            alpha = 0.45f * (1f - f),
        )
    }
}

/** O respiro de 4,5% que acompanha o halo. */
private fun Modifier.respiroDoHub(ativo: Boolean): Modifier = composed {
    if (!ativo) return@composed this
    val transicao = rememberInfiniteTransition(label = "respiro")
    val escala = transicao.animateFloat(
        initialValue = 1f,
        targetValue = 1.045f,
        animationSpec = infiniteRepeatable(
            animation = tween(2_000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "escalaDoRespiro",
    )
    graphicsLayer {
        scaleX = escala.value
        scaleY = escala.value
    }
}

/** O contorno tracejado do `+` quando ele é origem e não botão. */
private fun Modifier.contornoCircularTracejado(cor: () -> Color): Modifier = drawBehind {
    val tinta = cor()
    if (tinta.alpha < 0.01f) return@drawBehind
    val traco = 1.6.dp.toPx()
    drawCircle(
        color = tinta,
        radius = size.minDimension / 2f - traco / 2f,
        style = Stroke(
            width = traco,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(traco * 4.5f, traco * 3.5f)),
        ),
    )
}

/** O quanto o alvo cresce ao ser alcançado: de 68 dp para 76 dp. */
private val CRESCIMENTO_DO_REALCE =
    DIAMETRO_DO_ALVO_MARCADO.value / DIAMETRO_DO_ALVO.value - 1f

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
private fun AlvoDoLeque(
    formato: CaptureFormat,
    marcado: Boolean,
    visivel: Boolean,
    atraso: Long,
    reduzido: Boolean,
) {
    val paleta = coresDoFormato(formato)
    val destino: DpOffset = deslocamentoDe(formato)

    val avanco = remember { Animatable(0f) }
    LaunchedEffect(visivel) {
        if (visivel) {
            if (atraso > 0 && !reduzido) delay(atraso)
            avanco.animateTo(1f, Movimento.molaDeGesto())
        } else {
            avanco.animateTo(0f, tween(100, easing = FastOutSlowInEasing))
        }
    }

    val realce = animateFloatAsState(
        targetValue = if (marcado) 1f else 0f,
        animationSpec = tween(REALCE_DO_ALVO_MS, easing = LinearOutSlowInEasing),
        label = "realceDoAlvo",
    )
    val tinta by animateColorAsState(
        targetValue = if (marcado) Color.White else paleta.cor,
        animationSpec = tween(REALCE_DO_ALVO_MS),
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
            .requiredSize(DIAMETRO_DO_ALVO)
            .graphicsLayer {
                val f = avanco.value
                translationX = destino.x.toPx() * f
                translationY = destino.y.toPx() * f
                alpha = f.coerceIn(0f, 1f)
                val crescimento = (0.62f + 0.38f * f) * (1f + CRESCIMENTO_DO_REALCE * realce.value)
                scaleX = crescimento
                scaleY = crescimento
            }
            .drawBehind {
                val r = realce.value
                val raio = size.minDimension / 2f
                // O anel fica por fora do disco: confirma a escolha sem disputar
                // espaço com o ícone que está sendo apontado.
                if (r > 0.01f) {
                    drawCircle(color = paleta.cor, radius = raio + 7.dp.toPx() * r, alpha = 0.26f * r)
                }
                drawCircle(color = lerp(paleta.fundo, paleta.cor, r), radius = raio)
            },
    ) {
        Icon(
            imageVector = iconeDoFormato(formato),
            contentDescription = null,
            tint = tinta,
            modifier = Modifier.size(26.dp),
        )
        Text(
            text = rotuloDoFormato(formato),
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            fontWeight = if (marcado) FontWeight.Bold else FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .requiredWidth(112.dp)
                .offset(y = DIAMETRO_DO_ALVO + 9.dp)
                .graphicsLayer { alpha = 0.78f + 0.22f * realce.value },
        )
    }
}

/** O comprimento do toco que aponta para cima enquanto nenhum alvo está marcado. */
private val TOCO_DA_GUIA = 46.dp

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
private fun GuiaAteOAlvo(alvo: AlvoDoGesto, aberto: Boolean) {
    val densidade = LocalDensity.current
    val marcado = (alvo as? AlvoDoGesto.Modo)?.formato
    val destino = with(densidade) {
        val ponto = marcado?.let(::deslocamentoDe) ?: DpOffset(0.dp, -(RAIO_DE_ORIGEM + TOCO_DA_GUIA))
        Offset(ponto.x.toPx(), ponto.y.toPx())
    }

    val ponta = remember { Animatable(destino, Offset.VectorConverter) }
    LaunchedEffect(destino) { ponta.animateTo(destino, Movimento.molaDeGesto()) }

    val presenca = animateFloatAsState(
        targetValue = if (aberto) 1f else 0f,
        animationSpec = tween(Movimento.RAPIDO),
        label = "guia",
    )
    val realce = animateFloatAsState(
        targetValue = if (marcado != null) 1f else 0f,
        animationSpec = tween(REALCE_DO_ALVO_MS),
        label = "realceDaGuia",
    )

    Box(
        Modifier.requiredSize(1.dp).drawBehind {
            val forca = presenca.value
            if (forca < 0.01f) return@drawBehind
            val fim = ponta.value
            val distancia = fim.getDistance()
            if (distancia < 1f) return@drawBehind
            val inicio = fim * (RAIO_DE_ORIGEM.toPx() / distancia)
            drawLine(
                color = Color.White,
                start = center + inicio,
                end = center + fim * forca,
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round,
                alpha = (0.26f + 0.18f * realce.value) * forca,
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
private fun DicaDoGesto(alvo: AlvoDoGesto, aberto: Boolean) {
    val marcado = (alvo as? AlvoDoGesto.Modo)?.formato
    val texto = when (marcado) {
        CaptureFormat.TEXT -> "solte para escrever"
        CaptureFormat.AUDIO -> "solte para gravar"
        CaptureFormat.PHOTO -> "solte para fotografar"
        null -> "arraste e solte no alvo"
    }
    val fundo by animateColorAsState(
        targetValue = marcado?.let { coresDoFormato(it).cor } ?: NOITE.copy(alpha = 0.82f),
        animationSpec = tween(REALCE_DO_ALVO_MS),
        label = "fundoDaDica",
    )
    val presenca = animateFloatAsState(
        targetValue = if (aberto) 1f else 0f,
        animationSpec = tween(Movimento.RAPIDO),
        label = "dica",
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .offset(y = (-238).dp)
            .graphicsLayer { alpha = presenca.value }
            // A frase troca de tamanho junto com o alvo: sem isto a pílula daria
            // um salto de largura a cada vez que o dedo entra num disco.
            .animateContentSize(tween(REALCE_DO_ALVO_MS, easing = LinearOutSlowInEasing))
            .background(fundo, CircleShape)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Text(
            text = texto,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            fontWeight = if (marcado != null) FontWeight.Bold else FontWeight.Medium,
        )
    }
}
