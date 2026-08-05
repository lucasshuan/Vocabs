package com.jean.vocabs.ui.select

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jean.vocabs.R
import com.jean.vocabs.media.picoDaBarra
import com.jean.vocabs.media.rememberPhoto
import com.jean.vocabs.media.rememberPlayer
import com.jean.vocabs.media.rememberWaveformProfile
import com.jean.vocabs.shared.domain.CaptureFormat
import com.jean.vocabs.shared.domain.CaptureStatus
import com.jean.vocabs.shared.domain.SelectedTarget
import com.jean.vocabs.ui.components.AppIcons
import com.jean.vocabs.ui.components.CategoryDisc
import com.jean.vocabs.ui.components.CircularButton
import com.jean.vocabs.ui.components.CircularFlag
import com.jean.vocabs.ui.components.DuplicateNotice
import com.jean.vocabs.ui.components.PrimaryButton
import com.jean.vocabs.ui.components.ScreenCard
import com.jean.vocabs.ui.components.SectionLabel
import com.jean.vocabs.ui.components.SelectionChips
import com.jean.vocabs.ui.components.TermPicker
import com.jean.vocabs.ui.components.cardOutline
import com.jean.vocabs.ui.components.shrinkOnTouch
import com.jean.vocabs.ui.components.formatColors
import com.jean.vocabs.ui.components.formatDurationMs
import com.jean.vocabs.ui.components.formatLabel
import com.jean.vocabs.ui.components.relativeTime
import com.jean.vocabs.ui.components.rememberHaptics
import com.jean.vocabs.ui.languages.displayName
import com.jean.vocabs.ui.languages.languageOf
import com.jean.vocabs.ui.theme.LocalDarkTheme

/**
 * "What caught your eye?" — one task: marking.
 *
 * The marking does not stay in the text: each confirmed selection clears the
 * snippet and becomes a chip below. That is what lets one snippet yield both
 * `fence` and `on the fence` without a soup of overlapping highlights.
 *
 * The language at the top is still changeable here: the capture exists, but no
 * card has been born in this pair until "Save".
 */
@Composable
fun SelectScreen(
    id: Long,
    onBack: () -> Unit,
    onSave: (List<Long>) -> Unit,
    vm: SelectViewModel = viewModel(),
) {
    val flow = remember(id) { vm.observe(id) }
    val capture by flow.collectAsStateWithLifecycle()
    val duplicate by vm.duplicate.collectAsStateWithLifecycle()
    val courses by vm.courses.collectAsStateWithLifecycle()
    var snippet by remember { mutableStateOf("") }
    val selections = remember { mutableStateListOf<SelectedTarget>() }
    var correcting by remember { mutableStateOf(false) }
    var confirmDeletion by remember { mutableStateOf(false) }

    LaunchedEffect(capture?.id, capture?.snippet) {
        snippet = capture?.snippet.orEmpty()
        selections.clear()
        correcting = snippet.isBlank()
    }
    LaunchedEffect(selections.lastOrNull()?.text, capture?.languagePair?.target) {
        vm.findDuplicate(selections.lastOrNull()?.text.orEmpty(), capture?.languagePair?.target.orEmpty())
    }

    if (confirmDeletion) {
        AlertDialog(
            onDismissRequest = { confirmDeletion = false },
            title = { Text(stringResource(R.string.select_discard_title)) },
            text = { Text(stringResource(R.string.select_discard_body)) },
            confirmButton = {
                TextButton(onClick = { vm.delete(id); confirmDeletion = false; onBack() }) {
                    Text(stringResource(R.string.select_discard), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDeletion = false }) { Text(stringResource(R.string.select_keep)) } },
        )
    }

    val current = capture

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(start = 8.dp, end = 14.dp, top = 8.dp),
        ) {
            CircularButton(AppIcons.Back, stringResource(R.string.back), onBack, MaterialTheme.colorScheme.onSurface)
            Text(
                text = stringResource(R.string.select_question),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f).padding(start = 4.dp),
            )
            current?.let {
                LanguagePicker(
                    target = it.languagePair.target,
                    courses = courses,
                    onChoose = { code -> vm.switchLanguage(id, code) },
                )
            }
        }

        if (current == null) return@Column

        Column(
            verticalArrangement = Arrangement.spacedBy(13.dp),
            modifier = Modifier.padding(horizontal = 20.dp).padding(top = 12.dp),
        ) {
            when (current.format) {
                CaptureFormat.PHOTO -> current.mediaPath?.let { PhotoPreview(it) }
                CaptureFormat.AUDIO -> current.mediaPath?.let {
                    AudioPlayerBar(
                        path = it,
                        durationMs = current.durationMs,
                        correcting = correcting,
                        onCorrect = { correcting = !correcting },
                    )
                }
                CaptureFormat.TEXT -> TextSource(current.createdAt)
            }

            when {
                current.status == CaptureStatus.TRANSCRIBING -> ProcessNotice(
                    text = stringResource(R.string.select_transcribing),
                    withProgress = true,
                )
                current.transcriptionError != null -> ErrorNotice(current.transcriptionError.orEmpty())
            }

            if (correcting || snippet.isBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionLabel(stringResource(R.string.select_transcription))
                    OutlinedTextField(
                        value = snippet,
                        onValueChange = { snippet = it; selections.clear() },
                        placeholder = { Text(stringResource(R.string.select_type_manually)) },
                        minLines = 2,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (snippet.isNotBlank()) {
                        PrimaryButton(stringResource(R.string.select_done_marking), { correcting = false })
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TermPicker(
                        snippet = snippet,
                        onSelect = { target ->
                            if (selections.none { it.start == target.start && it.end == target.end }) selections += target
                        },
                    )
                    Text(
                        text = stringResource(R.string.select_tap_or_drag),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            AnimatedVisibility(
                visible = selections.isNotEmpty(),
                enter = fadeIn(tween(160)) + expandVertically(tween(180)),
                exit = fadeOut(tween(120)) + shrinkVertically(tween(140)),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    SectionLabel(pluralStringResource(R.plurals.select_chosen, selections.size, selections.size))
                    SelectionChips(selections, selections::remove)
                }
            }

            duplicate?.let { DuplicateNotice(it) }

            PrimaryButton(
                text = if (selections.isEmpty()) stringResource(R.string.select_nothing_chosen)
                else pluralStringResource(R.plurals.select_save_captures, selections.size, selections.size),
                onClick = { vm.save(id, snippet, selections.toList(), onSave) },
                enabled = snippet.isNotBlank() && selections.isNotEmpty(),
                modifier = Modifier.padding(top = 4.dp),
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                DiscardCaptureButton { confirmDeletion = true }
            }
        }
        Spacer(Modifier.navigationBarsPadding().height(24.dp))
    }
}

/**
 * "Discard capture" — soft red, with the trash icon.
 *
 * It used to be a `TextButton` in `outline`, which in light is a lilac with 1 px
 * of contrast: the only exit for someone who opened a capture by mistake was
 * written in the color of the borders. It is now the same pairing (light
 * container plus error ink) the delete swipe uses before its threshold — across
 * the app that pair means "this erases, and you can still back out".
 */
@Composable
private fun DiscardCaptureButton(onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    // In dark, Material's `errorContainer` is a nearly opaque wine, too heavy for
    // a supporting action.
    val background = if (LocalDarkTheme.current) colors.error.copy(alpha = 0.14f) else colors.errorContainer
    val touch = rememberHaptics()
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = background,
        contentColor = colors.error,
        interactionSource = touch,
        modifier = Modifier.shrinkOnTouch(touch, minimum = 0.94f),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 11.dp),
        ) {
            Icon(AppIcons.Trash, contentDescription = null, tint = colors.error, modifier = Modifier.size(18.dp))
            Text(stringResource(R.string.select_discard_capture), style = MaterialTheme.typography.labelLarge, color = colors.error)
        }
    }
}

/**
 * A destination, not a label: whoever recorded with the wrong language marked
 * fixes it here, at the last moment it is still cheap.
 */
@Composable
private fun LanguagePicker(target: String, courses: List<String>, onChoose: (String) -> Unit) {
    var activePair by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme

    Box {
        Surface(
            onClick = { activePair = true },
            shape = CircleShape,
            color = colors.surface,
            border = cardOutline(),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(start = 5.dp, end = 8.dp, top = 5.dp, bottom = 5.dp),
            ) {
                CircularFlag(languageOf(target), size = 19.dp)
                Icon(AppIcons.Expand, stringResource(R.string.a11y_change_language), tint = colors.onSurfaceVariant, modifier = Modifier.size(14.dp))
            }
        }
        DropdownMenu(expanded = activePair, onDismissRequest = { activePair = false }) {
            courses.forEach { code ->
                DropdownMenuItem(
                    text = { Text(languageOf(code).displayName) },
                    leadingIcon = { CircularFlag(languageOf(code), size = 20.dp) },
                    trailingIcon = {
                        if (code == target) Icon(AppIcons.Check, null, tint = colors.tertiary, modifier = Modifier.size(16.dp))
                    },
                    onClick = { activePair = false; if (code != target) onChoose(code) },
                )
            }
        }
    }
}

@Composable
private fun TextSource(createdAt: Long) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CategoryDisc(CaptureFormat.TEXT, size = 22.dp)
        SectionLabel(stringResource(R.string.select_pasted_text, relativeTime(createdAt)))
    }
}

@Composable
private fun PhotoPreview(path: String) {
    val image by rememberPhoto(path)
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        image?.let { Image(it, stringResource(R.string.a11y_captured_photo), contentScale = ContentScale.FillWidth, modifier = Modifier.fillMaxWidth()) }
            ?: Box(Modifier.height(180.dp))
    }
}

/**
 * Deliberately without word-level sync: listening again solves it, and a
 * highlight following the speech would need per-word alignment the local
 * transcription does not provide.
 */
@Composable
private fun AudioPlayerBar(path: String, durationMs: Long?, correcting: Boolean, onCorrect: () -> Unit) {
    val player = rememberPlayer(path)
    val palette = formatColors(CaptureFormat.AUDIO)
    ScreenCard(filling = PaddingValues(14.dp), modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
            Surface(onClick = player::alternar, shape = CircleShape, color = palette.color) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = if (player.playing) AppIcons.Stop else AppIcons.Play,
                        contentDescription = stringResource(if (player.playing) R.string.a11y_stop else R.string.a11y_play),
                        tint = palette.background,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            AudioWave(
                path = path,
                color = palette.color,
                progress = player.progress,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = if (player.playing) formatDurationMs(player.positionMs)
                else durationMs?.let(::formatDurationMs).orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
            SectionLabel(stringResource(R.string.select_audio_transcribed), Modifier.weight(1f))
            Text(
                text = stringResource(if (correcting) R.string.select_back else R.string.select_fix_text),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .then(Modifier)
                    .clickableWithoutRipple(onCorrect),
            )
        }
    }
}

@Composable
private fun ProcessNotice(text: String, withProgress: Boolean) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(14.dp)) {
            if (withProgress) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 12.dp))
        }
    }
}

@Composable
private fun ErrorNotice(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(14.dp),
        )
    }
}

/** What the lowest bar still occupies, so silence is a line and not a gap. */
private val MIN_WAVE_HEIGHT = 3.dp

private const val PRESS_OPACITY = 0.3f

/**
 * A fixed bar width rather than ten stretched bars: what reads as "audio" is the
 * repeated thin bar, not the count of them.
 *
 * Until the profile arrives — or if the file is unreadable — every bar stays at
 * minimum height; an invented wave would say something nobody measured.
 */
@Composable
private fun AudioWave(
    path: String,
    color: androidx.compose.ui.graphics.Color,
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val profile by rememberWaveformProfile(path)
    Canvas(modifier = modifier.height(26.dp)) {
        val width = 3.dp.toPx()
        val step = width * 2
        val smallest = MIN_WAVE_HEIGHT.toPx()
        val amount = (size.width / step).toInt().coerceAtLeast(1)
        val needle = size.width * progress
        repeat(amount) { index ->
            val height = smallest + (size.height - smallest) * profile.picoDaBarra(index, amount)
            val x = index * step
            drawRoundRect(
                color = color,
                topLeft = Offset(x, (size.height - height) / 2f),
                size = Size(width, height),
                cornerRadius = CornerRadius(width / 2f),
                alpha = if (x + width <= needle) 1f else PRESS_OPACITY,
            )
        }
    }
}

@Composable
private fun Modifier.clickableWithoutRipple(onClick: () -> Unit): Modifier {
    val interaction = remember { MutableInteractionSource() }
    return clickable(interactionSource = interaction, indication = null, onClick = onClick)
}
