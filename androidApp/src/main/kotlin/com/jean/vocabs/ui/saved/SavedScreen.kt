package com.jean.vocabs.ui.saved

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
import com.jean.vocabs.shared.domain.Entry
import com.jean.vocabs.shared.domain.EntryStatus
import com.jean.vocabs.ui.components.AppIcons
import com.jean.vocabs.ui.components.CircularFlag
import com.jean.vocabs.ui.components.Motion
import com.jean.vocabs.ui.components.PrimaryButton
import com.jean.vocabs.ui.components.ScreenCard
import com.jean.vocabs.ui.components.breathing
import com.jean.vocabs.ui.components.entryTitle
import com.jean.vocabs.ui.components.smoothEntrance
import com.jean.vocabs.ui.languages.displayName
import com.jean.vocabs.ui.languages.languageOf
import kotlinx.coroutines.delay

/**
 * "Saved".
 *
 * The highlighted action is **capture again**, not leave: whoever just saved two
 * things from one snippet usually has a third, and the cost of the next capture
 * is the whole app's subject.
 *
 * It closes itself once nothing is happening. While a card is still being built
 * it stays — promising to show the work and then vanishing with it would be odd.
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
            // The count comes from the route rather than the database: it is
            // known on the first frame, and a "0 saved" flashing before the query
            // would contradict the one screen that exists to confirm.
            Text(
                text = "${ids.size} ${if (ids.size == 1) "guardada" else "guardadas"}",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )
            if (state.target.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    CircularFlag(languageOf(state.target), size = 18.dp)
                    Text(
                        text = "no seu ${languageOf(state.target).displayName.lowercase()} · ${state.courseTotal} ${if (state.courseTotal == 1) "card" else "cards"} agora",
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
                text = if (ids.size > 1) "Todas entram na revisão de hoje. Se a IA errar o sentido, você corrige na ficha — nada trava aqui."
                else "Ela entra na revisão de hoje. Se a IA errar o sentido, você corrige na ficha — nada trava aqui.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(14.dp),
            )
        }

        Spacer(Modifier.weight(1f))

        Column(verticalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.padding(bottom = 8.dp)) {
            PrimaryButton(
                text = "Capturar outra",
                onClick = { interacted = true; onCaptureAnother() },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth()) {
                ExitAction("Ver as fichas", Modifier.weight(1f)) { interacted = true; onViewCards() }
                ExitAction("Revisar agora", Modifier.weight(1f)) { interacted = true; onReview() }
            }
            Text(
                text = if (state.working) "A IA continua montando ao fundo." else "Fecha sozinho e volta para onde você estava.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.outline,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.navigationBarsPadding().height(8.dp))
    }
}

/** The tick springs in: the app's only celebration, and it lasts 300 ms. */
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
 * One row of what just came in.
 *
 * The only screen whose content changes **on its own** while being watched: the
 * AI returns and "working out the sense" becomes the translation. Both halves of
 * the row crossfade rather than switching between frames.
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
                            text = "não deu certo — dá para tentar de novo em Pendentes",
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
                                text = "montando o sentido",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant,
                                modifier = Modifier.breathing(active = true),
                            )
                        }
                    }
                }
            }
            // The ready badge springs in: it is the end of a wait, and the only
            // thing on the row someone may be watching for.
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
                        Text("ficha pronta", style = MaterialTheme.typography.labelSmall, color = colors.onTertiaryContainer)
                    }
                }
            }
            if (!isReady && !withError) {
                Text("IA", style = MaterialTheme.typography.labelSmall, color = colors.outline)
            }
        }
    }
}

/**
 * The bar that moves without knowing how much is left.
 *
 * Generation has no measurable progress — it is a request that returns or does
 * not. A bar faking a percentage would be inventing one.
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
        border = com.jean.vocabs.ui.components.cardOutline(),
        modifier = modifier,
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.height(48.dp)) {
            Text(text, style = MaterialTheme.typography.titleSmall)
        }
    }
}

/** Reading time for two short lines, and nothing beyond that. */
private const val AUTO_CLOSE_MS = 3_500L
