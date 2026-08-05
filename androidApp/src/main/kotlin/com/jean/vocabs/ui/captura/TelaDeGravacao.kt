package com.jean.vocabs.ui.capture

import com.jean.vocabs.ui.displayName
import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.jean.vocabs.contracts.Language
import com.jean.vocabs.ui.components.BandeiraCircular
import com.jean.vocabs.ui.components.Icones
import com.jean.vocabs.ui.components.Movimento
import com.jean.vocabs.ui.components.encolheAoTocar
import com.jean.vocabs.ui.components.formatarDuracao
import com.jean.vocabs.ui.components.lembrarToque
import com.jean.vocabs.ui.components.respirando
import com.jean.vocabs.ui.theme.LocalTemaEscuro
import com.jean.vocabs.ui.theme.VocabuColors
import kotlinx.coroutines.delay
import kotlin.math.pow

/**
 * A gravação, com tela e ações próprias.
 *
 * O gesto acaba quando o dedo sobe: a gravação **começa** com a mão já livre, e
 * não enquanto o dedo segura o `+`. Isso muda o que a tela precisa dar. Antes o
 * gesto era o controle — soltar guardava, voltar à origem descartava — e o preço
 * era o telefone preso ao dedo: não dava para apoiar o aparelho na mesa,
 * aproximá-lo de quem estava falando nem trocar de mão sem arriscar a captura.
 * Agora a tela é estável e encerrar é uma decisão à parte.
 *
 * **Encerrar é um toque, e nunca um gesto.** O arrasto lateral que ocupava esta
 * base foi embora: ele lembrava atender uma chamada, adiava guardar e descartar
 * até o fim de um movimento e cobrava aprendizado numa tela que a pessoa olha de
 * relance enquanto alguém fala. Os dois destinos agora estão à vista o tempo todo
 * e custam um toque cada — ver [AcoesDaGravacao] para por que eles não têm o
 * mesmo tamanho.
 *
 * A tela é opaca e barra todo toque no que está debaixo dela. Ela é a última
 * coisa desenhada pelo hub, portanto está por cima do `+`, que não pode continuar
 * clicável debaixo de uma tela que já não é aquela.
 */
@Composable
internal fun TelaDeGravacao(
    capture: CapturaRapida,
    language: Language,
    gravando: Boolean,
    modifier: Modifier = Modifier,
) {
    val guardar: () -> Unit = { capture.guardarAudio() }
    val cancelar: () -> Unit = { capture.cancelarAudio() }

    // Voltar **guarda**. A regra do app é que nada é descartado sem a pessoa
    // pedir, e o pedido tem um lugar só: o botão de descartar. Quem apertou
    // voltar pediu para sair dali, não para perder o que falou — e o áudio
    // guardado por engano custa um toque em Pendentes, enquanto o descartado por
    // engano não custa nada porque não existe mais.
    BackHandler(enabled = gravando) { guardar() }

    // A tela apaga tudo, e o app desenha atrás da barra de status. No tema claro
    // os ícones do sistema são escuros e sumiriam sobre este fundo: vira o relógio
    // e a bateria para o claro enquanto durar, e devolve ao que o tema pede
    // quando a gravação termina.
    val vista = LocalView.current
    val temaEscuro = LocalTemaEscuro.current
    DisposableEffect(temaEscuro) {
        val controle = (vista.context as? Activity)
            ?.window
            ?.let { WindowCompat.getInsetsController(it, vista) }
        controle?.isAppearanceLightStatusBars = false
        onDispose { controle?.isAppearanceLightStatusBars = !temaEscuro }
    }

    val presenca = animateFloatAsState(
        targetValue = if (gravando) 1f else 0f,
        animationSpec = tween(
            if (gravando) Movimento.PADRAO else Movimento.RAPIDO,
            easing = FastOutSlowInEasing,
        ),
        label = "telaDeGravacao",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { alpha = presenca.value }
            .drawBehind { drawRect(NOITE) }
            .then(if (gravando) Modifier.barraToques() else Modifier),
    ) {
        SeloDeGravando(
            gravando = gravando,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 14.dp)
                .graphicsLayer { alpha = presenca.value },
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(19.dp),
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-56).dp)
                .graphicsLayer { translationY = 18.dp.toPx() * (1f - presenca.value) },
        ) {
            Text(
                text = formatarDuracao(capture.segundos),
                style = MaterialTheme.typography.displaySmall.copy(fontSize = 46.sp, lineHeight = 50.sp),
                color = Color.White,
            )
            OndaDoMicrofone(gravando) { capture.nivelAgora() }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                BandeiraCircular(language, tamanho = 16.dp)
                // O handoff escreve "o idioma sai da fala". Não sai: não há
                // detector de idioma no app, e o que existe é a regra de fallback
                // do próprio handoff — cai no curso aberto no hub. A frase diz o
                // que de fato acontece, e é ela que precisa mudar no dia em que
                // houver detecção.
                Text(
                    text = "vai para o curso de ${language.displayName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.72f),
                )
            }
        }

        AcoesDaGravacao(
            aoDescartar = cancelar,
            aoGuardar = guardar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
                .graphicsLayer { translationY = 26.dp.toPx() * (1f - presenca.value) },
        )
    }
}

/**
 * Barra o toque no que está debaixo desta tela, sem consumir nada.
 *
 * Estar no caminho basta: o teste de toque do Compose para no filho mais acima
 * que é atingido, então um `pointerInput` de tela cheia já impede que o `+` e a
 * barra — que continuam compostos debaixo de uma superfície opaca — recebam
 * qualquer coisa.
 *
 * Consumir, além disso, era ativamente nocivo: um pai que consome o evento depois
 * de o filho vê-lo parece inofensivo, mas os gestos do Compose desistem quando
 * encontram uma mudança já consumida. Barrar é trabalho do teste de toque;
 * consumir é do gesto que de fato quer o evento.
 */
private fun Modifier.barraToques(): Modifier = pointerInput(Unit) {
    awaitEachGesture { awaitFirstDown(requireUnconsumed = false) }
}

/**
 * Cores da gravação: a tela é escura nos dois temas, e as cores são as claras.
 *
 * `colorScheme.tertiary` é o verde escuro no tema claro — legível sobre branco e
 * quase invisível sobre este fundo. Aqui o par é sempre o mesmo: menta clara e
 * salmão para o que se lê, e o verde cheio para o botão de guardar, que leva
 * texto branco por cima.
 *
 * O vermelho merece uma linha. A regra do app é que ele é **categoria** — a foto —
 * e nunca erro nem ação, para que uma foto na fila não pareça uma foto com
 * problema. Esta tela abre a única exceção e a paga: aqui o salmão quer dizer
 * descartar, e aqui não existe alvo de foto nenhum — os dois sentidos nunca
 * dividem a tela. Puxar o `error` do tema para cá criaria um segundo vermelho
 * quase igual ao primeiro, que é o jeito garantido de tornar os dois ilegíveis.
 */
private val MENTA_NA_NOITE = VocabuColors.Mint
private val SALMAO_NA_NOITE = VocabuColors.PapagaioEscuro
private val VERDE_CHEIO = VocabuColors.MintDark

/**
 * O selo de "está gravando", no alto.
 *
 * Mora longe das ações de propósito: em cima está o que a tela **é**, embaixo o
 * que ela faz. O ponto pulsa porque é o único elemento da tela que prova que o
 * microfone está aberto agora — o relógio correndo diz o mesmo, mas o olho que dá
 * uma conferida rápida no aparelho apoiado na mesa não lê números.
 */
@Composable
private fun SeloDeGravando(gravando: Boolean, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .pilulaEscura()
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Box(
            Modifier
                .requiredSize(8.dp)
                .respirando(ativo = gravando, minimo = 0.25f)
                .drawBehind { drawCircle(SALMAO_NA_NOITE) },
        )
        Text(
            text = "gravando",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.86f),
        )
    }
}

/** O fundo translúcido do selo — uma pílula de 8% de branco sobre a noite. */
private fun Modifier.pilulaEscura(): Modifier = drawBehind {
    drawRoundRect(Color.White, alpha = 0.08f, cornerRadius = CornerRadius(size.height / 2f))
}

/** Altura e canto dos dois botões da base. */
private val ALTURA_DA_ACAO = 68.dp
private val CANTO_DA_ACAO = 22.dp

/**
 * Descartar à esquerda, guardar à direita, um toque cada.
 *
 * **Os dois não têm o mesmo tamanho, e é isso que sustenta a opção.** Dois alvos
 * na base de uma tela que se olha de relance convidam ao toque errado; o que
 * separa um do outro aqui não é a cor, é a área. Guardar ocupa o que sobra da
 * largura e é o único preenchido — o polegar acha sem mirar. Descartar é estreito,
 * só contorno, e mora no canto onde o dedo não cai por acaso. A assimetria é a
 * frequência escrita em layout: quase toda gravação é para ficar.
 *
 * Nenhum dos dois pede confirmação. Guardar por engano se desfaz em Pendentes, e
 * o aviso de 5 s já leva até lá; descartar é o próprio pedido de descarte, e a
 * regra do app é que nada some sem alguém pedir — perguntar "tem certeza?" depois
 * de a pessoa ter escolhido o alvo pequeno seria cobrar duas vezes pela mesma
 * decisão.
 */
@Composable
private fun AcoesDaGravacao(
    aoDescartar: () -> Unit,
    aoGuardar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxWidth().height(ALTURA_DA_ACAO),
    ) {
        BotaoDeDescartar(aoDescartar)
        BotaoDeGuardar(aoGuardar)
    }
}

@Composable
private fun BotaoDeDescartar(aoClicar: () -> Unit) {
    val toque = lembrarToque()
    Surface(
        onClick = aoClicar,
        shape = RoundedCornerShape(CANTO_DA_ACAO),
        color = Color.Transparent,
        contentColor = SALMAO_NA_NOITE,
        border = BorderStroke(1.dp, SALMAO_NA_NOITE.copy(alpha = 0.45f)),
        interactionSource = toque,
        modifier = Modifier
            .width(86.dp)
            .fillMaxHeight()
            .encolheAoTocar(toque, minimo = 0.94f),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterVertically),
            modifier = Modifier.fillMaxSize(),
        ) {
            Icon(Icones.Lixeira, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(
                text = "Descartar",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun RowScope.BotaoDeGuardar(aoClicar: () -> Unit) {
    val toque = lembrarToque()
    Surface(
        onClick = aoClicar,
        shape = RoundedCornerShape(CANTO_DA_ACAO),
        color = VERDE_CHEIO,
        contentColor = Color.White,
        interactionSource = toque,
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            // Menos que os cartões (0,97): num alvo deste tamanho a mesma
            // proporção vira solavanco em vez de toque.
            .encolheAoTocar(toque, minimo = 0.985f),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp, Alignment.CenterHorizontally),
            modifier = Modifier.fillMaxSize(),
        ) {
            Icon(Icones.Check, contentDescription = null, modifier = Modifier.size(22.dp))
            Text(text = "Guardar", style = MaterialTheme.typography.titleMedium)
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
private fun OndaDoMicrofone(gravando: Boolean, level: () -> Float) {
    val historico = remember { FloatArray(BARRAS_DA_ONDA) }
    var cursor by remember { mutableIntStateOf(0) }

    LaunchedEffect(gravando) {
        if (!gravando) return@LaunchedEffect
        historico.fill(0f)
        cursor = 0
        while (true) {
            historico[cursor % BARRAS_DA_ONDA] = level()
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
                        color = MENTA_NA_NOITE,
                        topLeft = Offset(x, (size.height - altura) / 2f),
                        size = Size(largura, altura),
                        cornerRadius = CornerRadius(largura / 2f),
                        alpha = 1f - 0.5f * (i.toFloat() / BARRAS_DA_ONDA),
                    )
                }
            },
    )
}
