package com.jean.vocabs.ui.captura

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jean.vocabs.shared.domain.FormatoCaptura
import com.jean.vocabs.ui.components.ALTURA_DA_BARRA
import com.jean.vocabs.ui.components.BandeiraCircular
import com.jean.vocabs.ui.components.Icones
import com.jean.vocabs.ui.components.Movimento
import com.jean.vocabs.ui.components.corDeDescarte
import com.jean.vocabs.ui.components.coresDoFormato
import com.jean.vocabs.ui.components.formatarDuracao
import com.jean.vocabs.ui.components.iconeDoFormato
import com.jean.vocabs.ui.components.movimentoReduzido
import com.jean.vocabs.ui.components.rotuloDoFormato
import androidx.core.view.WindowCompat
import com.jean.vocabs.contracts.Idioma
import com.jean.vocabs.ui.theme.LocalTemaEscuro
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.pow

/**
 * O `+` e tudo que sai dele.
 *
 * O handoff troca dois toques por um gesto: **arrastar do `+`**. Segurar abre um
 * leque de três alvos, o dedo aponta para um e soltar executa. O áudio começa a
 * gravar no instante em que o dedo entra no alvo — não ao soltar — e soltar
 * encerra. O dedo nunca sai da tela entre abrir e guardar, e não existe botão de
 * parar em lugar nenhum.
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
 * O hub ocupa a tela inteira porque o leque, o véu e a gravação precisam desenhar
 * por cima da barra e do conteúdo. Nada aqui intercepta toque além do próprio
 * botão: um `Box` sem `pointerInput` não consome nada, e a barra de baixo
 * continua clicável através dele.
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
    val emGesto = aberto || gravando
    val sobreCancelar = gravando && alvo == AlvoDoGesto.Origem

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

    // A gravação apaga a tela inteira, e o app desenha atrás da barra de status.
    // No tema claro os ícones do sistema são escuros: sobre o véu de 96% eles
    // sumiriam. Vira o relógio e a bateria para o claro enquanto durar, e devolve
    // ao que o tema pede quando o dedo sobe.
    val vista = LocalView.current
    val temaEscuro = LocalTemaEscuro.current
    DisposableEffect(gravando, temaEscuro) {
        val controle = (vista.context as? Activity)
            ?.window
            ?.let { WindowCompat.getInsetsController(it, vista) }
        controle?.isAppearanceLightStatusBars = !gravando && !temaEscuro
        onDispose { controle?.isAppearanceLightStatusBars = !temaEscuro }
    }

    // O leque continua composto por um instante depois de fechar, para a saída
    // poder ser animada. Sem isto os alvos desapareceriam no quadro em que o
    // dedo sobe, e o gesto terminaria num corte seco.
    var desenhando by remember { mutableStateOf(false) }
    LaunchedEffect(emGesto) {
        if (emGesto) {
            desenhando = true
        } else {
            delay(Movimento.PADRAO.toLong() + 80)
            desenhando = false
        }
    }

    // Um véu só, com dois destinos: o do leque deixa o conteúdo legível por trás
    // (a pessoa está decidindo, e o app é o contexto da decisão), o da gravação
    // apaga tudo — não há nada para ler enquanto se fala.
    val veu = animateFloatAsState(
        targetValue = when {
            gravando -> 0.96f
            aberto -> 0.46f
            else -> 0f
        },
        animationSpec = tween(if (gravando) Movimento.PADRAO else Movimento.RAPIDO, easing = FastOutSlowInEasing),
        label = "veu",
    )

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .drawBehind { if (veu.value > 0.001f) drawRect(NOITE, alpha = veu.value) },
        )

        // O relógio e a onda são da tela, não do botão: ancorá-los ao `+` os
        // jogaria para fora em aparelho baixo e os deixaria perdidos no meio em
        // aparelho alto. Eles pertencem ao centro da tela, que é para onde o
        // olho vai quando o resto apaga.
        if (desenhando) {
            PainelDeGravacao(
                captura = captura,
                idioma = idioma,
                gravando = gravando,
                modifier = Modifier.align(Alignment.Center).offset(y = (-48).dp),
            )
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .fillMaxWidth()
                .height(ALTURA_DA_ANCORA)
                // A âncora é alta para caber o leque inteiro sem apertar as
                // medidas de ninguém, e desce até o centro dela coincidir com o
                // centro do botão. Daí em diante todo alvo é um `offset` puro a
                // partir do `+`, que é como o handoff descreve as posições.
                .offset(y = ALTURA_DA_ANCORA / 2 - ALTURA_DA_BARRA / 2 - ELEVACAO_DO_BOTAO),
        ) {
            if (desenhando) {
                GuiaAteOAlvo(alvo, gravando)
                FormatoCaptura.entries.forEach { formato ->
                    AlvoDoLeque(
                        formato = formato,
                        marcado = alvo == AlvoDoGesto.Modo(formato),
                        visivel = if (gravando) formato == FormatoCaptura.AUDIO else aberto,
                        atraso = ATRASO_DE_ENTRADA.getValue(formato),
                        reduzido = reduzido,
                    )
                }
                SaidasDoGesto(alvo, aberto, gravando, sobreCancelar)
            }

            BotaoDoHub(
                aberto = aberto,
                gravando = gravando,
                sobreCancelar = sobreCancelar,
                reduzido = reduzido,
                modifier = Modifier
                    .offset(y = -ELEVACAO_DO_BOTAO)
                    .semantics {
                        contentDescription = if (gravando) "Gravando. Toque para guardar." else "Capturar"
                        // O arrasto não tem equivalente para quem navega por
                        // leitor de tela: as três saídas do leque viram ações
                        // do próprio botão, e a gravação iniciada por aqui é
                        // encerrada por um toque em vez de por uma soltura.
                        customActions = listOf(
                            CustomAccessibilityAction("Escrever") { abrirTexto(); true },
                            CustomAccessibilityAction("Fotografar") { captura.tirarFoto(); true },
                            CustomAccessibilityAction(
                                if (captura.gravando) "Parar e guardar" else "Gravar áudio",
                            ) {
                                when {
                                    captura.gravando -> captura.guardarAudio()
                                    captura.temPermissaoDeAudio -> captura.gravarAudio()
                                    else -> captura.pedirPermissaoDeAudio()
                                }
                                true
                            },
                        )
                    }
                    .pointerInput(Unit) {
                        val raioDeOrigemPx = RAIO_DE_ORIGEM.toPx()
                        val alcancePx = ALCANCE_DO_LEQUE.toPx()
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
                                // Toque solto no `+`. Se houver gravação em
                                // curso — só acontece pela ação de
                                // acessibilidade — o mesmo toque encerra.
                                if (captura.gravando) captura.guardarAudio() else abrirTexto()
                                return@awaitEachGesture
                            }

                            aberto = true
                            haptico.performHapticFeedback(HapticFeedbackType.LongPress)

                            var atual: AlvoDoGesto = AlvoDoGesto.Origem
                            var faltouMicrofone = false
                            var soltou = false

                            while (true) {
                                val mudanca = proximaMudanca(primeiro) ?: break
                                val desloc = mudanca.position - centro

                                val novo = if (captura.gravando) {
                                    if (voltouParaOrigem(desloc, raioDeOrigemPx)) AlvoDoGesto.Origem
                                    else AlvoDoGesto.Modo(FormatoCaptura.AUDIO)
                                } else {
                                    alvoPara(desloc, raioDeOrigemPx, alcancePx)
                                }

                                if (novo != atual) {
                                    atual = novo
                                    alvo = novo
                                    haptico.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    if (novo == AlvoDoGesto.Modo(FormatoCaptura.AUDIO) && !captura.gravando) {
                                        if (captura.temPermissaoDeAudio) captura.gravarAudio()
                                        else faltouMicrofone = true
                                    }
                                }

                                if (!mudanca.pressed) {
                                    soltou = true
                                    break
                                }
                            }

                            aberto = false
                            alvo = AlvoDoGesto.Origem
                            concluir(captura, atual, soltou, faltouMicrofone, haptico, abrirTexto)
                        }
                    },
            )
        }
    }
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
 * O que soltar o dedo significa.
 *
 * [soltou] separa o dedo que subiu do gesto que o sistema cancelou — outro
 * ponteiro, a tela apagando, o app indo para trás. Um cancelamento tratado como
 * soltura guardaria um áudio que ninguém terminou de gravar.
 */
private fun concluir(
    captura: CapturaRapida,
    alvo: AlvoDoGesto,
    soltou: Boolean,
    faltouMicrofone: Boolean,
    haptico: HapticFeedback,
    abrirTexto: () -> Unit,
) {
    if (captura.gravando) {
        if (soltou && alvo != AlvoDoGesto.Origem) captura.guardarAudio() else captura.cancelarAudio()
        haptico.performHapticFeedback(HapticFeedbackType.LongPress)
        return
    }
    if (!soltou) return
    when {
        faltouMicrofone -> captura.pedirPermissaoDeAudio()
        alvo == AlvoDoGesto.Modo(FormatoCaptura.TEXTO) -> abrirTexto()
        alvo == AlvoDoGesto.Modo(FormatoCaptura.FOTO) -> captura.tirarFoto()
        // O leque abriu por tempo e o dedo nunca saiu do `+`. Isso é um toque
        // devagar, não uma desistência: quem só encostou no botão fica com o
        // caminho do texto em vez de com um gesto que não fez nada. O handoff
        // manda fechar sem aviso, mas ali "soltar fora" quer dizer soltar longe,
        // e é isso que o `else` cobre.
        alvo == AlvoDoGesto.Origem -> abrirTexto()
        else -> Unit
    }
}

/** Cor única do véu e do fundo da gravação: ameixa quase preta, nos dois temas. */
private val NOITE = Color(0xFF17111F)

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
 * Meia-altura de 260 dp contra 205 dp do topo do alvo mais distante e 238 dp da
 * dica que vai acima dele.
 */
private val ALTURA_DA_ANCORA = 520.dp

/** O áudio parte primeiro: é o alvo mais provável, e é dele que o olho precisa antes. */
private val ATRASO_DE_ENTRADA = mapOf(
    FormatoCaptura.AUDIO to 0L,
    FormatoCaptura.TEXTO to 20L,
    FormatoCaptura.FOTO to 40L,
)

/**
 * O `+`, em quatro estados.
 *
 * Em repouso é um disco cheio de ameixa com o halo respirando. Com o leque
 * aberto vira contorno: deixou de ser botão e passou a ser a origem do gesto, e
 * um disco cheio ali competiria com os alvos. Gravando, o mesmo contorno fica
 * vermelho e o `+` gira 45° — o alvo que fez a coisa é o que a desfaz, e o giro
 * é o que liga uma forma à outra. Com o dedo em cima dele durante a gravação ele
 * enche: descartar deixou de ser ameaça e virou o que vai acontecer.
 */
@Composable
private fun BotaoDoHub(
    aberto: Boolean,
    gravando: Boolean,
    sobreCancelar: Boolean,
    reduzido: Boolean,
    modifier: Modifier = Modifier,
) {
    val cores = MaterialTheme.colorScheme
    val descarte = corDeDescarte()
    val vazio = aberto || gravando

    val fundo by animateColorAsState(
        targetValue = when {
            sobreCancelar -> descarte
            gravando -> descarte.copy(alpha = 0.18f)
            aberto -> Color.Transparent
            else -> cores.primary
        },
        animationSpec = tween(Movimento.RAPIDO),
        label = "fundoDoBotao",
    )
    val contorno by animateColorAsState(
        targetValue = when {
            sobreCancelar -> Color.Transparent
            gravando -> descarte
            aberto -> Color.White.copy(alpha = 0.38f)
            else -> Color.Transparent
        },
        animationSpec = tween(Movimento.RAPIDO),
        label = "contornoDoBotao",
    )
    val tinta by animateColorAsState(
        targetValue = when {
            sobreCancelar -> Color.White
            gravando -> descarte
            aberto -> Color.White.copy(alpha = 0.5f)
            else -> cores.onPrimary
        },
        animationSpec = tween(Movimento.RAPIDO),
        label = "tintaDoBotao",
    )
    val escala = animateFloatAsState(
        targetValue = when {
            sobreCancelar -> 1.08f
            vazio -> 0.94f
            else -> 1f
        },
        animationSpec = Movimento.molaDeGesto(),
        label = "escalaDoBotao",
    )
    val giro = animateFloatAsState(
        targetValue = if (gravando) 45f else 0f,
        animationSpec = Movimento.mola(),
        label = "giroDoBotao",
    )
    // A sombra vale para o botão sólido. Sobre o véu ela não separa nada de nada
    // e só borra o contorno tracejado.
    val sombra = animateFloatAsState(
        targetValue = if (vazio) 0f else 1f,
        animationSpec = tween(Movimento.RAPIDO),
        label = "sombraDoBotao",
    )

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
            .halo(ativo = !vazio && !reduzido, cor = cores.primary)
            .respiroDoHub(ativo = !vazio && !reduzido)
            .background(fundo, CircleShape)
            .contornoCircularTracejado { contorno },
    ) {
        Icon(
            imageVector = Icones.Mais,
            contentDescription = null,
            tint = tinta,
            modifier = Modifier
                .size(30.dp)
                .graphicsLayer { rotationZ = giro.value },
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

/**
 * Um dos três alvos, saindo de dentro do `+`.
 *
 * O alvo **voa** do botão até o lugar dele em vez de aparecer lá: é o que conta
 * que aquilo saiu daqui e que o caminho de volta desfaz. A posição é interpolada
 * dentro do `graphicsLayer`, na fase de desenho — três alvos animando posição por
 * layout obrigariam a âncora inteira a remedir a cada quadro do gesto.
 */
@Composable
private fun AlvoDoLeque(
    formato: FormatoCaptura,
    marcado: Boolean,
    visivel: Boolean,
    atraso: Long,
    reduzido: Boolean,
) {
    val cores = MaterialTheme.colorScheme
    val paleta = coresDoFormato(formato)
    val principal = formato == FormatoCaptura.AUDIO
    val diametro = if (principal) DIAMETRO_DO_ALVO_CENTRAL else DIAMETRO_DO_ALVO_LATERAL
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
        animationSpec = Movimento.molaDeGesto(),
        label = "realceDoAlvo",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .requiredWidth(112.dp)
            .graphicsLayer {
                val f = avanco.value
                translationX = destino.x.toPx() * f
                translationY = destino.y.toPx() * f
                alpha = f.coerceIn(0f, 1f)
                val crescimento = 0.62f + 0.38f * f + 0.09f * realce.value
                scaleX = crescimento
                scaleY = crescimento
            },
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .requiredSize(diametro)
                // O anel do alvo marcado fica por fora do disco: confirma a
                // escolha sem mexer no tamanho do que está sendo apontado.
                .drawBehind {
                    if (realce.value > 0.01f) {
                        drawCircle(
                            color = paleta.cor,
                            radius = size.minDimension / 2f + 7.dp.toPx() * realce.value,
                            alpha = 0.32f * realce.value,
                        )
                    }
                }
                .background(if (principal) paleta.cor else cores.surface, CircleShape),
        ) {
            Icon(
                imageVector = iconeDoFormato(formato),
                contentDescription = null,
                tint = if (principal) Color.White else paleta.cor,
                modifier = Modifier.size(if (principal) 30.dp else 23.dp),
            )
        }
        Text(
            text = rotuloDoFormato(formato),
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            fontWeight = if (marcado) FontWeight.Bold else FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.graphicsLayer { alpha = 0.72f + 0.28f * realce.value },
        )
    }
}

/**
 * A linha pontilhada do `+` até o alvo marcado.
 *
 * O gesto precisa de um trilho visível: sem nada ligando as pontas, três discos
 * flutuando sobre um véu não dizem que saíram do botão nem que o caminho de
 * volta desfaz. Ela some quando nenhum alvo está marcado — uma linha apontando
 * para o nada seria pior que linha nenhuma.
 */
@Composable
private fun GuiaAteOAlvo(alvo: AlvoDoGesto, gravando: Boolean) {
    val destino = (alvo as? AlvoDoGesto.Modo)?.formato?.let(::deslocamentoDe)
        ?: DESLOCAMENTO_DO_AUDIO.takeIf { gravando }
    var ultimo by remember { mutableStateOf(DESLOCAMENTO_DO_AUDIO) }
    LaunchedEffect(destino) { if (destino != null) ultimo = destino }

    val forca = animateFloatAsState(
        targetValue = if (destino != null) 1f else 0f,
        animationSpec = tween(Movimento.RAPIDO),
        label = "guia",
    )

    Box(
        Modifier.requiredSize(1.dp).drawBehind {
            if (forca.value < 0.01f) return@drawBehind
            val fim = Offset(ultimo.x.toPx(), ultimo.y.toPx())
            val inicio = fim * (RAIO_DE_ORIGEM.toPx() / fim.getDistance())
            drawLine(
                color = Color.White,
                start = center + inicio,
                end = center + fim * forca.value,
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round,
                alpha = 0.42f * forca.value,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 7.dp.toPx())),
            )
        },
    )
}

/**
 * A frase que muda conforme o dedo anda.
 *
 * "arraste e solte no alvo" só serve enquanto nada está marcado; assim que há um
 * alvo sob o dedo, a frase útil é o que **soltar** vai fazer. É o que ensina o
 * leque na primeira vez sem ninguém explicar — e, na gravação, é o que ensina
 * que voltar ao `+` descarta, porque a frase troca sozinha quando o dedo chega
 * lá.
 */
@Composable
private fun SaidasDoGesto(
    alvo: AlvoDoGesto,
    aberto: Boolean,
    gravando: Boolean,
    sobreCancelar: Boolean,
) {
    val descarte = corDeDescarte()
    val texto = when {
        sobreCancelar -> "solte para descartar"
        gravando -> "solte para guardar"
        alvo is AlvoDoGesto.Modo -> when (alvo.formato) {
            FormatoCaptura.TEXTO -> "solte para escrever"
            FormatoCaptura.AUDIO -> "solte para gravar"
            FormatoCaptura.FOTO -> "solte para fotografar"
        }
        else -> "arraste e solte no alvo"
    }
    val presenca = animateFloatAsState(
        targetValue = if (aberto || gravando) 1f else 0f,
        animationSpec = tween(Movimento.RAPIDO),
        label = "saidas",
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .offset(y = (-238).dp)
            .graphicsLayer { alpha = presenca.value }
            .background(NOITE.copy(alpha = 0.82f), CircleShape)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Text(
            text = texto,
            style = MaterialTheme.typography.labelMedium,
            color = if (sobreCancelar) descarte else Color.White,
        )
    }

    // O lembrete do caminho de volta mora entre o alvo e o `+`, que é o trecho
    // que o dedo percorre para desfazer. Some quando o dedo já chegou lá: nesse
    // ponto quem fala é o próprio botão, vermelho e cheio.
    Text(
        text = "arraste de volta para descartar",
        style = MaterialTheme.typography.bodySmall,
        color = descarte,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .requiredWidth(240.dp)
            .offset(y = (-96).dp)
            .graphicsLayer { alpha = if (gravando && !sobreCancelar) presenca.value else 0f },
    )
}

/**
 * O relógio, a onda e o idioma de destino.
 *
 * Não é uma tela nova: é o mesmo hub com o véu fechado. O alvo do áudio continua
 * exatamente onde estava quando o dedo chegou nele e os outros dois recolhem
 * para dentro do `+`. Trocar isto por uma tela custaria uma transição inteira no
 * meio de um gesto em curso — e faria o alvo saltar de lugar debaixo do dedo.
 */
@Composable
private fun PainelDeGravacao(
    captura: CapturaRapida,
    idioma: Idioma,
    gravando: Boolean,
    modifier: Modifier = Modifier,
) {
    val presenca = animateFloatAsState(
        targetValue = if (gravando) 1f else 0f,
        animationSpec = tween(Movimento.PADRAO, easing = FastOutSlowInEasing),
        label = "painelDeGravacao",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(15.dp),
        modifier = modifier.graphicsLayer {
            alpha = presenca.value
            translationY = 18.dp.toPx() * (1f - presenca.value)
        },
    ) {
        Text(
            text = formatarDuracao(captura.segundos),
            style = MaterialTheme.typography.displaySmall.copy(fontSize = 44.sp, lineHeight = 48.sp),
            color = Color.White,
        )
        OndaDoMicrofone(gravando) { captura.nivelAgora() }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            BandeiraCircular(idioma, tamanho = 16.dp)
            // O handoff escreve "o idioma sai da fala". Não sai: não há detector
            // de idioma no app, e o que existe é a regra de fallback do próprio
            // handoff — cai no curso aberto no hub. A frase diz o que de fato
            // acontece, e é ela que precisa mudar no dia em que houver detecção.
            Text(
                text = "gravando",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.78f),
            )
        }
    }
}

/** Quantas barras a onda tem, e de quanto em quanto tempo entra uma nova. */
private const val BARRAS_DA_ONDA = 22
private const val INTERVALO_DA_ONDA = 70L

/**
 * A onda: o pico real do microfone, uma barra a cada 70 ms.
 *
 * A barra nova entra pela direita e empurra a fila. É o desenho de um gravador
 * de verdade, e responde à única pergunta de quem está falando: *está pegando?*
 * A raiz aplicada ao pico é perceptual e não estética — a fala normal vive na
 * parte de baixo da escala linear, e sem ela a onda passaria a gravação inteira
 * rente ao chão.
 *
 * O histórico é um `FloatArray` cru com um cursor observável. Só o cursor é
 * estado do Compose, e ele é lido dentro do `drawBehind`: catorze quadros por
 * segundo invalidam o desenho de um retângulo e nada mais.
 */
@Composable
private fun OndaDoMicrofone(gravando: Boolean, nivel: () -> Float) {
    val historico = remember { FloatArray(BARRAS_DA_ONDA) }
    var cursor by remember { mutableIntStateOf(0) }
    val cor = MaterialTheme.colorScheme.tertiary

    LaunchedEffect(gravando) {
        if (!gravando) return@LaunchedEffect
        historico.fill(0f)
        cursor = 0
        while (true) {
            historico[cursor % BARRAS_DA_ONDA] = nivel()
            cursor++
            delay(INTERVALO_DA_ONDA)
        }
    }

    Box(
        Modifier
            .requiredSize(width = 208.dp, height = 52.dp)
            .drawBehind {
                val posicao = cursor
                val largura = 4.dp.toPx()
                val vao = 5.5.dp.toPx()
                val minima = 4.dp.toPx()
                for (i in 0 until BARRAS_DA_ONDA) {
                    // A mais recente à direita, as antigas caminhando à esquerda.
                    val indice = ((posicao - 1 - i) % BARRAS_DA_ONDA + BARRAS_DA_ONDA) % BARRAS_DA_ONDA
                    val altura = minima + (size.height - minima) * historico[indice].coerceIn(0f, 1f).pow(0.42f)
                    val x = size.width - largura - i * vao
                    if (x < 0) break
                    drawRoundRect(
                        color = cor,
                        topLeft = Offset(x, (size.height - altura) / 2f),
                        size = Size(largura, altura),
                        cornerRadius = CornerRadius(largura / 2f),
                        alpha = 1f - 0.5f * (i.toFloat() / BARRAS_DA_ONDA),
                    )
                }
            },
    )
}
