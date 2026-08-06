package io.github.lucasshuan.vocabu.ui.languages

import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import io.github.lucasshuan.vocabu.R
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.lucasshuan.vocabu.contracts.Language
import io.github.lucasshuan.vocabu.ui.components.AppIcons
import io.github.lucasshuan.vocabu.ui.components.CircularFlag
import io.github.lucasshuan.vocabu.ui.components.EmptyState
import io.github.lucasshuan.vocabu.ui.components.InnerHeader
import io.github.lucasshuan.vocabu.ui.components.PrimaryButton
import io.github.lucasshuan.vocabu.ui.components.ScreenCard
import io.github.lucasshuan.vocabu.ui.components.SectionLabel
import io.github.lucasshuan.vocabu.ui.components.cardOutline
import io.github.lucasshuan.vocabu.ui.languages.displayName

/**
 * Serves both language choices: enrolling and switching the native language are
 * the same list, search and row. Duplicating it to change a title and a verb
 * would give two screens that diverge on the first adjustment.
 */
@Composable
fun NewLanguageScreen(
    onBack: () -> Unit,
    forNative: Boolean = false,
    vm: NewLanguageViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var chosen by remember { mutableStateOf<Language?>(null) }
    var confirming by remember { mutableStateOf<Language?>(null) }

    confirming?.let { language ->
        SwitchNativeDialog(
            to = language,
            from = languageOf(state.native),
            onConfirm = { vm.switchNative(language.code); confirming = null; onBack() },
            onDismiss = { confirming = null },
        )
    }

    val context = LocalContext.current
    val available = remember(state, query, context) { vm.available().search(query, context::nameOf) }

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize().statusBarsPadding().imePadding().padding(horizontal = 20.dp),
    ) {
        InnerHeader(
            title = stringResource(if (forNative) R.string.newlanguage_which_is_yours else R.string.newlanguage_which_to_learn),
            onBack = onBack,
            modifier = Modifier.padding(top = 8.dp),
        )

        if (!forNative && state.alreadyHas.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                SectionLabel(stringResource(R.string.newlanguage_you_already_have))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    state.alreadyHas.forEach { language -> LanguagePill(language) }
                }
            }
        }

        SearchField(value = query, onChange = { query = it })

        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
            SectionLabel(stringResource(R.string.newlanguage_choose_now), Modifier.weight(1f))
            Text(
                text = pluralStringResource(R.plurals.newlanguage_available, available.size, available.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }

        if (available.isEmpty()) {
            EmptyState(
                icon = AppIcons.MagnifyingGlass,
                title = stringResource(R.string.newlanguage_nothing_named),
                detail = stringResource(R.string.newlanguage_try_another),
                modifier = Modifier.weight(1f),
            )
        } else {
            ScreenCard(filling = PaddingValues(0.dp), modifier = Modifier.weight(1f).fillMaxWidth()) {
                LazyColumn {
                    items(available, key = { it.code }) { language ->
                        LanguageRow(
                            language = language,
                            selected = language.code == chosen?.code,
                            onClick = { chosen = language },
                        )
                    }
                }
            }
        }

        PrimaryButton(
            text = chosen
                ?.let { stringResource(if (forNative) R.string.newlanguage_use else R.string.newlanguage_start, it.displayName) }
                ?: stringResource(R.string.newlanguage_pick_one),
            enabled = chosen != null,
            onClick = {
                chosen?.let { language ->
                    // Enrolling needs no warning. Switching the language you
                    // speak changes every future card, and used to look like
                    // losing the ones already stored.
                    if (forNative) confirming = language else { vm.enroll(language.code); onBack() }
                }
            },
            modifier = Modifier.padding(bottom = 6.dp).navigationBarsPadding(),
        )
    }
}

@Composable
private fun LanguagePill(language: Language) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, border = cardOutline()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            modifier = Modifier.padding(start = 7.dp, end = 12.dp, top = 7.dp, bottom = 7.dp),
        ) {
            CircularFlag(language, size = 20.dp)
            Text(
                text = language.displayName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LanguageRow(language: Language, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        color = if (selected) colors.secondaryContainer else colors.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp),
            ) {
                CircularFlag(language, size = 30.dp)
                Text(language.displayName, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                if (selected) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(22.dp).background(colors.primary, CircleShape),
                    ) {
                        Icon(AppIcons.Check, null, tint = colors.onPrimary, modifier = Modifier.size(14.dp))
                    }
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(colors.outlineVariant))
        }
    }
}

/**
 * `BasicTextField`, not `OutlinedTextField`: Material's brings a floating label,
 * its own outline and a minimum height, none of which fit this card.
 */
@Composable
private fun SearchField(value: String, onChange: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = MaterialTheme.shapes.small,
        color = colors.surface,
        border = cardOutline(),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            modifier = Modifier.padding(horizontal = 14.dp).height(48.dp),
        ) {
            Icon(AppIcons.MagnifyingGlass, null, tint = colors.outline, modifier = Modifier.size(18.dp))
            Box(Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(stringResource(R.string.newlanguage_search), style = MaterialTheme.typography.bodyMedium, color = colors.outline)
                }
                BasicTextField(
                    value = value,
                    onValueChange = onChange,
                    singleLine = true,
                    textStyle = LocalTextStyle.current.merge(
                        MaterialTheme.typography.bodyMedium.copy(color = colors.onSurface),
                    ),
                    cursorBrush = SolidColor(colors.primary),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Search),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/**
 * The change is invisible until the next card is generated, and the screen
 * behind used to read zero afterwards — so the fear it answers is "did I just
 * delete everything". Reassurance first, both languages named, and a line
 * separating this from the app's own language, the setting just above it.
 */
@Composable
private fun SwitchNativeDialog(
    to: Language,
    from: Language,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.switch_native_title, to.displayName)) },
        text = {
            Text(
                AnnotatedString.fromHtml(
                    stringResource(R.string.switch_native_body, to.displayName, from.displayName)
                )
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.switch_native_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
