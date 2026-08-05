package com.jean.vocabs.ui.pending

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jean.vocabs.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jean.vocabs.shared.domain.Capture
import com.jean.vocabs.shared.domain.Entry
import com.jean.vocabs.shared.domain.EntryStatus
import com.jean.vocabs.ui.components.AppIcons
import com.jean.vocabs.ui.components.CategoryDisc
import com.jean.vocabs.ui.components.EmptyState
import com.jean.vocabs.ui.components.LanguageFilterPill
import com.jean.vocabs.ui.components.LanguageMark
import com.jean.vocabs.ui.components.ListRow
import com.jean.vocabs.ui.components.Pill
import com.jean.vocabs.ui.components.ScreenCard
import com.jean.vocabs.ui.components.SectionLabel
import com.jean.vocabs.ui.components.SwipeToDelete
import com.jean.vocabs.ui.components.captureTitle
import com.jean.vocabs.ui.components.entryTitle
import com.jean.vocabs.ui.components.relativeTime
import com.jean.vocabs.ui.languages.displayName
import com.jean.vocabs.ui.languages.languageOf
import com.jean.vocabs.ui.components.errorText

/**
 * "Pending", across every language.
 *
 * The flag filter is a manual choice and lives **in the composition**, not in the
 * ViewModel: leaving the tab undoes it. A slice that survived would make the
 * queue look smaller than it is on the next visit.
 *
 * Every row's subtext is the language. Transcription state left it because the
 * colored disc already says whether it is audio, photo or text — while the
 * language, decided at recording time, appeared nowhere.
 *
 * Every card here leaves by being dragged sideways. It is the only screen where
 * that applies, for one reason: a queue is the thing you clear. Discarding used
 * to be a round trip — open the capture, find the button, confirm — to say "this
 * was nothing", and a queue that costs that to shrink is a queue nobody shrinks.
 */
@Composable
fun PendingScreen(
    onOpenCapture: (Capture) -> Unit,
    onOpenCard: (Entry) -> Unit,
    vm: PendingViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var filter by remember { mutableStateOf<String?>(null) }

    val captures = state.captures.filter { filter == null || it.languagePair.target == filter }
    val cards = state.cards.filter { filter == null || it.languagePair.target == filter }
    val oldest = captures.minOfOrNull(Capture::createdAt)
    val languages = state.byLanguage

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(9.dp),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 120.dp),
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
    ) {
        item(key = "cabecalho") {
            Text(stringResource(R.string.pending_title), style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(top = 22.dp))
            Text(
                text = queueSummary(captures.size, cards.size, oldest),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 3.dp),
            )
            if (state.total > 0) SwipeHint()
        }

        if (languages.size > 1) {
            item(key = "filtros") {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 4.dp),
                ) {
                    item(key = "tudo") {
                        LanguageFilterPill(
                            label = "Tudo · ${state.total}",
                            language = null,
                            selected = filter == null,
                            onClick = { filter = null },
                        )
                    }
                    items(languages.entries.toList(), key = { it.key }) { (code, count) ->
                        LanguageFilterPill(
                            label = "${languageOf(code).displayName} · $count",
                            language = languageOf(code),
                            selected = filter == code,
                            onClick = { filter = if (filter == code) null else code },
                        )
                    }
                }
            }
        }

        if (state.total == 0) {
            item(key = "vazio") {
                EmptyState(
                    icon = AppIcons.Check,
                    title = stringResource(R.string.pending_all_clear_title),
                    detail = stringResource(R.string.pending_all_clear_detail),
                )
            }
        }

        items(captures, key = { "c${it.id}" }) { capture ->
            SwipeToDelete(
                onDelete = { vm.deleteCapture(capture) },
                actionLabel = stringResource(R.string.pending_delete_capture),
                modifier = Modifier.animateItem().fillMaxWidth(),
            ) {
                ListRow(
                    onClick = { onOpenCapture(capture) },
                    start = { CategoryDisc(capture.format) },
                    end = {
                        Text(
                            text = relativeTime(capture.createdAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    },
                ) {
                    Text(captureTitle(capture), style = MaterialTheme.typography.titleSmall, maxLines = 1)
                    LanguageMark(languageOf(capture.languagePair.target), Modifier.padding(top = 3.dp))
                }
            }
        }

        if (cards.isNotEmpty()) {
            item(key = "section-cards") { SectionLabel(stringResource(R.string.pending_section_cards), Modifier.padding(top = 10.dp)) }
            items(cards, key = { "e${it.id}" }) { entry ->
                SwipeToDelete(
                    onDelete = { vm.deleteCard(entry) },
                    actionLabel = stringResource(R.string.pending_delete_card),
                    modifier = Modifier.animateItem().fillMaxWidth(),
                ) {
                    EntryCard(
                        entry = entry,
                        onClick = { onOpenCard(entry) },
                        retry = { vm.tryAgain(entry.id) },
                    )
                }
            }
        }
    }
}

/**
 * The row that teaches the gesture.
 *
 * A gesture that only exists under the finger is one half the people never find:
 * no arrow, shadow or border announces a drag. The sentence stays — it is not a
 * first-run bubble — because it costs one 12 sp grey line and answers the only
 * question the screen would leave open. It goes with the queue: an empty screen
 * has no card to drag.
 */
@Composable
private fun SwipeHint() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(top = 7.dp),
    ) {
        Icon(
            imageVector = AppIcons.Trash,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = stringResource(R.string.pending_swipe_hint),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun queueSummary(captures: Int, cards: Int, oldest: Long?): String = when {
    captures > 0 -> {
        val head = pluralStringResource(R.plurals.pending_raw_captures, captures, captures)
        oldest?.let { stringResource(R.string.pending_summary_with_oldest, head, relativeTime(it)) }
            ?: head
    }
    cards > 0 -> pluralStringResource(R.plurals.pending_cards_generating, cards, cards)
    else -> stringResource(R.string.pending_empty)
}

@Composable
private fun EntryCard(
    entry: Entry,
    onClick: () -> Unit,
    retry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ScreenCard(
        shape = MaterialTheme.shapes.medium,
        filling = PaddingValues(horizontal = 15.dp, vertical = 14.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).clickable(onClick = onClick)) {
                Text(entryTitle(entry), style = MaterialTheme.typography.titleSmall)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 3.dp)) {
                    LanguageMark(languageOf(entry.languagePair.target))
                    if (entry.status == EntryStatus.ERROR) {
                        Text(
                            text = stringResource(R.string.pending_error_suffix, errorText(entry.errorCode)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            maxLines = 1,
                        )
                    }
                }
            }
            if (entry.status == EntryStatus.GENERATING) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            if (entry.status == EntryStatus.ERROR) Pill(stringResource(R.string.pending_retry), highlight = true, onClick = retry)
        }
    }
}
