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
import com.jean.vocabs.contracts.TipoAlvo
import com.jean.vocabs.shared.domain.AlvoSelecionado
import com.jean.vocabs.shared.domain.selecionarTokens
import com.jean.vocabs.shared.domain.tokenizarTrecho
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
fun SeletorDeTermos(
    trecho: String,
    aoSelecionar: (AlvoSelecionado) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = remember(trecho) { tokenizarTrecho(trecho) }
    var layout by remember(trecho) { mutableStateOf<TextLayoutResult?>(null) }
    var previa by remember(trecho) { mutableStateOf<AlvoSelecionado?>(null) }
    val cores = MaterialTheme.colorScheme

    val anotado = buildAnnotatedString {
        append(trecho)
        previa?.takeIf { it.inicio >= 0 && it.fim <= trecho.length }?.let { alvo ->
            addStyle(
                SpanStyle(background = cores.primary, color = cores.onPrimary, fontWeight = FontWeight.Bold),
                alvo.inicio,
                alvo.fim,
            )
        }
    }

    fun indiceEm(posicao: Offset): Int? {
        val resultado = layout ?: return null
        val deslocamento = resultado.getOffsetForPosition(posicao)
        return tokens.indexOfFirst { deslocamento in it.inicio until it.fim }.takeIf { it >= 0 }
    }

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = cores.surface,
        border = contornoDeCartao(),
        modifier = modifier.fillMaxWidth(),
    ) {
        BasicText(
            text = anotado,
            style = MaterialTheme.typography.bodyLarge.copy(color = cores.onSurface, lineHeight = 28.sp),
            onTextLayout = { layout = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 14.dp)
                .pointerInput(trecho) {
                    awaitEachGesture {
                        val toque = awaitFirstDown()
                        val inicio = indiceEm(toque.position) ?: return@awaitEachGesture
                        var ultimo = inicio
                        var arrastou = false
                        previa = selecionarTokens(trecho, inicio)
                        do {
                            val evento = awaitPointerEvent()
                            val mudanca = evento.changes.firstOrNull() ?: break
                            if ((mudanca.position - toque.position).getDistance() > viewConfiguration.touchSlop) {
                                arrastou = true
                            }
                            indiceEm(mudanca.position)?.let { ultimo = it }
                            previa = selecionarTokens(trecho, inicio, if (arrastou) ultimo else inicio)
                        } while (evento.changes.any { it.pressed })

                        previa?.let(aoSelecionar)
                        previa = null
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
fun ChipsDeSelecao(
    selecoes: List<AlvoSelecionado>,
    aoRemover: (AlvoSelecionado) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        selecoes.forEach { alvo ->
            key(alvo.inicio, alvo.fim) {
                ChipDeSelecao(alvo) { aoRemover(alvo) }
            }
        }
    }
}

@Composable
private fun ChipDeSelecao(alvo: AlvoSelecionado, aoRemover: () -> Unit) {
    var chegou by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { chegou = true }
    val escala by animateFloatAsState(
        targetValue = if (chegou) 1f else 0.7f,
        animationSpec = Movimento.molaElastica(),
        label = "escalaDoChip",
    )
    val toque = lembrarToque()

    Surface(
        onClick = aoRemover,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        interactionSource = toque,
        modifier = Modifier
            .graphicsLayer { scaleX = escala; scaleY = escala; alpha = if (chegou) 1f else 0f }
            .encolheAoTocar(toque, minimo = 0.94f),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(start = 13.dp, end = 10.dp, top = 7.dp, bottom = 7.dp),
        ) {
            Text(
                text = alvo.texto,
                style = MaterialTheme.typography.titleSmall.copy(fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 15.sp),
            )
            Text(
                text = if (alvo.tipo == TipoAlvo.PALAVRA) "PALAVRA" else "EXPRESSÃO",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.62f),
            )
            Icon(
                imageVector = Icones.Fechar,
                contentDescription = "Remover ${alvo.texto}",
                tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.55f),
                modifier = Modifier.size(14.dp),
            )
        }
    }
}
