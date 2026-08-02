package com.jean.vocabs.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

/** Uma aba da barra: ícone só, com um ponto embaixo quando ativa. */
data class Aba(
    val rota: String,
    val icone: ImageVector,
    val rotulo: String,
    val selo: Int = 0,
)

private val VERMELHO_GRAVACAO = Color(0xFFE5484D)

/** O quanto o botão central escapa por cima da barra. Ver [BarraInferior]. */
private val SALTO_DO_BOTAO = 14.dp

/** Onde o centro do botão fica, medido do topo desta barra. */
internal val CENTRO_DO_BOTAO_DO_TOPO = 35.dp

/**
 * A barra inferior: quatro abas e, no meio, o botão que captura.
 *
 * Encosta nas bordas e no rodapé em vez de flutuar como uma pílula: ela é a
 * moldura fixa do app, e uma moldura que paira parece um cartão a mais no meio
 * dos cartões das telas. Retangular, sem canto arredondado nenhum — qualquer
 * curva no topo redesenharia a silhueta de um cartão, que é justamente o que ela
 * não é. A separação do conteúdo fica por conta da sombra.
 *
 * O fundo desce por baixo da barra de gestos do sistema, e é o **conteúdo** que
 * recebe o inset. Sem isso sobraria uma faixa da cor da tela entre a barra e a
 * borda inferior, que é exatamente o "fechadinho" que se quer evitar.
 *
 * O botão central é a única coisa daqui que não navega — abre um leque com os
 * três jeitos de capturar. Fica no centro do rodapé porque é o alvo mais fácil
 * de acertar com o polegar de qualquer mão, e a captura é o que o app precisa
 * que não tenha atrito nenhum (princípio 1 do produto).
 */
@Composable
fun BarraInferior(
    abasEsquerda: List<Aba>,
    abasDireita: List<Aba>,
    rotaAtual: String?,
    lequeAberto: Boolean,
    gravando: Boolean,
    segundosGravados: Long,
    aoNavegar: (String) -> Unit,
    aoAlternarLeque: () -> Unit,
    aoPararGravacao: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cores = MaterialTheme.colorScheme

    Box(modifier = modifier.fillMaxWidth()) {
        Surface(
            shape = RectangleShape,
            color = cores.surface,
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                // Reserva a faixa por onde o botão escapa.
                .padding(top = SALTO_DO_BOTAO),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .height(64.dp),
            ) {
                abasEsquerda.forEach { aba ->
                    ItemDeAba(
                        aba = aba,
                        selecionada = rotaAtual == aba.rota,
                        aoClicar = { aoNavegar(aba.rota) },
                        modifier = Modifier.weight(1f),
                    )
                }
                // O vão do botão central. Existe como espaço reservado para que
                // as quatro abas fiquem simétricas em volta dele.
                Box(modifier = Modifier.weight(1f))
                abasDireita.forEach { aba ->
                    ItemDeAba(
                        aba = aba,
                        selecionada = rotaAtual == aba.rota,
                        aoClicar = { aoNavegar(aba.rota) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        BotaoCentral(
            aberto = lequeAberto,
            gravando = gravando,
            segundos = segundosGravados,
            aoClicar = { if (gravando) aoPararGravacao() else aoAlternarLeque() },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 6.dp),
        )
    }
}

@Composable
private fun ItemDeAba(
    aba: Aba,
    selecionada: Boolean,
    aoClicar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cores = MaterialTheme.colorScheme
    val cor by animateColorAsState(
        targetValue = if (selecionada) cores.primary else cores.onSurfaceVariant,
        animationSpec = tween(220),
        label = "corAba",
    )
    // O ponto cresce em vez de aparecer: um alvo que pisca chama mais atenção
    // que a troca de tela, e a troca é que importa.
    val ponto by animateDpAsState(
        targetValue = if (selecionada) 5.dp else 0.dp,
        animationSpec = tween(220),
        label = "pontoAba",
    )

    Surface(
        onClick = aoClicar,
        color = Color.Transparent,
        shape = RoundedCornerShape(18.dp),
        modifier = modifier.height(62.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box {
                Icon(
                    imageVector = aba.icone,
                    contentDescription = aba.rotulo,
                    tint = cor,
                    modifier = Modifier.size(23.dp),
                )
                SeloDeContagem(
                    quantidade = aba.selo,
                    modifier = Modifier.align(Alignment.TopEnd),
                )
            }
            Box(
                modifier = Modifier
                    .padding(top = 5.dp)
                    .size(ponto)
                    .background(cor, CircleShape),
            )
        }
    }
}

/**
 * Três estados no mesmo alvo: fechado, aberto e gravando.
 *
 * Gravar não abre outra tela nem move o botão — o próprio hexágono vira
 * vermelho, mostra o cronômetro e passa a ser o "parar". O dedo já está ali.
 */
@Composable
private fun BotaoCentral(
    aberto: Boolean,
    gravando: Boolean,
    segundos: Long,
    aoClicar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cores = MaterialTheme.colorScheme
    val interacao = remember { MutableInteractionSource() }

    val fundo by animateColorAsState(
        targetValue = when {
            gravando -> VERMELHO_GRAVACAO
            aberto -> cores.inverseSurface
            else -> cores.primary
        },
        animationSpec = tween(260),
        label = "fundoBotaoCentral",
    )
    // Acompanha o fundo: no tema escuro o `inverseSurface` é claro, e um ícone
    // branco fixo sumiria dentro dele quando o leque abrisse.
    val tinta by animateColorAsState(
        targetValue = when {
            gravando -> Color.White
            aberto -> cores.inverseOnSurface
            else -> cores.onPrimary
        },
        animationSpec = tween(260),
        label = "tintaBotaoCentral",
    )
    // 45° transforma o "+" em "×" sem trocar de ícone: a mesma forma diz
    // "abrir" e "fechar", e o giro conta a transição entre as duas.
    val giro by animateFloatAsState(
        targetValue = if (aberto) 45f else 0f,
        animationSpec = tween(280),
        label = "giroBotaoCentral",
    )
    val escala by animateFloatAsState(
        targetValue = if (aberto || gravando) 1.06f else 1f,
        animationSpec = tween(280),
        label = "escalaBotaoCentral",
    )

    Box(contentAlignment = Alignment.BottomCenter, modifier = modifier) {
        AnimatedVisibility(
            visible = gravando,
            enter = fadeIn(tween(200)) + scaleIn(tween(200)),
            exit = fadeOut(tween(120)) + scaleOut(tween(120)),
            modifier = Modifier.offset(y = (-72).dp),
        ) {
            Surface(shape = RoundedCornerShape(50), color = VERMELHO_GRAVACAO) {
                Text(
                    text = formatarCronometro(segundos),
                    style = MaterialTheme.typography.labelMedium
                        .copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                )
            }
        }

        Surface(
            onClick = aoClicar,
            shape = CircleShape,
            color = fundo,
            shadowElevation = 10.dp,
            interactionSource = interacao,
            modifier = Modifier
                .size(58.dp)
                .graphicsLayer {
                    scaleX = escala
                    scaleY = escala
                },
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (gravando) Icones.Parar else Icones.Mais,
                    contentDescription = when {
                        gravando -> "Parar gravação"
                        aberto -> "Fechar"
                        else -> "Capturar"
                    },
                    tint = tinta,
                    modifier = Modifier
                        .size(if (gravando) 20.dp else 26.dp)
                        .rotate(if (gravando) 0f else giro),
                )
            }
        }
    }
}

/**
 * As três formas de capturar, abrindo em leque a partir do botão central.
 *
 * Em arco e não em lista: saindo do mesmo ponto em que o dedo tocou, o
 * movimento explica de onde as opções vieram, e as três caem à mesma distância
 * do polegar — numa coluna, a de cima ficaria bem mais longe que a de baixo.
 */
@Composable
fun LequeDeCaptura(
    aberto: Boolean,
    aoGravarAudio: () -> Unit,
    aoTirarFoto: () -> Unit,
    aoEscrever: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cores = MaterialTheme.colorScheme

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        OpcaoDoLeque(
            aberto = aberto,
            ordem = 0,
            anguloGraus = 152f,
            rotulo = "Áudio",
            icone = Icones.Microfone,
            cor = cores.primary,
            corConteudo = cores.onPrimary,
            aoClicar = aoGravarAudio,
        )
        OpcaoDoLeque(
            aberto = aberto,
            ordem = 1,
            anguloGraus = 90f,
            rotulo = "Foto",
            icone = Icones.Camera,
            cor = cores.tertiary,
            corConteudo = cores.onTertiary,
            aoClicar = aoTirarFoto,
        )
        OpcaoDoLeque(
            aberto = aberto,
            ordem = 2,
            anguloGraus = 28f,
            rotulo = "Escrever",
            // Lápis e não "+": o botão central já é o mais, e repetir o mesmo
            // símbolo dentro do menu que ele abriu não diz nada.
            icone = Icones.Lapis,
            cor = cores.inverseSurface,
            corConteudo = cores.inverseOnSurface,
            aoClicar = aoEscrever,
        )
    }
}

@Composable
private fun OpcaoDoLeque(
    aberto: Boolean,
    ordem: Int,
    anguloGraus: Float,
    rotulo: String,
    icone: ImageVector,
    cor: Color,
    corConteudo: Color,
    aoClicar: () -> Unit,
) {
    // Abre escalonado, fecha junto: a entrada em cascata mostra que são três
    // coisas distintas; a saída em cascata só faria esperar.
    val progresso by animateFloatAsState(
        targetValue = if (aberto) 1f else 0f,
        animationSpec = if (aberto) {
            tween(durationMillis = 300, delayMillis = ordem * 55)
        } else {
            tween(durationMillis = 160)
        },
        label = "progressoOpcao$ordem",
    )

    if (progresso < 0.01f) return

    val radianos = Math.toRadians(anguloGraus.toDouble())
    val raio = 104f
    val interacao = remember { MutableInteractionSource() }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .offset(
                x = (cos(radianos).toFloat() * raio * progresso).dp,
                y = (-sin(radianos).toFloat() * raio * progresso).dp,
            )
            .graphicsLayer {
                alpha = progresso
                scaleX = progresso
                scaleY = progresso
            },
    ) {
        Surface(
            onClick = aoClicar,
            shape = CircleShape,
            color = cor,
            shadowElevation = 8.dp,
            interactionSource = interacao,
            modifier = Modifier.size(54.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icone,
                    contentDescription = rotulo,
                    tint = corConteudo,
                    modifier = Modifier.size(23.dp),
                )
            }
        }
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.9f),
            modifier = Modifier.padding(top = 6.dp),
        ) {
            Text(
                text = rotulo,
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
                color = MaterialTheme.colorScheme.inverseOnSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
    }
}

/**
 * O backlog de pendentes à vista o tempo todo.
 *
 * O documento de métricas trata essa fila como o alerta mais importante do app:
 * se ela só cresce, a captura está andando mais rápido que o processamento.
 */
@Composable
private fun SeloDeContagem(quantidade: Int, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = quantidade > 0,
        enter = scaleIn(tween(240)) + fadeIn(tween(240)),
        exit = scaleOut(tween(160)) + fadeOut(tween(160)),
        modifier = modifier,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .offset(x = 7.dp, y = (-6).dp)
                .defaultMinSize(minWidth = 15.dp, minHeight = 15.dp)
                .background(MaterialTheme.colorScheme.tertiary, CircleShape),
        ) {
            Text(
                text = if (quantidade > 9) "9+" else "$quantidade",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    letterSpacing = 0.sp,
                ),
                color = MaterialTheme.colorScheme.onTertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 3.dp),
            )
        }
    }
}

private fun formatarCronometro(segundos: Long): String =
    "%d:%02d".format(segundos / 60, segundos % 60)
