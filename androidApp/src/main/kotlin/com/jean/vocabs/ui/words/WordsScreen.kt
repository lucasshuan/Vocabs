package com.jean.vocabs.ui.words

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jean.vocabs.R
import com.jean.vocabs.shared.domain.Entry
import com.jean.vocabs.shared.domain.MemoryLevel
import com.jean.vocabs.ui.components.AppIcons
import com.jean.vocabs.ui.components.CircularFlag
import com.jean.vocabs.ui.components.EmptyState
import com.jean.vocabs.ui.components.MemoryBar
import com.jean.vocabs.ui.components.ScreenCard
import com.jean.vocabs.ui.components.SelectablePill
import com.jean.vocabs.ui.components.TypeBadge
import com.jean.vocabs.ui.components.entryTitle
import com.jean.vocabs.ui.components.levelLabel
import com.jean.vocabs.ui.components.levelLabelColor
import com.jean.vocabs.ui.components.nextReviewText
import com.jean.vocabs.ui.languages.displayName
import com.jean.vocabs.ui.languages.languageOf

/**
 * "Words", all three languages together.
 *
 * The levels stay just below the search, where they have always been. What
 * changed is the language: it stopped being one more pill in the same row and
 * became a group header. Two filter rows on one screen would teach that language
 * and level are the same kind of choice, and they are not — level is a slice,
 * language is a division.
 */
@Composable
fun WordsScreen(
    onOpenCard: (Long) -> Unit,
    vm: WordsViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 120.dp),
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
    ) {
        item(key = "cabecalho") {
            Text(stringResource(R.string.words_title), style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(top = 22.dp))
            Text(
                text = stringResource(
                    R.string.home_stock,
                    pluralStringResource(R.plurals.home_stock_cards, state.total, state.total),
                    state.mastered,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 3.dp),
            )
            OutlinedTextField(
                value = state.query,
                onValueChange = vm::search,
                leadingIcon = { Icon(AppIcons.MagnifyingGlass, null) },
                placeholder = { Text(stringResource(R.string.words_search_placeholder)) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 12.dp),
            ) {
                MemoryFilter.entries.forEach { filter ->
                    SelectablePill(filter.label, state.filter == filter, onClick = { vm.filter(filter) })
                }
            }
        }

        if (state.loaded && state.total == 0) {
            item(key = "vazio") {
                EmptyState(
                    icon = AppIcons.Cards,
                    title = stringResource(R.string.words_empty_title),
                    detail = stringResource(R.string.words_empty_detail),
                )
            }
        } else if (state.loaded && state.matches == 0) {
            item(key = "sem-resultado") {
                EmptyState(
                    icon = AppIcons.MagnifyingGlass,
                    title = stringResource(R.string.words_none_found_title),
                    detail = stringResource(R.string.words_none_found_detail),
                )
            }
        }

        // A search with no results shows only the empty state: repeating three
        // headers with nothing under them would turn the answer into a list of
        // noes.
        val withGroups = state.matches > 0

        state.groups.forEach { group ->
            if (group.total == 0 || !withGroups || group.emptyByFilter) return@forEach

            item(key = "g${group.languagePair.target}") {
                LanguageHeader(
                    group = group,
                    modifier = Modifier.animateItem(),
                    onToggle = { vm.toggleGroup(group.languagePair.target) },
                )
            }
            if (!group.isCollapsed) {
                items(group.entries, key = { "e${it.id}" }) { entry ->
                    WordCard(entry, Modifier.animateItem()) { onOpenCard(entry.id) }
                }
            }
        }
    }
}

/**
 * A language header: flag, name, what is inside, and the chevron.
 *
 * The chevron rotates rather than swapping icon — it is the same element changing
 * state, and 90° says where the list went without anyone comparing two drawings.
 */
@Composable
private fun LanguageHeader(
    group: LanguageGroup,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val activePair = !group.isCollapsed
    val spin by animateFloatAsState(
        targetValue = if (activePair) 90f else 0f,
        animationSpec = tween(200),
        label = "headerRotation",
    )

    Surface(
        onClick = onToggle,
        shape = RoundedCornerShape(12.dp),
        color = if (activePair) colors.secondaryContainer else colors.surfaceVariant,
        modifier = modifier.fillMaxWidth().padding(top = 6.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            CircularFlag(languageOf(group.languagePair.target), size = 20.dp)
            Text(
                text = languageOf(group.languagePair.target).displayName,
                style = MaterialTheme.typography.titleSmall,
                color = if (activePair) colors.onSecondaryContainer else colors.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = if (group.inQueue > 0) stringResource(R.string.words_group_to_review, group.total, group.inQueue)
                else stringResource(R.string.words_group_up_to_date, group.total),
                style = MaterialTheme.typography.bodySmall,
                color = if (group.inQueue > 0) colors.primary else colors.onSurfaceVariant,
            )
            Icon(
                imageVector = AppIcons.Forward,
                contentDescription = stringResource(if (activePair) R.string.a11y_collapse else R.string.a11y_expand),
                tint = if (activePair) colors.primary else colors.onSurfaceVariant,
                modifier = Modifier.size(16.dp).rotate(spin),
            )
        }
    }
}

@Composable
private fun WordCard(entry: Entry, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val now = System.currentTimeMillis()
    val level = entry.retention?.levelAt(now) ?: MemoryLevel.NEW
    val points = entry.retention?.pointsAt(now) ?: 0.0
    val next = nextReviewText(entry.retention, now)
    val inQueue = entry.needsReview(now)

    ScreenCard(modifier = modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(entryTitle(entry), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            TypeBadge(entry.type)
        }
        Text(
            text = entry.card?.translation.orEmpty(),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 9.dp),
        )
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 11.dp)) {
            MemoryBar(points, level, Modifier.width(84.dp), height = 6.dp)
            Text(
                text = levelLabel(level),
                style = MaterialTheme.typography.bodySmall,
                color = levelLabelColor(level),
                modifier = Modifier.padding(start = 9.dp).weight(1f),
            )
            next?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (inQueue) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
