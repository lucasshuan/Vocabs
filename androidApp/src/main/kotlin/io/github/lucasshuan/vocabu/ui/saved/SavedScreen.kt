package io.github.lucasshuan.vocabu.ui.saved

import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import io.github.lucasshuan.vocabu.R
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.lucasshuan.vocabu.shared.domain.Entry
import io.github.lucasshuan.vocabu.shared.domain.EntryStatus
import io.github.lucasshuan.vocabu.ui.components.AppIcons
import io.github.lucasshuan.vocabu.ui.components.CircularFlag
import io.github.lucasshuan.vocabu.ui.components.Motion
import io.github.lucasshuan.vocabu.ui.components.PrimaryButton
import io.github.lucasshuan.vocabu.ui.components.ScreenCard
import io.github.lucasshuan.vocabu.ui.components.breathing
import io.github.lucasshuan.vocabu.ui.components.entryTitle
import io.github.lucasshuan.vocabu.ui.components.smoothEntrance
import io.github.lucasshuan.vocabu.ui.languages.displayName
import io.github.lucasshuan.vocabu.ui.languages.languageOf
import kotlinx.coroutines.delay

/**
 * The highlighted action is capture again, not leave: two things saved from one
 * snippet usually means a third.
 *
 * Closes itself once nothing is happening, and only then — promising to show the
 * work and vanishing with it would be odd.
 */
@Composable
fun SavedScreen(
    ids: List<Long>,
    onClose: () -> Unit,
    onCaptureAnother: () -> Unit,
    onViewCards: () -> Unit,
    onReview: () -> Unit,
    vm: SavedViewModel = viewModel(),
) {
    LaunchedEffect(ids) { vm.follow(ids) }
    val state by vm.state.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme
    var interacted by remember { mutableStateOf(false) }

    LaunchedEffect(state.working, state.entries.isEmpty(), interacted) {
        if (state.entries.isEmpty() || state.working || interacted) return@LaunchedEffect
        delay(AUTO_CLOSE_MS)
        onClose()
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 20.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(13.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 34.dp),
        ) {
            ConfirmBadge()
            // From the route, not the database: known on the first frame, where
            // a flashed "0 saved" would contradict the screen's whole purpose.
            Text(
                text = pluralStringResource(R.plurals.saved_count, ids.size, ids.size),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )
            if (state.target.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    CircularFlag(languageOf(state.target), size = 18.dp)
                    Text(
                        text = stringResource(
                            R.string.saved_in_your_language,
                            languageOf(state.target).displayName,
                            pluralStringResource(R.plurals.home_stock_cards, state.courseTotal, state.courseTotal),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            state.entries.forEachIndexed { index, entry ->
                SavedRow(entry, Modifier.smoothEntrance(index))
            }
        }

        Surface(shape = MaterialTheme.shapes.medium, color = colors.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(if (ids.size > 1) R.string.saved_note_many else R.string.saved_note_one),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(14.dp),
            )
        }

        Spacer(Modifier.weight(1f))

        Column(verticalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.padding(bottom = 8.dp)) {
            PrimaryButton(
                text = stringResource(R.string.saved_capture_another),
                onClick = { interacted = true; onCaptureAnother() },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth()) {
                ExitAction(stringResource(R.string.saved_see_cards), Modifier.weight(1f)) { interacted = true; onViewCards() }
                ExitAction(stringResource(R.string.saved_review_now), Modifier.weight(1f)) { interacted = true; onReview() }
            }
            Text(
                text = stringResource(if (state.working) R.string.saved_still_working else R.string.saved_closes_itself),
                style = MaterialTheme.typography.bodySmall,
                color = colors.outline,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.navigationBarsPadding().height(8.dp))
    }
}

/** The app's only celebration, and it lasts 300ms. */
@Composable
private fun ConfirmBadge() {
    val colors = MaterialTheme.colorScheme
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val scale by animateFloatAsState(
        targetValue = if (appeared) 1f else 0.5f,
        animationSpec = Motion.elasticSpring(),
        label = "badgeScale",
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.scale(scale).size(96.dp).background(colors.tertiaryContainer.copy(alpha = 0.45f), CircleShape),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(76.dp).background(colors.tertiaryContainer, CircleShape),
        ) {
            Icon(AppIcons.Check, null, tint = colors.tertiary, modifier = Modifier.size(34.dp))
        }
    }
}

/**
 * The only content in the app that changes while being watched — the AI returns
 * and "working out the sense" becomes the translation. Both halves crossfade.
 */
@Composable
private fun SavedRow(entry: Entry, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val isReady = entry.status == EntryStatus.READY
    val withError = entry.status == EntryStatus.ERROR

    ScreenCard(
        shape = MaterialTheme.shapes.medium,
        filling = PaddingValues(horizontal = 15.dp, vertical = 13.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(entryTitle(entry), style = MaterialTheme.typography.titleMedium)
                AnimatedContent(
                    targetState = entry.status,
                    transitionSpec = { fadeIn(tween(Motion.DEFAULT)) togetherWith fadeOut(tween(Motion.FAST)) },
                    label = "rowState",
                ) { status ->
                    when (status) {
                        EntryStatus.READY -> Text(
                            text = entry.card?.translation.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                            modifier = Modifier.padding(top = 3.dp),
                        )
                        EntryStatus.ERROR -> Text(
                            text = stringResource(R.string.saved_failed),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.error,
                            modifier = Modifier.padding(top = 3.dp),
                        )
                        else -> Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                            modifier = Modifier.padding(top = 5.dp),
                        ) {
                            IndeterminateBar()
                            Text(
                                text = stringResource(R.string.saved_building),
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant,
                                modifier = Modifier.breathing(active = true),
                            )
                        }
                    }
                }
            }
            // The end of a wait, and the only thing on the row worth watching for.
            AnimatedVisibility(
                visible = isReady,
                enter = scaleIn(Motion.elasticSpring()) + fadeIn(tween(Motion.DEFAULT)),
                exit = fadeOut(tween(Motion.FAST)),
            ) {
                Surface(shape = CircleShape, color = colors.tertiaryContainer) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    ) {
                        Icon(AppIcons.Check, null, tint = colors.tertiary, modifier = Modifier.size(11.dp))
                        Text(stringResource(R.string.saved_card_ready), style = MaterialTheme.typography.labelSmall, color = colors.onTertiaryContainer)
                    }
                }
            }
            if (!isReady && !withError) {
                Text(stringResource(R.string.saved_ai), style = MaterialTheme.typography.labelSmall, color = colors.outline)
            }
        }
    }
}

/**
 * Generation has no measurable progress — a request returns or it does not, and
 * a percentage would be invented.
 */
@Composable
private fun IndeterminateBar() {
    val colors = MaterialTheme.colorScheme
    var full by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { full = true }
    val fraction by animateFloatAsState(
        targetValue = if (full) 0.72f else 0.12f,
        animationSpec = tween(2_400),
        label = "generationFraction",
    )
    Box(Modifier.width(54.dp).height(5.dp).background(colors.outlineVariant, CircleShape)) {
        Box(Modifier.fillMaxWidth(fraction).height(5.dp).background(colors.primary, CircleShape))
    }
}

@Composable
private fun ExitAction(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = io.github.lucasshuan.vocabu.ui.components.cardOutline(),
        modifier = modifier,
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.height(48.dp)) {
            Text(text, style = MaterialTheme.typography.titleSmall)
        }
    }
}

/** Reading time for two short lines. */
private const val AUTO_CLOSE_MS = 3_500L
