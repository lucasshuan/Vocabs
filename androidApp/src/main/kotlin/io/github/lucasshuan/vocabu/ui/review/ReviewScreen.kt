package io.github.lucasshuan.vocabu.ui.review

import androidx.compose.ui.res.stringResource
import io.github.lucasshuan.vocabu.R
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.lucasshuan.vocabu.shared.domain.clozeSnippet
import io.github.lucasshuan.vocabu.ui.components.AppIcons
import io.github.lucasshuan.vocabu.ui.components.CircularButton
import io.github.lucasshuan.vocabu.ui.components.EmptyState
import io.github.lucasshuan.vocabu.ui.components.MetricCard
import io.github.lucasshuan.vocabu.ui.components.Motion
import io.github.lucasshuan.vocabu.ui.components.PrimaryButton
import io.github.lucasshuan.vocabu.ui.components.ScreenCard
import io.github.lucasshuan.vocabu.ui.components.SecondaryAction
import io.github.lucasshuan.vocabu.ui.components.SectionLabel
import io.github.lucasshuan.vocabu.ui.components.animatedCount
import io.github.lucasshuan.vocabu.ui.components.animatedFraction
import io.github.lucasshuan.vocabu.ui.components.smoothEntrance

private const val GAP = "________"

@Composable
fun ReviewScreen(onBack: () -> Unit, vm: ReviewViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 20.dp, top = 8.dp),
        ) {
            CircularButton(AppIcons.Close, stringResource(R.string.a11y_close_review), onBack)
            val card = state as? ReviewState.CardSurface
            ProgressBar(
                fraction = card?.let { it.position.toFloat() / it.total.coerceAtLeast(1) } ?: 0f,
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
            )
            Text(
                text = card?.let { stringResource(R.string.review_position, it.position, it.total) }.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        when (val current = state) {
            ReviewState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            ReviewState.Empty -> Empty(onBack)
            is ReviewState.CardSurface -> CardSurface(current, vm)
            is ReviewState.Summary -> Summary(current, vm, onBack)
        }
    }
}

/**
 * The only thing showing the whole session, and it only plays that part if the
 * advance is seen: jumping with the new card, it disappears mid-swap.
 */
@Composable
private fun ProgressBar(fraction: Float, modifier: Modifier = Modifier) {
    val advance by animatedFraction(fraction, "avancoDaSessao")
    Box(
        modifier = modifier
            .height(6.dp)
            .background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(3.dp)),
    ) {
        Box(
            Modifier
                .fillMaxWidth(advance)
                .height(6.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(3.dp)),
        )
    }
}

@Composable
private fun CardSurface(state: ReviewState.CardSurface, vm: ReviewViewModel) {
    val keyboard = LocalSoftwareKeyboardController.current
    // A copy, not a live read: "Continue" clears the feedback and swaps the card
    // while the box is still shrinking, and those 150ms would show the next
    // card's answer — giving away the word about to be asked.
    var lastVerdict by remember { mutableStateOf<Pair<ReviewFeedback, String>?>(null) }
    LaunchedEffect(state.feedback, state.entry.id) {
        state.feedback?.let { lastVerdict = it to state.entry.target.orEmpty() }
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        // Only the snippet moves, not the screen: swapping the whole column
        // would drop keyboard focus on every answered word.
        AnimatedContent(
            targetState = state.entry,
            transitionSpec = {
                (slideInHorizontally(tween(Motion.DEFAULT)) { width -> width / 4 } + fadeIn(tween(Motion.DEFAULT)))
                    .togetherWith(slideOutHorizontally(tween(Motion.FAST)) { width -> -width / 4 } + fadeOut(tween(Motion.FAST)))
            },
            contentKey = { it.id },
            label = "reviewCard",
        ) { entry ->
            ScreenCard(
                shape = MaterialTheme.shapes.extraLarge,
                filling = PaddingValues(24.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                SectionLabel(stringResource(R.string.review_complete_snippet))
                Text(
                    text = annotatedCloze(clozeSnippet(entry, GAP)),
                    style = MaterialTheme.typography.headlineSmall.copy(lineHeight = MaterialTheme.typography.headlineLarge.lineHeight),
                    modifier = Modifier.padding(vertical = 22.dp),
                )
                Text(
                    entry.card?.translation.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        OutlinedTextField(
            value = state.answer,
            onValueChange = vm::editAnswer,
            enabled = state.feedback == null,
            placeholder = { Text(stringResource(R.string.review_answer_placeholder)) },
            singleLine = false,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { keyboard?.hide(); vm.confirm() }),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        )

        // Opens by pushing the button down rather than appearing ready: 240ms
        // separates "the screen answered" from "it was already like this".
        AnimatedVisibility(
            visible = state.feedback != null,
            enter = expandVertically(tween(Motion.DEFAULT)) + fadeIn(tween(Motion.DEFAULT)),
            exit = shrinkVertically(tween(Motion.FAST)) + fadeOut(tween(Motion.FAST)),
        ) {
            // Live on entry, from the copy on exit: never a frame without
            // content, never the wrong card's answer.
            val verdict = state.feedback?.let { it to state.entry.target.orEmpty() } ?: lastVerdict
            if (verdict != null) {
                val (feedback, answer) = verdict
                val correct = feedback == ReviewFeedback.CORRETA
                Surface(
                    color = if (correct) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.errorContainer,
                    contentColor = if (correct) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            when {
                                correct -> stringResource(R.string.review_right)
                                feedback == ReviewFeedback.NAO_LEMBRO -> stringResource(R.string.review_dont_remember_answer)
                                else -> stringResource(R.string.review_almost)
                            },
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(answer, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }
        }

        if (state.feedback == null) {
            PrimaryButton(stringResource(R.string.review_check), { keyboard?.hide(); vm.confirm() }, enabled = state.answer.isNotBlank())
            SecondaryAction(stringResource(R.string.review_dont_remember), { keyboard?.hide(); vm.dontRemember() })
        } else {
            PrimaryButton(stringResource(R.string.review_continue), vm::advance)
        }
        Spacer(Modifier.navigationBarsPadding().height(20.dp))
    }
}

/** A solid plum rule, not underscores. */
@Composable
private fun annotatedCloze(snippet: String) = buildAnnotatedString {
    val cut = snippet.indexOf(GAP)
    if (cut < 0) {
        append(snippet)
        return@buildAnnotatedString
    }
    append(snippet.substring(0, cut))
    pushStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline))
    append(GAP)
    pop()
    append(snippet.substring(cut + GAP.length))
}

@Composable
private fun Empty(onBack: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        EmptyState(
            icon = AppIcons.Check,
            title = stringResource(R.string.review_up_to_date_title),
            detail = stringResource(R.string.review_up_to_date_detail),
            action = { PrimaryButton(stringResource(R.string.back), onBack, Modifier.padding(horizontal = 40.dp)) },
        )
    }
}

@Composable
private fun Summary(state: ReviewState.Summary, vm: ReviewViewModel, onBack: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
    ) {
        Text(
            text = stringResource(R.string.review_session_done),
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.smoothEntrance().padding(top = 48.dp),
        )
        // Hits and streak count up; "to reinforce" does not. It is what is still
        // missing, and a rising count there turns a mistake into a score.
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
            MetricCard("${animatedCount(state.hits, "hits")}", stringResource(R.string.review_hits), Modifier.weight(1f), highlight = true)
            MetricCard("${state.misses}", stringResource(R.string.review_to_reinforce), Modifier.weight(1f))
            MetricCard(
                value = "${animatedCount(state.dayStreak, "dayStreak")}",
                label = stringResource(R.string.review_day_streak),
                modifier = Modifier.weight(1f),
                highlight = true,
            )
        }
        if (state.wrong.isNotEmpty()) {
            Text(
                "Voltam para a fila: ${state.wrong.distinct().joinToString()}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.smoothEntrance(index = 2).padding(top = 20.dp),
            )
        }
        PrimaryButton("Concluir", onBack, Modifier.smoothEntrance(index = 3).padding(top = 28.dp))
        if (state.rest > 0) SecondaryAction("Mais uma rodada (${state.rest})", vm::newRound)
        Spacer(Modifier.navigationBarsPadding().height(20.dp))
    }
}
