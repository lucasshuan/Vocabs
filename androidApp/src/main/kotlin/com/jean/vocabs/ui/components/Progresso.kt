package com.jean.vocabs.ui.components

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
    Box(contentAlignment = Alignment.Center, modifier = modifier.size(tamanho)) {
        Canvas(Modifier.fillMaxSize()) {
            val traco = Stroke(espessura.toPx(), cap = StrokeCap.Round)
            drawArc(trilha, -90f, 360f, false, style = traco)
            drawArc(cor, -90f, 360f * fracao.coerceIn(0f, 1f), false, style = traco)
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
    val revisoes: Int,
    val hoje: Boolean,
    val futuro: Boolean,
)

/**
 * Os sete dias da semana corrente, com hoje em ameixa e o que já foi em menta.
 *
 * Aparece no cartão de Progresso e no topo do Dia a dia — a mesma faixa, para
 * que passar de uma tela para a outra não pareça trocar de assunto.
 */
@Composable
fun FaixaDaSemana(dias: List<DiaDaSemana>, modifier: Modifier = Modifier) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = modifier.fillMaxWidth()) {
        dias.forEach { dia ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = if (dia.hoje) "hoje" else dia.sigla,
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
                    color = when {
                        dia.hoje -> MaterialTheme.colorScheme.primary
                        dia.futuro -> MaterialTheme.colorScheme.outline
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                QuadradoDoDia(dia)
            }
        }
    }
}

@Composable
private fun QuadradoDoDia(dia: DiaDaSemana) {
    val cores = MaterialTheme.colorScheme
    // Dois tons de menta e não um gradiente: a faixa tem sete quadrados de 40 dp,
    // e uma escala fina neles não é legível — o que precisa ficar claro é
    // "trabalhei" contra "trabalhei bastante".
    val fundo = when {
        dia.hoje -> cores.secondaryContainer
        dia.futuro -> cores.surfaceVariant
        dia.revisoes >= REVISOES_DE_DIA_CHEIO -> cores.tertiary
        dia.revisoes > 0 -> cores.tertiaryContainer
        else -> cores.outlineVariant
    }
    val texto = when {
        dia.hoje -> cores.primary
        dia.futuro -> cores.outline
        dia.revisoes >= REVISOES_DE_DIA_CHEIO -> cores.onTertiary
        dia.revisoes > 0 -> cores.onTertiaryContainer
        else -> cores.onSurfaceVariant
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(fundo, RoundedCornerShape(12.dp))
            .then(
                if (dia.hoje) Modifier.border(2.dp, cores.primary, RoundedCornerShape(12.dp)) else Modifier,
            )
            .semantics {
                contentDescription = when {
                    dia.futuro -> "dia ${dia.numero}, ainda não chegou"
                    dia.revisoes == 0 -> "dia ${dia.numero}, sem revisões"
                    dia.revisoes == 1 -> "dia ${dia.numero}, 1 revisão"
                    else -> "dia ${dia.numero}, ${dia.revisoes} revisões"
                }
            },
    ) {
        Text(
            text = dia.numero.toString(),
            style = MaterialTheme.typography.bodySmall,
            color = texto,
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
 */
@Composable
fun BarraDeFaixas(faixas: List<Pair<Int, Color>>, modifier: Modifier = Modifier, altura: Dp = 8.dp) {
    val visiveis = faixas.filter { it.first > 0 }
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier.fillMaxWidth().height(altura),
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
    titulo: String,
    aoVoltar: () -> Unit,
    modifier: Modifier = Modifier,
    fim: (@Composable () -> Unit)? = null,
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
            text = titulo,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.weight(1f).padding(start = 4.dp),
        )
        fim?.invoke()
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
fun LinhaDeUsoDeIa(usadas: Int, limite: Int, modifier: Modifier = Modifier) {
    val fracao = (usadas.toFloat() / limite.coerceAtLeast(1)).coerceIn(0f, 1f)
    LinhaDeLista(
        titulo = "Gerações por IA",
        detalhe = "$usadas de $limite este mês",
        modifier = modifier,
        fim = {
            Box(
                Modifier
                    .width(52.dp)
                    .height(6.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant, CircleShape),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(fracao)
                        .height(6.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                )
            }
        },
    )
}

/** O rótulo de contagem discreto do canto de uma seção ("37 idiomas", "Todas · 24"). */
@Composable
fun ContagemDeSecao(texto: String, modifier: Modifier = Modifier) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = modifier) {
        Text(
            text = texto,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}
