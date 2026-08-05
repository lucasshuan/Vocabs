package com.jean.vocabs.ui.pending

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jean.vocabs.ui.components.AppIcons
import com.jean.vocabs.ui.components.Motion
import com.jean.vocabs.ui.components.cardOutline
import com.jean.vocabs.ui.components.rememberHaptics

/**
 * O que fica no lugar do cartão que foi arrastado para fora.
 *
 * Ela é a metade indispensável do gesto de excluir. Arrastar é rápido, e o que é
 * rápido erra: sem esta faixa, um deslize distraído numa fila de dez capturas
 * apagaria um áudio gravado na rua e o arquivo junto, sem que nada na tela
 * tivesse perguntado nada. Com ela, o gesto continua custando um movimento só —
 * e a conta de errar cai para um toque.
 *
 * Ela conta o que sumiu **pelo nome** ("Áudio · 0:12", o trecho colado, o título
 * da ficha). Uma faixa que dissesse apenas "1 item excluído" obrigaria a desfazer
 * por precaução para descobrir o que era.
 *
 * A barra de baixo é a mesma do aviso de captura, com o mesmo sentido: mostra
 * quanto tempo ainda resta para mudar de ideia, e é o que torna deixá-la passar
 * uma decisão em vez de um descuido.
 */
@Composable
fun UndoStrip(
    deletion: PendingDeletion?,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // O último conteúdo não nulo continua desenhado enquanto o cartão desce:
    // sem isto a faixa sairia vazia no primeiro quadro da saída.
    var last by remember { mutableStateOf<PendingDeletion?>(null) }
    LaunchedEffect(deletion) { if (deletion != null) last = deletion }

    val remaining = remember { Animatable(1f) }
    LaunchedEffect(deletion?.key) {
        if (deletion == null) return@LaunchedEffect
        remaining.snapTo(1f)
        remaining.animateTo(0f, tween(VISIBLE_WINDOW_MS, easing = LinearEasing))
        // Quem apaga de verdade é o ViewModel, no fim da mesma janela. A barra
        // só desenha o tempo passando: dois relógios disputando quem manda
        // fariam a faixa sumir antes ou depois da exclusão acontecer.
    }

    AnimatedVisibility(
        visible = deletion != null,
        enter = slideInVertically(Motion.standardSpring()) { it / 2 } +
            fadeIn(tween(Motion.DEFAULT)) +
            scaleIn(Motion.standardSpring(), initialScale = 0.94f),
        exit = fadeOut(tween(Motion.FAST)) + slideOutVertically(tween(Motion.FAST)) { it / 3 },
        modifier = modifier,
    ) {
        last?.let { content ->
            CardSurface(deletion = content, remaining = { remaining.value }, onUndo = onUndo)
        }
    }
}

/**
 * Os mesmos 5 s da janela do ViewModel, escritos aqui porque quem desenha o
 * relógio não decide a hora — só a mostra.
 */
private const val VISIBLE_WINDOW_MS = 5_000

@Composable
private fun CardSurface(
    deletion: PendingDeletion,
    remaining: () -> Float,
    onUndo: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.surface,
        border = cardOutline(),
        shadowElevation = 10.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(horizontal = 15.dp, vertical = 14.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(colors.error),
                ) {
                    Icon(AppIcons.Trash, null, tint = colors.onError, modifier = Modifier.size(19.dp))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = if (deletion.isCapture) "Captura excluída" else "Ficha excluída",
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                    )
                    Text(
                        text = deletion.title,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                val toque = rememberHaptics()
                Surface(
                    onClick = onUndo,
                    shape = CircleShape,
                    color = colors.primary,
                    interactionSource = toque,
                ) {
                    Text(
                        text = "Desfazer",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onPrimary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                    )
                }
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(colors.outlineVariant)
                    .drawBehind {
                        drawRect(
                            color = colors.error.copy(alpha = 0.55f),
                            size = size.copy(width = size.width * remaining().coerceIn(0f, 1f)),
                        )
                    },
            )
        }
    }
}
