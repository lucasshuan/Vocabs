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
import com.jean.vocabs.ui.components.Icones
import com.jean.vocabs.ui.components.lembrarToque
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
fun GavetaDeTexto(
    aoGuardar: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cores = MaterialTheme.colorScheme
    val prancheta = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val foco = remember { FocusRequester() }
    val exemplo = remember { ExemplosDeTrecho.random() }
    var campo by remember { mutableStateOf(TextFieldValue()) }
    val temTexto = campo.text.isNotBlank()

    LaunchedEffect(Unit) { foco.requestFocus() }

    fun colar() {
        scope.launch {
            prancheta.getClipEntry()
                ?.clipData
                ?.takeIf { it.itemCount > 0 }
                ?.getItemAt(0)
                ?.text
                ?.toString()
                ?.takeIf { it.isNotBlank() }
                ?.let { campo = TextFieldValue(it, TextRange(it.length)) }
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
            value = campo,
            onValueChange = { campo = it },
            textStyle = LocalTextStyle.current.merge(MaterialTheme.typography.bodyLarge).copy(color = cores.onSurface),
            cursorBrush = SolidColor(cores.primary),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp, max = 220.dp)
                .focusRequester(foco),
            decorationBox = { conteudo ->
                // O `Box` não é decorativo: `decorationBox` entrega o conteúdo a
                // um layout de filho único, e emitir o exemplo e o campo lado a
                // lado sem contêiner deixa os dois disputando a mesma medida.
                Box {
                    if (campo.text.isEmpty()) {
                        Text(
                            text = exemplo,
                            style = MaterialTheme.typography.bodyLarge,
                            color = cores.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                    }
                    conteudo()
                }
            },
        )

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Pilula(
                text = "Colar",
                cor = cores.primary,
                fundo = cores.secondaryContainer,
                icone = true,
                aoClicar = ::colar,
            )
            // "Guardar" só existe quando há o que guardar. Um botão principal
            // permanentemente apagado no canto da gaveta é ruído: ele ocupa o
            // lugar de destaque sem nunca poder ser tocado.
            if (temTexto) {
                Pilula(
                    text = "Guardar",
                    cor = cores.onPrimary,
                    fundo = cores.primary,
                    icone = false,
                    aoClicar = { aoGuardar(campo.text.trim()) },
                    modifier = Modifier.padding(start = 9.dp),
                    peso = true,
                )
            }
        }

        Spacer(Modifier.navigationBarsPadding().height(4.dp))
    }
}

@Composable
private fun Pilula(
    text: String,
    cor: androidx.compose.ui.graphics.Color,
    fundo: androidx.compose.ui.graphics.Color,
    icone: Boolean,
    aoClicar: () -> Unit,
    modifier: Modifier = Modifier,
    peso: Boolean = false,
) {
    val toque = lembrarToque()
    Surface(
        onClick = aoClicar,
        shape = CircleShape,
        color = fundo,
        interactionSource = toque,
        modifier = modifier.then(if (peso) Modifier.fillMaxWidth() else Modifier),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (peso) Arrangement.Center else Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
        ) {
            if (icone) Icon(Icones.Colar, null, tint = cor, modifier = Modifier.size(15.dp))
            Text(text, style = MaterialTheme.typography.labelLarge, color = cor)
        }
    }
}

/**
 * Frases de exemplo para a gaveta: um trecho plausível, não uma instrução.
 *
 * "Digite ou cole o trecho" não mostra o que a IA espera — uma frase inteira, com
 * uma expressão capturável dentro dela. O exemplo mostra.
 */
private val ExemplosDeTrecho = listOf(
    "She rolled her eyes and told him to knock it off.",
    "The password was hidden inside a broken vending machine.",
    "He's been dragging his feet on this decision for weeks.",
    "Something about the hallway felt off the moment she stepped in.",
    "I can't believe you pulled that off without any backup.",
    "The recipe calls for a pinch of saffron and a splash of lemon.",
    "He shrugged it off like it was no big deal.",
    "The storm rolled in just as they reached the summit.",
)
