package com.jean.vocabs.ui.card

import android.content.Intent
import android.speech.tts.TextToSpeech
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jean.vocabs.shared.domain.Entry
import com.jean.vocabs.shared.domain.EntryStatus
import com.jean.vocabs.shared.domain.RetentionNow
import com.jean.vocabs.ui.components.AppIcons
import com.jean.vocabs.ui.components.CircularButton
import com.jean.vocabs.ui.components.MemoryBar
import com.jean.vocabs.ui.components.Motion
import com.jean.vocabs.ui.components.Pill
import com.jean.vocabs.ui.components.ScreenCard
import com.jean.vocabs.ui.components.SectionLabel
import com.jean.vocabs.ui.components.TypeBadge
import com.jean.vocabs.ui.components.cardOutline
import com.jean.vocabs.ui.components.entryTitle
import com.jean.vocabs.ui.components.levelLabel
import com.jean.vocabs.ui.components.levelLabelColor
import com.jean.vocabs.ui.components.relativeTime
import com.jean.vocabs.ui.components.timeUntil
import com.jean.vocabs.ui.languages.languageOf
import com.jean.vocabs.ui.temporaryErrorText
import java.util.Locale

@Composable
fun CardScreen(id: Long, onBack: () -> Unit, vm: CardViewModel = viewModel()) {
    val entryFlow = remember(id) { vm.observe(id) }
    val memoryFlow = remember(id) { vm.observeMemory(id) }
    val entry by entryFlow.collectAsStateWithLifecycle()
    val title = entry?.let { entryTitle(it) }.orEmpty()
    val memory by memoryFlow.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    var confirmDeletion by remember { mutableStateOf(false) }
    var didExpand by remember { mutableStateOf(false) }
    val tts = rememberTts(languageOf(entry?.languagePair?.target).tag)

    if (confirmDeletion) {
        AlertDialog(
            onDismissRequest = { confirmDeletion = false },
            title = { Text("Excluir esta ficha?") },
            text = { Text("A mídia compartilhada será preservada enquanto houver outra ficha da mesma captura.") },
            confirmButton = {
                TextButton(onClick = { vm.delete(); confirmDeletion = false; onBack() }) {
                    Text("Excluir", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDeletion = false }) { Text("Cancelar") } },
        )
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 8.dp),
        ) {
            CircularButton(AppIcons.Back, "Voltar", onBack)
            Spacer(Modifier.weight(1f))
            Box {
                CircularButton(AppIcons.MoreVertical, "Mais opções", { menu = true })
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text("Compartilhar") },
                        leadingIcon = { Icon(AppIcons.Share, null) },
                        onClick = {
                            menu = false
                            entry?.let { item ->
                                val text = "$title — ${item.card?.translation.orEmpty()}\n${item.snippet.orEmpty()}\nVocabu"
                                context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, text)
                                }, "Compartilhar ficha"))
                            }
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Excluir", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(AppIcons.Trash, null, tint = MaterialTheme.colorScheme.error) },
                        onClick = { menu = false; confirmDeletion = true },
                    )
                }
            }
        }

        val item = entry
        if (item == null) {
            CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally).padding(top = 100.dp))
            return@Column
        }
        Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(horizontal = 20.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(title, style = MaterialTheme.typography.displaySmall, modifier = Modifier.weight(1f, fill = false))
                    // With no voice installed for the card's language the button
                    // does not appear: a button that does nothing is worse than
                    // its absence, and German in a Portuguese voice is worse yet.
                    tts?.let { voice ->
                        Surface(
                            onClick = { voice.speak(title, TextToSpeech.QUEUE_FLUSH, null, "Vocabu-ficha") },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                        ) {
                            Icon(
                                imageVector = AppIcons.Play,
                                contentDescription = "Ouvir pronúncia",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp).padding(8.dp),
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    item.card?.pronunciation?.takeIf(String::isNotBlank)?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TypeBadge(item.type)
                }
                item.card?.translation?.takeIf(String::isNotBlank)?.let {
                    Text(it, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 2.dp))
                }
            }

            // The card can become ready with the screen open: tapping a Pending
            // row lands here while the AI is still working. The whole body
            // crossfades rather than being swapped in place — otherwise the
            // screen blinks from "being created…" to a full page of text, and the
            // answer arriving looks like a drawing glitch.
            AnimatedContent(
                targetState = item.status,
                transitionSpec = { fadeIn(tween(Motion.DEFAULT)) togetherWith fadeOut(tween(Motion.FAST)) },
                label = "cardBody",
            ) { status -> when (status) {
                EntryStatus.GENERATING, EntryStatus.PENDING -> Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(18.dp)) {
                        CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                        Text("A ficha está sendo criada…", Modifier.padding(start = 12.dp))
                    }
                }
                EntryStatus.ERROR -> Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Text(
                            temporaryErrorText(item.errorCode),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                        // Untranslated on purpose: this is the AI provider's own
                        // sentence, kept for diagnosis. Subordinate styling so it
                        // never reads as Vocabu's own copy.
                        item.errorDetail?.takeIf { it.isNotBlank() }?.let { detail ->
                            Text(
                                detail,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f),
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                        Button(onClick = vm::tryAgain, modifier = Modifier.padding(top = 12.dp)) { Text("Tentar de novo") }
                    }
                }
                EntryStatus.READY -> CardReady(
                    entry = item,
                    memory = memory,
                    didExpand = didExpand,
                    onToggleRelated = { didExpand = !didExpand },
                )
            } }
            Spacer(Modifier.navigationBarsPadding().height(36.dp))
        }
    }
}

/**
 * The card body once the AI has answered.
 *
 * Pulled out of the `when` into a single child: the `AnimatedContent` crossing
 * the card's states hands one slot per state, and the column keeps the four
 * blocks spaced as they were when they were direct siblings.
 */
@Composable
private fun CardReady(
    entry: Entry,
    memory: RetentionNow?,
    didExpand: Boolean,
    onToggleRelated: () -> Unit,
) {
    val card = entry.card ?: return
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        memory?.let { retention ->
            ScreenCard(shape = MaterialTheme.shapes.medium, filling = PaddingValues(15.dp), modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    SectionLabel("Força de memória", Modifier.weight(1f))
                    Text(
                        "${levelLabel(retention.level)} · ${retention.points.toInt()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = levelLabelColor(retention.level),
                    )
                }
                MemoryBar(retention.points, retention.level, Modifier.fillMaxWidth().padding(top = 9.dp))
                Text(
                    text = if (retention.nextInMillis <= 0L) "Está na fila de revisão agora."
                    else "Volta para revisão ${timeUntil(retention.nextInMillis)}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 9.dp),
                )
            }
        }

        entry.snippet?.takeIf(String::isNotBlank)?.let { snippet ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel("Seu contexto")
                HighlightedSnippet(snippet)
                Text(
                    text = listOfNotNull(entry.source?.takeIf(String::isNotBlank), relativeTime(entry.createdAt)).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionLabel("Definições")
            card.definitions.forEachIndexed { index, definition ->
                Text("${index + 1}. $definition", style = MaterialTheme.typography.bodyMedium)
            }
            card.example.takeIf(String::isNotBlank)?.let {
                Text(
                    "Exemplo: $it",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (card.related.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                SectionLabel("Puxa outras palavras")
                // `animateContentSize` because "see more" changes the block's
                // height: without it the rest of the card jumps down between
                // frames and nobody sees where the new pills came from.
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                    modifier = Modifier.animateContentSize(Motion.standardSpring()),
                ) {
                    (if (didExpand) card.related else card.related.take(3)).forEach { term ->
                        Pill(term)
                    }
                    if (card.related.size > 3) {
                        Pill(
                            text = if (didExpand) "ver menos" else "ver mais",
                            highlight = true,
                            onClick = onToggleRelated,
                        )
                    }
                }
            }
        }
    }
}

/** The user's snippet, with the plum bar saying "this is yours, not the dictionary's". */
@Composable
private fun HighlightedSnippet(snippet: String) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface,
        border = cardOutline(),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(
                Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)),
            )
            Text(snippet, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 15.dp, vertical = 13.dp))
        }
    }
}

/**
 * The voice speaks the **card's** language, not the open course's.
 *
 * The card stores the pair it was born in for exactly this: opening an old German
 * word after switching to Spanish has to speak German.
 *
 * `setLanguage` returns `LANG_MISSING_DATA`/`LANG_NOT_SUPPORTED` when the device
 * has no voice installed — the same case as a transcriber with no model. The
 * button then disappears rather than pronouncing German in Portuguese.
 */
@Composable
private fun rememberTts(tag: String): TextToSpeech? {
    val context = LocalContext.current
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(context, tag) {
        // The callback may read the engine itself because `onInit` only arrives
        // after construction returns: it depends on an asynchronous service bind.
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context) { status ->
            val isAvailable = status == TextToSpeech.SUCCESS &&
                engine?.setLanguage(Locale.forLanguageTag(tag)) !in NO_VOICE
            tts = engine.takeIf { isAvailable }
        }
        onDispose {
            engine.stop()
            engine.shutdown()
            tts = null
        }
    }
    return tts
}

private val NO_VOICE = setOf(TextToSpeech.LANG_MISSING_DATA, TextToSpeech.LANG_NOT_SUPPORTED, null)
