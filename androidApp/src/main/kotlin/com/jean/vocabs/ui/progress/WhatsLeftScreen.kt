package com.jean.vocabs.ui.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.dp
import com.jean.vocabs.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jean.vocabs.shared.domain.Entry
import com.jean.vocabs.shared.domain.MemoryLevel
import com.jean.vocabs.shared.domain.Steps
import com.jean.vocabs.ui.components.AppIcons
import com.jean.vocabs.ui.components.EmptyState
import com.jean.vocabs.ui.components.InnerHeader
import com.jean.vocabs.ui.components.ListRow
import com.jean.vocabs.ui.components.ScreenCard
import com.jean.vocabs.ui.components.SelectablePill
import com.jean.vocabs.ui.components.entryTitle
import com.jean.vocabs.ui.components.levelLabel
import com.jean.vocabs.ui.components.nextReviewText

/**
 * "What's left".
 *
 * Each word with the step it is on and how many correct answers remain to the
 * next name. This screen speaks in **steps**, not memory strength: the question
 * here is what to do, and memory strength answers something else — how much is
 * remembered now, which walks backwards on its own and is not a task.
 */
@Composable
fun WhatsLeftScreen(
    target: String?,
    onBack: () -> Unit,
    onOpenCard: (Long) -> Unit,
    vm: ProgressViewModel = viewModel(),
) {
    LaunchedEffect(target) { vm.open(target) }
    val state by vm.state.collectAsStateWithLifecycle()
    var onlyClose by remember { mutableStateOf(true) }

    val perto = state.closeToLeveling
    val list = remember(state.words, onlyClose) {
        val base = if (onlyClose) perto else state.words.filter { Steps.level(it.step) != MemoryLevel.MASTERED }
        base.sortedBy { it.step }
    }
    val mastered = state.mastered

    Column(
        verticalArrangement = Arrangement.spacedBy(13.dp),
        modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 20.dp),
    ) {
        InnerHeader("O que falta", onBack, Modifier.padding(top = 8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SelectablePill(
                label = "Perto de virar · ${perto.size}",
                isSelected = onlyClose,
                onClick = { onlyClose = true },
            )
            SelectablePill(
                label = "Todas · ${state.total}",
                isSelected = !onlyClose,
                onClick = { onlyClose = false },
            )
        }

        if (list.isEmpty()) {
            EmptyState(
                icon = AppIcons.Check,
                title = if (onlyClose) "Nenhuma está perto" else "Nada em aberto",
                detail = if (onlyClose) {
                    "Acerte mais uma vez e a palavra aparece aqui."
                } else {
                    "Todas as palavras já estão dominadas."
                },
                modifier = Modifier.weight(1f),
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(7.dp),
                modifier = Modifier.weight(1f),
            ) {
                items(list, key = { it.id }) { entry ->
                    WordRow(entry, onClick = { onOpenCard(entry.id) })
                }
                if (mastered > 0) {
                    item {
                        ListRow(
                            title = "$mastered já ${if (mastered == 1) "dominada" else "mastered"}",
                            detail = "só voltam de mês em mês",
                            modifier = Modifier.padding(top = 9.dp),
                        )
                    }
                }
                item { Spacer(Modifier.navigationBarsPadding().height(110.dp)) }
            }
        }
    }
}

@Composable
private fun WordRow(entry: Entry, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val step = entry.step
    val level = Steps.level(step)
    val remain = Steps.hitsToLevelUp(step)
    val next = nextReviewText(entry.retention, System.currentTimeMillis())

    ScreenCard(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        filling = PaddingValues(horizontal = 15.dp, vertical = 13.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(entryTitle(entry), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            next?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (it == "revisar agora") colors.primary else colors.onSurfaceVariant,
                )
            }
        }

        entry.card?.translation?.takeIf(String::isNotBlank)?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        ) {
            StepLadder(step, Modifier.weight(1f))
            Text(
                text = "${levelLabel(level)} · degrau $step de ${Steps.TOTAL}",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(start = 10.dp),
            )
        }

        if (remain > 0) {
            Text(
                text = whatsLeftText(remain, Steps.level(step + remain)),
                style = MaterialTheme.typography.bodySmall,
                color = colors.primary,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/**
 * The ladder as five strokes.
 *
 * Five separate strokes rather than one continuous bar: a step is a count, and a
 * bar would suggest positions exist between one step and the next — which is
 * exactly what memory strength shows, on the other screen.
 */
@Composable
private fun StepLadder(step: Int, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = modifier) {
        repeat(Steps.TOTAL) { index ->
            val reached = index < step
            Box(
                Modifier
                    .weight(1f)
                    .height(6.dp)
                    .background(
                        color = if (reached) colors.tertiary else colors.outlineVariant,
                        shape = RoundedCornerShape(3.dp),
                    ),
            )
        }
    }
}

@Composable
internal fun whatsLeftText(hits: Int, nextLevel: MemoryLevel): String =
    pluralStringResource(R.plurals.whats_left_hits, hits, hits, levelLabel(nextLevel))
