package com.jean.vocabs.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jean.vocabs.contracts.TargetType
import com.jean.vocabs.shared.domain.Capture
import com.jean.vocabs.shared.domain.Entry
import com.jean.vocabs.shared.domain.CaptureFormat
import com.jean.vocabs.shared.domain.MemoryLevel
import com.jean.vocabs.shared.domain.Retention
import com.jean.vocabs.shared.domain.EntryStatus
import com.jean.vocabs.ui.theme.LocalTemaEscuro

/**
 * Selo "palavra" / "expressão".
 *
 * A expressão é a que ganha o tom de ameixa: dois ou mais tokens é o caso menos
 * frequente e o que o handoff quer que salte na lista. Caixa baixa de propósito
 * — é classificação, não título.
 */
@Composable
fun TypeBadge(type: TargetType, modifier: Modifier = Modifier) {
    val cores = MaterialTheme.colorScheme
    val escuro = LocalTemaEscuro.current
    val (fundo, text) = when (type) {
        TargetType.PHRASE -> cores.secondaryContainer to if (escuro) cores.onSurface else cores.primary
        TargetType.WORD -> cores.surfaceVariant to cores.onSurfaceVariant
    }
    Surface(shape = CircleShape, color = fundo, modifier = modifier) {
        Text(
            text = if (type == TargetType.WORD) "palavra" else "expressão",
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
            color = text,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
        )
    }
}

@Composable
fun DuplicateNotice(entry: Entry, modifier: Modifier = Modifier) {
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
                    imageVector = AppIcons.Repetir,
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
                    text = duplicateDetail(entry),
                    style = MaterialTheme.typography.bodySmall,
                    color = cores.onSecondaryContainer.copy(alpha = 0.76f),
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

private fun duplicateDetail(entry: Entry): String = buildString {
    append(entry.title)
    append(" · ")
    append(duplicateStatusLabel(entry.status))
    append(" · ")
    append(relativeTime(entry.createdAt))
    entry.source?.takeIf { it.isNotBlank() }?.let { source ->
        append(" · ")
        append(source)
    }
}

private fun duplicateStatusLabel(status: EntryStatus): String = when (status) {
    EntryStatus.READY -> "ficha pronta"
    EntryStatus.GENERATING -> "gerando ficha"
    EntryStatus.PENDING -> "na fila"
    EntryStatus.ERROR -> "com erro"
}

/** "agora", "há 5min", "há 2h", "ontem", "há 3d". */
fun relativeTime(entao: Long, now: Long = System.currentTimeMillis()): String {
    val minutos = ((now - entao) / 60_000L).coerceAtLeast(0)
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
fun timeUntil(millis: Long): String {
    val minutos = (millis / 60_000L).coerceAtLeast(0)
    return when {
        minutos < 1 -> "agora"
        minutos < 60 -> "em ${minutos}min"
        minutos < 60 * 24 -> "em ${minutos / 60}h"
        minutos < 60 * 48 -> "amanhã"
        else -> "em ${minutos / (60 * 24)} dias"
    }
}

/** "0:12", "1:03:20" — a duração de um áudio, a partir de millis. */
fun formatDurationMs(durationMs: Long): String {
    val totalSegundos = (durationMs / 1_000).coerceAtLeast(0)
    val horas = totalSegundos / 3_600
    val minutos = (totalSegundos % 3_600) / 60
    val segundos = totalSegundos % 60
    val segundosComZero = segundos.toString().padStart(2, '0')
    return if (horas > 0) "$horas:${minutos.toString().padStart(2, '0')}:$segundosComZero" else "$minutos:$segundosComZero"
}

/** A mesma duração, a partir de segundos — o que o cronômetro da gravação conta. */
fun formatDuration(segundos: Long): String = formatDurationMs(segundos * 1_000L)

/**
 * Como chamar uma captura crua: "Áudio · 0:12", "Foto do Kindle", "“tant pis”".
 *
 * A lista de Pendentes e o cabeçalho da seleção precisam do mesmo nome — quem
 * toca numa linha tem que reconhecer a tela que abriu. É também o único lugar
 * onde `origem` aparece, o que a torna útil de preencher.
 *
 * Texto colado mostra o próprio trecho entre aspas, e não a palavra "Texto":
 * numa fila de cinco capturas, "Texto" três vezes não distingue nada, e o
 * trecho é o que a pessoa reconhece de imediato.
 */
fun captureTitle(capture: Capture): String {
    val source = capture.source?.takeIf { it.isNotBlank() }
    return when (capture.format) {
        CaptureFormat.AUDIO -> capture.durationMs?.let { "Áudio · ${formatDurationMs(it)}" } ?: "Áudio"
        CaptureFormat.PHOTO -> source?.let { "Foto do $it" } ?: "Foto"
        CaptureFormat.TEXT -> capture.snippet?.takeIf { it.isNotBlank() }?.let { "“${summarize(it)}”" }
            ?: source?.let { "Texto do $it" }
            ?: "Texto"
    }
}

/** Uma linha de trecho para caber num título, cortada na palavra e não na letra. */
fun summarize(text: String, limit: Int = 38): String {
    val limpo = text.trim().replace(Regex("\\s+"), " ")
    if (limpo.length <= limit) return limpo
    val corte = limpo.take(limit).substringBeforeLast(' ', limpo.take(limit))
    return "$corte…"
}

/**
 * A ameixa identifica a marca/ações; a menta fica reservada para progresso.
 */
@Composable
fun levelColor(level: MemoryLevel): Color = when (level) {
    MemoryLevel.NEW -> MaterialTheme.colorScheme.onSurfaceVariant
    MemoryLevel.LEARNING -> MaterialTheme.colorScheme.primary
    MemoryLevel.FAMILIAR -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.72f)
    MemoryLevel.MASTERED -> MaterialTheme.colorScheme.tertiary
}

/**
 * O rótulo do nível só ganha menta quando é "dominada".
 *
 * Nos outros níveis ele é informação de apoio e fica em cinza-lilás — quem
 * carrega o estado é a barra ao lado, e dois elementos coloridos dizendo a mesma
 * coisa fariam "familiar" parecer uma conquista.
 */
@Composable
fun levelLabelColor(level: MemoryLevel): Color =
    if (level == MemoryLevel.MASTERED) MaterialTheme.colorScheme.tertiary
    else MaterialTheme.colorScheme.onSurfaceVariant

fun levelLabel(level: MemoryLevel): String = when (level) {
    MemoryLevel.NEW -> "nova"
    MemoryLevel.LEARNING -> "aprendendo"
    MemoryLevel.FAMILIAR -> "familiar"
    MemoryLevel.MASTERED -> "dominada"
}

/**
 * "revisar agora" quando já cruzou o limiar, senão "em 2d 4h".
 *
 * Nulo quando não há retenção: sem ficha pronta não existe revisão marcada, e
 * dizer "revisar agora" nesse caso mandaria a pessoa a uma fila vazia.
 */
fun nextReviewText(retention: Retention?, now: Long): String? {
    val falta = retention?.nextReviewIn(now) ?: return null
    return if (falta <= 0L) "revisar agora" else timeUntil(falta)
}

/**
 * A força de memória como barra.
 *
 * Dá para usá-la curta (a lista de Palavras reserva 84 dp para ela, ao lado do
 * rótulo) ou inteira (a ficha), por isso ela não força largura nenhuma: quem
 * chama decide pelo modifier.
 */
@Composable
fun MemoryBar(
    points: Double,
    level: MemoryLevel,
    modifier: Modifier = Modifier,
    altura: Dp = 8.dp,
) {
    val fracao by animateFloatAsState(
        targetValue = (points / 100.0).toFloat().coerceIn(0f, 1f),
        animationSpec = tween(500),
        label = "fracaoMemoria",
    )

    Box(
        modifier = modifier
            .height(altura)
            .background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(altura / 2)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fracao)
                .fillMaxHeight()
                .background(levelColor(level), RoundedCornerShape(altura / 2)),
        )
    }
}
