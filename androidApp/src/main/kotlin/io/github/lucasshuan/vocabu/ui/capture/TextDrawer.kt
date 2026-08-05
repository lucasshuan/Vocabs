package io.github.lucasshuan.vocabu.ui.capture

import androidx.compose.ui.res.stringResource
import io.github.lucasshuan.vocabu.R
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
import io.github.lucasshuan.vocabu.ui.components.AppIcons
import io.github.lucasshuan.vocabu.ui.components.rememberHaptics
import kotlinx.coroutines.launch

/**
 * The text drawer — the snippet, "Paste" and "Save", and nothing else.
 *
 * Tapping and releasing on the `+` is text: the fan never opens and the cursor is
 * already here. No field frame, no title, no language choice.
 *
 * What left matters more than what stayed. The previous sheet asked for the
 * course before letting anyone write, charging one tap on every capture to get
 * right the few where the course was not the open one. Without it the keyboard is
 * already up as the drawer finishes rising.
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
                // The `Box` is not decorative: `decorationBox` hands the content
                // to a single-child layout, and emitting the example and the
                // field side by side leaves them fighting over the same measure.
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
                text = stringResource(R.string.paste),
                color = colors.primary,
                background = colors.secondaryContainer,
                icon = true,
                onClick = ::paste,
            )
            // "Save" only exists when there is something to save. A permanently
            // greyed primary button in the drawer's corner is noise: it holds the
            // prominent place without ever being tappable.
            if (hasText) {
                Pill(
                    text = stringResource(R.string.save),
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
    val touch = rememberHaptics()
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = background,
        interactionSource = touch,
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
 * Example sentences for the drawer: a plausible snippet, not an instruction.
 *
 * "Type or paste the snippet" does not show what the AI expects — a whole
 * sentence with a capturable expression inside it. The example does.
 *
 * These are target-language content and must follow the course language, never
 * the interface language.
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
