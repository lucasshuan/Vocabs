package com.jean.vocabs.ui.capture

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.jean.vocabs.ui.components.AppIcons
import com.jean.vocabs.ui.components.rememberHaptics
import kotlinx.coroutines.launch

/**
 * Tela 05 do handoff — o texto, e nada além do texto.
 *
 * Tocar e soltar no `+` é texto: o leque nem chega a abrir e o cursor já está
 * aqui. A gaveta tem três coisas — o trecho, "Colar" e "Guardar" — e nenhuma
 * moldura de campo, nenhum título e nenhuma escolha de idioma.
 *
 * O que sumiu daqui vale mais que o que ficou. A folha anterior perguntava o
 * curso antes de deixar escrever, e essa pergunta custava um toque em toda
 * captura para acertar as poucas em que o curso não era o que estava aberto. Sem
 * ela, o teclado já está de pé quando a gaveta termina de subir — que é a única
 * medida que importa numa captura de dez segundos.
 */
@Composable
fun TextDrawer(
    onSave: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val focus = remember { FocusRequester() }
    val example = remember { SNIPPET_EXAMPLES.random() }
    var field by remember { mutableStateOf(TextFieldValue()) }
    val hasText = field.text.isNotBlank()

    LaunchedEffect(Unit) { focus.requestFocus() }

    fun paste() {
        scope.launch {
            clipboard.getClipEntry()
                ?.clipData
                ?.takeIf { it.itemCount > 0 }
                ?.getItemAt(0)
                ?.text
                ?.toString()
                ?.takeIf { it.isNotBlank() }
                ?.let { field = TextFieldValue(it, TextRange(it.length)) }
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .imePadding()
            .padding(horizontal = 20.dp)
            .padding(bottom = 14.dp),
    ) {
        BasicTextField(
            value = field,
            onValueChange = { field = it },
            textStyle = LocalTextStyle.current.merge(MaterialTheme.typography.bodyLarge).copy(color = colors.onSurface),
            cursorBrush = SolidColor(colors.primary),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp, max = 220.dp)
                .focusRequester(focus),
            decorationBox = { content ->
                // O `Box` não é decorativo: `decorationBox` entrega o conteúdo a
                // um layout de filho único, e emitir o exemplo e o campo lado a
                // lado sem contêiner deixa os dois disputando a mesma medida.
                Box {
                    if (field.text.isEmpty()) {
                        Text(
                            text = example,
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                    }
                    content()
                }
            },
        )

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Pill(
                text = "Colar",
                color = colors.primary,
                background = colors.secondaryContainer,
                icon = true,
                onClick = ::paste,
            )
            // "Guardar" só existe quando há o que guardar. Um botão principal
            // permanentemente apagado no canto da gaveta é ruído: ele ocupa o
            // lugar de destaque sem nunca poder ser tocado.
            if (hasText) {
                Pill(
                    text = "Guardar",
                    color = colors.onPrimary,
                    background = colors.primary,
                    icon = false,
                    onClick = { onSave(field.text.trim()) },
                    modifier = Modifier.padding(start = 9.dp),
                    peso = true,
                )
            }
        }

        Spacer(Modifier.navigationBarsPadding().height(4.dp))
    }
}

@Composable
private fun Pill(
    text: String,
    color: androidx.compose.ui.graphics.Color,
    background: androidx.compose.ui.graphics.Color,
    icon: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    peso: Boolean = false,
) {
    val toque = rememberHaptics()
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = background,
        interactionSource = toque,
        modifier = modifier.then(if (peso) Modifier.fillMaxWidth() else Modifier),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (peso) Arrangement.Center else Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
        ) {
            if (icon) Icon(AppIcons.Paste, null, tint = color, modifier = Modifier.size(15.dp))
            Text(text, style = MaterialTheme.typography.labelLarge, color = color)
        }
    }
}

/**
 * Frases de exemplo para a gaveta: um trecho plausível, não uma instrução.
 *
 * "Digite ou cole o trecho" não mostra o que a IA espera — uma frase inteira, com
 * uma expressão capturável dentro dela. O exemplo mostra.
 */
private val SNIPPET_EXAMPLES = listOf(
    "She rolled her eyes and told him to knock it off.",
    "The password was hidden inside a broken vending machine.",
    "He's been dragging his feet on this decision for weeks.",
    "Something about the hallway felt off the moment she stepped in.",
    "I can't believe you pulled that off without any backup.",
    "The recipe calls for a pinch of saffron and a splash of lemon.",
    "He shrugged it off like it was no big deal.",
    "The storm rolled in just as they reached the summit.",
)
