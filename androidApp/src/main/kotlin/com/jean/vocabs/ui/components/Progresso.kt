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
fun AnelDeProgresso(
    fracao: Float,
    modifier: Modifier = Modifier,
    tamanho: Dp = 74.dp,
    espessura: Dp = 8.dp,
    cor: Color = MaterialTheme.colorScheme.tertiary,
    miolo: @Composable ColumnScope.() -> Unit,
) {
    val trilha = MaterialTheme.colorScheme.outlineVariant
    val animada = fracaoAnimada(fracao, "arcoDoAnel")
    Box(contentAlignment = Alignment.Center, modifier = modifier.size(tamanho)) {
        Canvas(Modifier.fillMaxSize()) {
            val traco = Stroke(espessura.toPx(), cap = StrokeCap.Round)
            drawArc(trilha, -90f, 360f, false, style = traco)
            drawArc(cor, -90f, 360f * animada.value, false, style = traco)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, content = miolo)
    }
}

/**
 * Um dia da faixa da semana.
 *
 * [futuro] é separado de "sem revisão" de propósito: sábado sem nada é um dia
 * vazio, domingo que ainda não chegou não é falha nenhuma, e pintá-los igual
 * transformaria a semana inteira num boletim de dias perdidos toda segunda.
 */
data class DiaDaSemana(
    val sigla: String,
    val numero: Int,
    val reviews: Int,
    val today: Boolean,
    val futuro: Boolean,
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
fun FaixaDaSemana(
    days: List<DiaDaSemana>,
    modifier: Modifier = Modifier,
    tracejada: Boolean = false,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = modifier.fillMaxWidth()) {
        days.forEachIndexed { indice, day ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f).entradaSuave(indice, deslocamento = 8.dp),
            ) {
                Text(
                    text = if (day.today) "hoje" else day.sigla,
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
                    color = when {
                        day.today -> MaterialTheme.colorScheme.primary
                        tracejada || day.futuro -> MaterialTheme.colorScheme.outline
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                if (tracejada && !day.today) QuadradoVazio() else QuadradoDoDia(day)
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
private fun QuadradoVazio() {
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .contornoTracejado(MaterialTheme.colorScheme.outline, raio = 12.dp),
    )
}

@Composable
private fun QuadradoDoDia(day: DiaDaSemana) {
    val cores = MaterialTheme.colorScheme
    // Dois tons de menta e não um gradiente: a faixa tem sete quadrados de 40 dp,
    // e uma escala fina neles não é legível — o que precisa ficar claro é
    // "trabalhei" contra "trabalhei bastante".
    val fundoAlvo = when {
        day.today -> cores.secondaryContainer
        day.futuro -> cores.surfaceVariant
        day.reviews >= REVISOES_DE_DIA_CHEIO -> cores.tertiary
        day.reviews > 0 -> cores.tertiaryContainer
        else -> cores.outlineVariant
    }
    // O quadrado de hoje muda de cor no meio da sessão, quando a terceira revisão
    // o leva de menta clara a menta forte. A transição é o que faz esse degrau
    // ser notado: repintado de um quadro para o outro, ele só aparece na próxima
    // vez que alguém vier olhar a semana.
    val fundo by animateColorAsState(fundoAlvo, tween(Movimento.PADRAO), label = "fundoDoDia")
    val text = when {
        day.today -> cores.primary
        day.futuro -> cores.outline
        day.reviews >= REVISOES_DE_DIA_CHEIO -> cores.onTertiary
        day.reviews > 0 -> cores.onTertiaryContainer
        else -> cores.onSurfaceVariant
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(fundo, RoundedCornerShape(12.dp))
            .then(
                if (day.today) Modifier.border(2.dp, cores.primary, RoundedCornerShape(12.dp)) else Modifier,
            )
            .semantics {
                contentDescription = when {
                    day.futuro -> "dia ${day.numero}, ainda não chegou"
                    day.reviews == 0 -> "dia ${day.numero}, sem revisões"
                    day.reviews == 1 -> "dia ${day.numero}, 1 revisão"
                    else -> "dia ${day.numero}, ${day.reviews} revisões"
                }
            },
    ) {
        Text(
            text = day.numero.toString(),
            style = MaterialTheme.typography.bodySmall,
            color = text,
            textAlign = TextAlign.Center,
        )
    }
}

/** A partir daqui o dia ganha a menta forte. É o piso da carga diária em regime. */
private const val REVISOES_DE_DIA_CHEIO = 3

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
fun BarraDeFaixas(faixas: List<Pair<Int, Color>>, modifier: Modifier = Modifier, altura: Dp = 8.dp) {
    val visiveis = faixas.filter { it.first > 0 }
    val tracado = fracaoAnimada(1f, "tracadoDaBarra")
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(altura)
            .graphicsLayer {
                scaleX = tracado.value
                transformOrigin = TransformOrigin(0f, 0.5f)
            },
    ) {
        if (visiveis.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxSize().background(MaterialTheme.colorScheme.outlineVariant, CircleShape))
            return@Row
        }
        visiveis.forEach { (peso, cor) ->
            Box(Modifier.weight(peso.toFloat()).fillMaxSize().background(cor, CircleShape))
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
fun CabecalhoDeDentro(
    title: String,
    aoVoltar: () -> Unit,
    modifier: Modifier = Modifier,
    end: (@Composable () -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        BotaoCircular(
            icone = Icones.Voltar,
            descricao = "Voltar",
            aoClicar = aoVoltar,
            cor = MaterialTheme.colorScheme.onSurface,
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
fun LinhaDeUsoDeIa(used: Int, limit: Int, modifier: Modifier = Modifier) {
    val cores = MaterialTheme.colorScheme
    val fracao by fracaoAnimada(
        target = used.toFloat() / limit.coerceAtLeast(1),
        rotulo = "fracaoDeUsoDeIa",
    )
    LinhaDeLista(
        title = "Gerações por IA",
        detail = "$used de $limit este mês",
        modifier = modifier,
        start = { DiscoDeIcone(Icones.Brilho, null, cor = cores.primary, fundo = cores.primaryContainer) },
        end = {
            Box(
                Modifier
                    .width(52.dp)
                    .height(6.dp)
                    .background(cores.outlineVariant, CircleShape),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(fracao)
                        .height(6.dp)
                        .background(cores.primary, CircleShape),
                )
            }
        },
    )
}

/** O rótulo de contagem discreto do canto de uma seção ("37 idiomas", "Todas · 24"). */
@Composable
fun ContagemDeSecao(text: String, modifier: Modifier = Modifier) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = modifier) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}
