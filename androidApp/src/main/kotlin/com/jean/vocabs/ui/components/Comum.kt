package com.jean.vocabs.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jean.vocabs.contracts.TipoAlvo
import com.jean.vocabs.shared.domain.Entrada
import com.jean.vocabs.shared.domain.NivelMemoria
import com.jean.vocabs.shared.domain.StatusEntrada

/** Selo PALAVRA (azul) / EXPRESSÃO (laranja), como no mockup. */
@Composable
fun TipoBadge(tipo: TipoAlvo, modifier: Modifier = Modifier) {
    val cores = MaterialTheme.colorScheme
    val (fundo, texto) = when (tipo) {
        TipoAlvo.PALAVRA -> cores.primaryContainer to cores.onPrimaryContainer
        TipoAlvo.EXPRESSAO -> cores.tertiaryContainer to cores.onTertiaryContainer
    }
    Surface(shape = RoundedCornerShape(50), color = fundo, modifier = modifier) {
        Text(
            text = if (tipo == TipoAlvo.PALAVRA) "PALAVRA" else "EXPRESSÃO",
            style = MaterialTheme.typography.labelSmall,
            color = texto,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

/**
 * Anel de status dos cartões: um arco que gira enquanto a ficha está sendo
 * escrita, ou fica parado (na fila) — o mesmo desenho dos anéis do mockup.
 */
@Composable
fun AnelProgresso(
    girando: Boolean,
    cor: Color,
    modifier: Modifier = Modifier,
    tamanho: Dp = 18.dp,
) {
    val trilha = MaterialTheme.colorScheme.surfaceVariant
    val rotacao by if (girando) {
        rememberInfiniteTransition(label = "anel").animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)),
            label = "rotacaoAnel",
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    Canvas(modifier = modifier.size(tamanho)) {
        val traco = Stroke(width = size.width * 0.16f, cap = StrokeCap.Round)
        drawArc(
            color = trilha,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            style = traco,
        )
        drawArc(
            color = cor,
            startAngle = rotacao - 90f,
            sweepAngle = 100f,
            useCenter = false,
            style = traco,
        )
    }
}

/** Pontinho de status do inbox; pulsa suavemente enquanto a ficha é gerada. */
@Composable
fun PontoStatus(cor: Color, pulsando: Boolean, modifier: Modifier = Modifier) {
    val alfa by if (pulsando) {
        rememberInfiniteTransition(label = "ponto").animateFloat(
            initialValue = 1f,
            targetValue = 0.3f,
            animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
            label = "alfaPonto",
        )
    } else {
        remember { mutableFloatStateOf(1f) }
    }
    Box(
        modifier = modifier
            .size(8.dp)
            .graphicsLayer { alpha = alfa }
            .background(cor, CircleShape),
    )
}

/** Encolhe de leve enquanto pressionado — dá peso físico aos cartões. */
@Composable
fun Modifier.escalaAoPressionar(interacao: InteractionSource): Modifier {
    val pressionado by interacao.collectIsPressedAsState()
    val escala by animateFloatAsState(
        targetValue = if (pressionado) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "escalaPressao",
    )
    return graphicsLayer {
        scaleX = escala
        scaleY = escala
    }
}

/** Rótulo de seção em caixa alta espaçada (SIGNIFICADO, INBOX…). */
@Composable
fun RotuloSecao(texto: String, modifier: Modifier = Modifier) {
    Text(
        text = texto,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

@Composable
fun BotaoCircular(
    icone: ImageVector,
    descricao: String,
    aoClicar: () -> Unit,
    cor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Surface(
        onClick = aoClicar,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(44.dp),
        ) {
            Icon(
                imageVector = icone,
                contentDescription = descricao,
                tint = cor,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
fun AvisoDuplicata(entrada: Entrada, modifier: Modifier = Modifier) {
    val cores = MaterialTheme.colorScheme
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = cores.secondaryContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(14.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(34.dp)
                    .background(cores.secondary, CircleShape),
            ) {
                Icon(
                    imageVector = Icones.Repetir,
                    contentDescription = null,
                    tint = cores.onSecondary,
                    modifier = Modifier.size(18.dp),
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
            ) {
                Text(
                    text = "Você já tem isso",
                    style = MaterialTheme.typography.titleSmall,
                    color = cores.onSecondaryContainer,
                )
                Text(
                    text = detalheDuplicata(entrada),
                    style = MaterialTheme.typography.bodySmall,
                    color = cores.onSecondaryContainer.copy(alpha = 0.76f),
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

private fun detalheDuplicata(entrada: Entrada): String = buildString {
    append(entrada.titulo)
    append(" · ")
    append(rotuloStatusDuplicata(entrada.status))
    append(" · ")
    append(tempoRelativo(entrada.criadoEm))
    entrada.origem?.takeIf { it.isNotBlank() }?.let { origem ->
        append(" · ")
        append(origem)
    }
}

private fun rotuloStatusDuplicata(status: StatusEntrada): String = when (status) {
    StatusEntrada.PRONTA -> "ficha pronta"
    StatusEntrada.GERANDO -> "gerando ficha"
    StatusEntrada.PENDENTE -> "na fila"
    StatusEntrada.ERRO -> "com erro"
    StatusEntrada.RASCUNHO -> "no inbox"
}

/** "agora", "há 5min", "há 2h", "ontem", "há 3d". */
fun tempoRelativo(entao: Long, agora: Long = System.currentTimeMillis()): String {
    val minutos = ((agora - entao) / 60_000L).coerceAtLeast(0)
    return when {
        minutos < 1 -> "agora"
        minutos < 60 -> "há ${minutos}min"
        minutos < 60 * 24 -> "há ${minutos / 60}h"
        minutos < 60 * 48 -> "ontem"
        else -> "há ${minutos / (60 * 24)}d"
    }
}

/**
 * O espelho de [tempoRelativo] olhando para a frente: "agora", "em 4h", "amanhã",
 * "em 3 dias".
 *
 * Recebe uma **duração**, não um instante, de propósito. [tempoRelativo] tem um
 * `System.currentTimeMillis()` embutido como default — um relógio da camada de UI
 * que ignora a costura de tempo injetada no repositório. Repetir isso aqui faria
 * a barra da ficha e o cartão da home discordarem sobre que horas são.
 */
fun tempoAte(millis: Long): String {
    val minutos = (millis / 60_000L).coerceAtLeast(0)
    return when {
        minutos < 1 -> "agora"
        minutos < 60 -> "em ${minutos}min"
        minutos < 60 * 24 -> "em ${minutos / 60}h"
        minutos < 60 * 48 -> "amanhã"
        else -> "em ${minutos / (60 * 24)} dias"
    }
}

/**
 * Vermelho → laranja → azul, e não a rampa vermelho-laranja-verde do costume.
 *
 * Dois motivos: o Material 3 não tem papel `success`, então verde entraria como
 * uma cor solta fora do esquema, com variante clara e escura para manter à mão; e
 * vermelho→laranja→verde é justamente a rampa de três cores que falha para
 * daltonismo protan/deutan. Azul já é a cor de confiança deste app.
 *
 * O vermelho aqui só preenche a barra, nunca vira fundo de container — e o rótulo
 * ao lado é sempre positivo ("Aprendendo", não "Fraca").
 */
@Composable
fun corDoNivel(nivel: NivelMemoria): Color = when (nivel) {
    NivelMemoria.NOVA -> MaterialTheme.colorScheme.onSurfaceVariant
    NivelMemoria.APRENDENDO -> MaterialTheme.colorScheme.error
    NivelMemoria.FAMILIAR -> MaterialTheme.colorScheme.tertiary
    NivelMemoria.DOMINADA -> MaterialTheme.colorScheme.primary
}

fun rotuloDoNivel(nivel: NivelMemoria): String = when (nivel) {
    NivelMemoria.NOVA -> "Nova"
    NivelMemoria.APRENDENDO -> "Aprendendo"
    NivelMemoria.FAMILIAR -> "Familiar"
    NivelMemoria.DOMINADA -> "Dominada"
}

/**
 * A força de memória como barra.
 *
 * Não reusa o [AnelProgresso] de propósito: aquele desenha um arco fixo de 100° e
 * não tem fração nenhuma — dar-lhe uma quebraria o caso `girando`, que é o que ele
 * existe para fazer.
 */
@Composable
fun BarraDeMemoria(
    pontos: Double,
    nivel: NivelMemoria,
    modifier: Modifier = Modifier,
) {
    val fracao by animateFloatAsState(
        targetValue = (pontos / 100.0).toFloat().coerceIn(0f, 1f),
        animationSpec = tween(500),
        label = "fracaoMemoria",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fracao)
                .height(8.dp)
                .background(corDoNivel(nivel), RoundedCornerShape(4.dp)),
        )
    }
}
