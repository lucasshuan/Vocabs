package com.jean.vocabs.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * O anel de rosca: uma fração desenhada como arco, com o número dentro.
 *
 * Serve à força média do Início e ao estoque de palavras do Progresso, que são a
 * mesma forma com conteúdos diferentes — daí o miolo ser um slot em vez de um
 * texto formatado aqui dentro.
 *
 * O arco se preenche do topo até o valor quando a tela abre. É a animação mais
 * longa do app ([Movimento.AMPLO]) e a que menos custa: ninguém espera por ela —
 * o número do miolo já está legível no primeiro quadro, e o anel é o que confirma
 * o que ele diz. A fração é lida dentro do `Canvas`, então cada quadro invalida o
 * desenho e nada mais.
 */
@Composable
fun ProgressRing(
    fraction: Float,
    modifier: Modifier = Modifier,
    size: Dp = 74.dp,
    thickness: Dp = 8.dp,
    color: Color = MaterialTheme.colorScheme.tertiary,
    core: @Composable ColumnScope.() -> Unit,
) {
    val track = MaterialTheme.colorScheme.outlineVariant
    val animated = animatedFraction(fraction, "arcoDoAnel")
    Box(contentAlignment = Alignment.Center, modifier = modifier.size(size)) {
        Canvas(Modifier.fillMaxSize()) {
            val line = Stroke(thickness.toPx(), cap = StrokeCap.Round)
            drawArc(track, -90f, 360f, false, style = line)
            drawArc(color, -90f, 360f * animated.value, false, style = line)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, content = core)
    }
}

/**
 * Um dia da faixa da semana.
 *
 * [futuro] é separado de "sem revisão" de propósito: sábado sem nada é um dia
 * vazio, domingo que ainda não chegou não é falha nenhuma, e pintá-los igual
 * transformaria a semana inteira num boletim de dias perdidos toda segunda.
 */
data class WeekDay(
    val abbreviation: String,
    val number: Int,
    val reviews: Int,
    val today: Boolean,
    val future: Boolean,
)

/**
 * Os sete dias da semana corrente, com hoje em ameixa e o que já foi em menta.
 *
 * Aparece no cartão de Progresso e no topo do Dia a dia — a mesma faixa, para
 * que passar de uma tela para a outra não pareça trocar de assunto.
 *
 * Os sete dias entram da segunda para o domingo, um logo atrás do outro. O
 * escalonamento aqui não é enfeite: ele desenha na tela a direção em que a semana
 * se lê, e como o passo é de 34 ms, o domingo chega 170 ms depois da segunda —
 * antes de a pessoa ter terminado de olhar para o primeiro quadrado.
 *
 * [tracejada] é a mesma semana de um curso que ainda não tem palavra nenhuma: a
 * anatomia não muda, os quadrados é que ficam só com o contorno. Hoje continua
 * marcado — é a única data que existe antes de haver histórico.
 */
@Composable
fun WeekStrip(
    days: List<WeekDay>,
    modifier: Modifier = Modifier,
    dashed: Boolean = false,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = modifier.fillMaxWidth()) {
        days.forEachIndexed { index, day ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f).smoothEntrance(index, offset = 8.dp),
            ) {
                Text(
                    text = if (day.today) "hoje" else day.abbreviation,
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
                    color = when {
                        day.today -> MaterialTheme.colorScheme.primary
                        dashed || day.future -> MaterialTheme.colorScheme.outline
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                if (dashed && !day.today) EmptySquare() else DaySquare(day)
            }
        }
    }
}

/**
 * O dia de uma semana que ainda não tem histórico: contorno e mais nada.
 *
 * Sem o número, de propósito. Um "27" apagado dentro de um quadrado vazio é a
 * data de um dia em que nada aconteceu, e sete deles em fileira se leem como uma
 * semana perdida — que é justamente o que ninguém precisa ver ao abrir um curso
 * que começou hoje.
 */
@Composable
private fun EmptySquare() {
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .dashedOutline(MaterialTheme.colorScheme.outline, radius = 12.dp),
    )
}

@Composable
private fun DaySquare(day: WeekDay) {
    val colors = MaterialTheme.colorScheme
    // Dois tons de menta e não um gradiente: a faixa tem sete quadrados de 40 dp,
    // e uma escala fina neles não é legível — o que precisa ficar claro é
    // "trabalhei" contra "trabalhei bastante".
    val targetBackground = when {
        day.today -> colors.secondaryContainer
        day.future -> colors.surfaceVariant
        day.reviews >= FULL_DAY_REVIEWS -> colors.tertiary
        day.reviews > 0 -> colors.tertiaryContainer
        else -> colors.outlineVariant
    }
    // O quadrado de hoje muda de cor no meio da sessão, quando a terceira revisão
    // o leva de menta clara a menta forte. A transição é o que faz esse degrau
    // ser notado: repintado de um quadro para o outro, ele só aparece na próxima
    // vez que alguém vier olhar a semana.
    val background by animateColorAsState(targetBackground, tween(Motion.DEFAULT), label = "fundoDoDia")
    val text = when {
        day.today -> colors.primary
        day.future -> colors.outline
        day.reviews >= FULL_DAY_REVIEWS -> colors.onTertiary
        day.reviews > 0 -> colors.onTertiaryContainer
        else -> colors.onSurfaceVariant
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(background, RoundedCornerShape(12.dp))
            .then(
                if (day.today) Modifier.border(2.dp, colors.primary, RoundedCornerShape(12.dp)) else Modifier,
            )
            .semantics {
                contentDescription = when {
                    day.future -> "dia ${day.number}, ainda não chegou"
                    day.reviews == 0 -> "dia ${day.number}, sem revisões"
                    day.reviews == 1 -> "dia ${day.number}, 1 revisão"
                    else -> "dia ${day.number}, ${day.reviews} revisões"
                }
            },
    ) {
        Text(
            text = day.number.toString(),
            style = MaterialTheme.typography.bodySmall,
            color = text,
            textAlign = TextAlign.Center,
        )
    }
}

/** A partir daqui o dia ganha a menta forte. É o piso da carga diária em regime. */
private const val FULL_DAY_REVIEWS = 3

/**
 * A barra que reparte um total em faixas proporcionais — dominadas, familiares e
 * aprendendo, lado a lado.
 *
 * Faixas de peso zero somem em vez de virarem um fio de 1 px: um traço sem
 * largura útil só diz que existe uma categoria vazia, e a legenda ao lado já diz.
 *
 * Ela se desenha da esquerda para a direita ao abrir, no mesmo tempo do anel logo
 * acima — os dois falam do mesmo estoque, e crescerem juntos é o que diz isso. O
 * traçado é `scaleX` num `graphicsLayer`, e não largura animada: assim as três
 * faixas são medidas uma vez só, e a animação fica inteira na fase de desenho.
 */
@Composable
fun BandBars(strips: List<Pair<Int, Color>>, modifier: Modifier = Modifier, height: Dp = 8.dp) {
    val visible = strips.filter { it.first > 0 }
    val stroke = animatedFraction(1f, "tracadoDaBarra")
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .graphicsLayer {
                scaleX = stroke.value
                transformOrigin = TransformOrigin(0f, 0.5f)
            },
    ) {
        if (visible.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxSize().background(MaterialTheme.colorScheme.outlineVariant, CircleShape))
            return@Row
        }
        visible.forEach { (peso, color) ->
            Box(Modifier.weight(peso.toFloat()).fillMaxSize().background(color, CircleShape))
        }
    }
}

/**
 * O cabeçalho das páginas de dentro: voltar e título, na mesma linha.
 *
 * Progresso, Dia a dia, O que falta, Configurações e Novo idioma abrem por cima
 * da aba e voltam para ela — sem esta seta elas seriam becos, porque a barra de
 * baixo continua marcando a aba de origem e não tem como desfazer a entrada.
 */
@Composable
fun InnerHeader(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    end: (@Composable () -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        CircularButton(
            icon = AppIcons.Back,
            contentDescription = "Voltar",
            onClick = onBack,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.weight(1f).padding(start = 4.dp),
        )
        end?.invoke()
    }
}

/**
 * A linha de consumo mensal de IA.
 *
 * Está na tela Você e na de Progresso, e é a mesma coisa nas duas: um contador
 * informativo, sem consequência nenhuma quando estoura — não é quota de
 * segurança, e por isso a barra é de apoio e não um alerta.
 */
@Composable
fun AiUsageRow(used: Int, limit: Int, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val fraction by animatedFraction(
        target = used.toFloat() / limit.coerceAtLeast(1),
        label = "fracaoDeUsoDeIa",
    )
    ListRow(
        title = "Gerações por IA",
        detail = "$used de $limit este mês",
        modifier = modifier,
        start = { IconDisc(AppIcons.Brightness, null, color = colors.primary, background = colors.primaryContainer) },
        end = {
            Box(
                Modifier
                    .width(52.dp)
                    .height(6.dp)
                    .background(colors.outlineVariant, CircleShape),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(fraction)
                        .height(6.dp)
                        .background(colors.primary, CircleShape),
                )
            }
        },
    )
}

/** O rótulo de contagem discreto do canto de uma seção ("37 idiomas", "Todas · 24"). */
@Composable
fun SectionCount(text: String, modifier: Modifier = Modifier) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = modifier) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}
