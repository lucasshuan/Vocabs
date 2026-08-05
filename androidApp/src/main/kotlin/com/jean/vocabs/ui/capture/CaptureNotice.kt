package com.jean.vocabs.ui.capture

import com.jean.vocabs.ui.displayName
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
import com.jean.vocabs.shared.domain.CaptureFormat
import com.jean.vocabs.ui.components.CircularFlag
import com.jean.vocabs.ui.components.AppIcons
import com.jean.vocabs.ui.components.Motion
import com.jean.vocabs.ui.components.cardOutline
import com.jean.vocabs.ui.components.formatDurationMs
import com.jean.vocabs.ui.components.rememberHaptics
import com.jean.vocabs.ui.languages.languageOf

/**
 * O que o aviso de baixo tem a dizer.
 *
 * [chave] existe para o aviso ser **substituível**: capturas em sequência trocam
 * o conteúdo e reiniciam a contagem, sem empilhar cartões. Duas capturas iguais
 * seguidas precisam contar como avisos diferentes, e é a chave que garante isso
 * quando o resto dos campos é idêntico.
 */
sealed interface Notice {

    val key: Long

    /**
     * A captura entrou na fila. [capturaId] chega alguns milissegundos depois do
     * resto, quando o banco devolve o id — até lá o atalho "Selecionar" não tem
     * para onde ir e por isso não é desenhado.
     */
    data class Guardado(
        override val key: Long,
        val format: CaptureFormat,
        val durationMs: Long?,
        val target: String,
        val captureId: Long? = null,
    ) : Notice

    /** Uma frase e mais nada: microfone negado, gravação curta demais. */
    data class Recado(override val key: Long, val text: String) : Notice
}

/** Quanto tempo cada aviso vive. O recado é mais curto porque não oferece nada. */
private const val VIDA_DO_GUARDADO_MS = 5_000
private const val VIDA_DO_RECADO_MS = 3_500

/**
 * Tela 04 do handoff — o aviso que passa.
 *
 * Ele volta para onde a pessoa estava e não muda nada de lugar: flutua sobre o
 * conteúdo, some sozinho em 5 s e a barra na base conta o tempo restante. Ignorar
 * é a saída padrão, e ignorar não gera cobrança nem alerta — "Selecionar" é
 * atalho para quem tem tempo agora, nunca a etapa seguinte.
 *
 * Ele substituiu a `Snackbar`: o que o handoff pede — bandeira do idioma, duração
 * do áudio, uma ação nomeada e um relógio visível — não cabe numa linha de texto,
 * e o relógio é a peça que faz o aviso ser ignorável sem ansiedade. Quem vê a
 * barra correr sabe que não precisa fazer nada.
 */
@Composable
fun NoticeStrip(
    aviso: Notice?,
    aoSelecionar: (Long) -> Unit,
    aoExpirar: (key: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    // O último aviso não nulo continua desenhado enquanto o cartão sai: sem isso
    // o conteúdo sumiria no primeiro quadro da saída e o cartão desceria vazio.
    var ultimo by remember { mutableStateOf<Notice?>(null) }
    LaunchedEffect(aviso) { if (aviso != null) ultimo = aviso }

    val restante = remember { Animatable(1f) }
    LaunchedEffect(aviso?.key) {
        val current = aviso ?: return@LaunchedEffect
        val vida = if (current is Notice.Recado) VIDA_DO_RECADO_MS else VIDA_DO_GUARDADO_MS
        restante.snapTo(1f)
        restante.animateTo(0f, tween(vida, easing = LinearEasing))
        aoExpirar(current.key)
    }

    AnimatedVisibility(
        visible = aviso != null,
        enter = slideInVertically(Motion.mola()) { it / 2 } +
            fadeIn(tween(Motion.PADRAO)) +
            scaleIn(Motion.mola(), initialScale = 0.94f),
        exit = fadeOut(tween(Motion.RAPIDO)) + slideOutVertically(tween(Motion.RAPIDO)) { it / 3 },
        modifier = modifier,
    ) {
        ultimo?.let { conteudo ->
            NoticeCard(
                aviso = conteudo,
                restante = { restante.value },
                aoSelecionar = aoSelecionar,
            )
        }
    }
}

@Composable
private fun NoticeCard(
    aviso: Notice,
    restante: () -> Float,
    aoSelecionar: (Long) -> Unit,
) {
    val cores = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = cores.surface,
        border = cardOutline(),
        shadowElevation = 10.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(start = 15.dp, end = 15.dp, top = 14.dp, bottom = 14.dp),
            ) {
                when (aviso) {
                    is Notice.Guardado -> {
                        Disc(cores.tertiary, AppIcons.Check)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                text = savedTitle(aviso),
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                val language = languageOf(aviso.target)
                                CircularFlag(language, tamanho = 15.dp)
                                Text(
                                    text = language.displayName.lowercase(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = cores.onSurfaceVariant,
                                )
                            }
                        }
                        aviso.captureId?.let { id ->
                            val toque = rememberHaptics()
                            Surface(
                                onClick = { aoSelecionar(id) },
                                shape = CircleShape,
                                color = cores.primary,
                                interactionSource = toque,
                            ) {
                                Text(
                                    text = "Selecionar",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = cores.onPrimary,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                                )
                            }
                        }
                    }

                    is Notice.Recado -> {
                        Disc(cores.secondary, AppIcons.Relogio)
                        Text(
                            text = aviso.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = cores.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            // O relógio do aviso. Desenhado a partir da fração animada lida
            // dentro do `drawBehind`: cinco segundos de barra não deveriam
            // recompor o cartão trezentas vezes.
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(cores.outlineVariant)
                    .drawBehind {
                        drawRect(
                            color = cores.onSurfaceVariant.copy(alpha = 0.45f),
                            size = size.copy(width = size.width * restante().coerceIn(0f, 1f)),
                        )
                    },
            )
        }
    }
}

@Composable
private fun Disc(cor: androidx.compose.ui.graphics.Color, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(36.dp).clip(CircleShape).background(cor),
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.surface, modifier = Modifier.size(19.dp))
    }
}

private fun savedTitle(aviso: Notice.Guardado): String = when (aviso.format) {
    CaptureFormat.AUDIO -> aviso.durationMs?.let { "Áudio ${formatDurationMs(it)} guardado" } ?: "Áudio guardado"
    CaptureFormat.PHOTO -> "Foto guardada"
    CaptureFormat.TEXT -> "Trecho guardado"
}
