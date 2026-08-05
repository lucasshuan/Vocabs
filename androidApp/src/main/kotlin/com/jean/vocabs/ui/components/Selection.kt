package com.jean.vocabs.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jean.vocabs.contracts.TargetType
import com.jean.vocabs.shared.domain.SelectedTarget
import com.jean.vocabs.shared.domain.selectTokens
import com.jean.vocabs.shared.domain.tokenizeSnippet
import com.jean.vocabs.ui.theme.Bricolage

/**
 * O trecho como campo de seleção: toque escolhe uma palavra, arraste escolhe uma
 * expressão.
 *
 * O realce existe **só enquanto o dedo está na tela**. Terminado o gesto, o
 * trecho volta ao normal e o que foi escolhido aparece embaixo, como pílula. É o
 * que o handoff mostra: o mesmo trecho pode render `fence` e `on the fence`, e
 * intervalos que se sobrepõem pintados ao mesmo tempo viram uma sopa de cor que
 * não diz mais quantas seleções existem. A lista de pílulas diz, e é ela que
 * também permite desfazer.
 */
@Composable
fun TermPicker(
    snippet: String,
    onSelect: (SelectedTarget) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = remember(snippet) { tokenizeSnippet(snippet) }
    var layout by remember(snippet) { mutableStateOf<TextLayoutResult?>(null) }
    var preview by remember(snippet) { mutableStateOf<SelectedTarget?>(null) }
    val colors = MaterialTheme.colorScheme

    val noted = buildAnnotatedString {
        append(snippet)
        preview?.takeIf { it.start >= 0 && it.end <= snippet.length }?.let { target ->
            addStyle(
                SpanStyle(background = colors.primary, color = colors.onPrimary, fontWeight = FontWeight.Bold),
                target.start,
                target.end,
            )
        }
    }

    fun indexAt(position: Offset): Int? {
        val result = layout ?: return null
        val offset = result.getOffsetForPosition(position)
        return tokens.indexOfFirst { offset in it.start until it.end }.takeIf { it >= 0 }
    }

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = colors.surface,
        border = cardOutline(),
        modifier = modifier.fillMaxWidth(),
    ) {
        BasicText(
            text = noted,
            style = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface, lineHeight = 28.sp),
            onTextLayout = { layout = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 14.dp)
                .pointerInput(snippet) {
                    awaitEachGesture {
                        val toque = awaitFirstDown()
                        val start = indexAt(toque.position) ?: return@awaitEachGesture
                        var last = start
                        var dragged = false
                        preview = selectTokens(snippet, start)
                        do {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            if ((change.position - toque.position).getDistance() > viewConfiguration.touchSlop) {
                                dragged = true
                            }
                            indexAt(change.position)?.let { last = it }
                            preview = selectTokens(snippet, start, if (dragged) last else start)
                        } while (event.changes.any { it.pressed })

                        preview?.let(onSelect)
                        preview = null
                    }
                },
        )
    }
}

/**
 * As pílulas do que já foi escolhido: termo, tipo e o ✕ que desfaz.
 *
 * Cada uma entra com um pulinho quando o dedo solta o trecho. É o recibo do
 * gesto: o realce no texto some no instante em que a seleção termina — por
 * decisão de desenho, para que intervalos sobrepostos não virem sopa de cor — e
 * sem esse movimento aqui embaixo o gesto acabaria sem nada confirmando que ele
 * pegou. O `key` prende a animação ao intervalo, e não à posição na lista: sem
 * ele, remover a primeira pílula faria todas as outras entrarem de novo.
 */
@Composable
fun SelectionChips(
    selections: List<SelectedTarget>,
    onRemove: (SelectedTarget) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        selections.forEach { target ->
            key(target.start, target.end) {
                SelectionChip(target) { onRemove(target) }
            }
        }
    }
}

@Composable
private fun SelectionChip(target: SelectedTarget, onRemove: () -> Unit) {
    var arrived by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { arrived = true }
    val scale by animateFloatAsState(
        targetValue = if (arrived) 1f else 0.7f,
        animationSpec = Motion.elasticSpring(),
        label = "escalaDoChip",
    )
    val toque = rememberHaptics()

    Surface(
        onClick = onRemove,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        interactionSource = toque,
        modifier = Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (arrived) 1f else 0f }
            .shrinkOnTouch(toque, minimum = 0.94f),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(start = 13.dp, end = 10.dp, top = 7.dp, bottom = 7.dp),
        ) {
            Text(
                text = target.text,
                style = MaterialTheme.typography.titleSmall.copy(fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 15.sp),
            )
            Text(
                text = if (target.type == TargetType.WORD) "PALAVRA" else "EXPRESSÃO",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.62f),
            )
            Icon(
                imageVector = AppIcons.Close,
                contentDescription = "Remover ${target.text}",
                tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.55f),
                modifier = Modifier.size(14.dp),
            )
        }
    }
}
