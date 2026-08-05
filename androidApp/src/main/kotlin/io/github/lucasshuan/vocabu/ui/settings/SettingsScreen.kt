package io.github.lucasshuan.vocabu.ui.settings

import androidx.compose.ui.platform.LocalResources
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.lucasshuan.vocabu.BuildConfig
import io.github.lucasshuan.vocabu.R
import io.github.lucasshuan.vocabu.shared.ThemePreference
import io.github.lucasshuan.vocabu.ui.components.AppIcons
import io.github.lucasshuan.vocabu.ui.components.CircularFlag
import io.github.lucasshuan.vocabu.ui.components.IconDisc
import io.github.lucasshuan.vocabu.ui.components.InnerHeader
import io.github.lucasshuan.vocabu.ui.components.ListRow
import io.github.lucasshuan.vocabu.ui.components.Motion
import io.github.lucasshuan.vocabu.ui.components.RowChevron
import io.github.lucasshuan.vocabu.ui.components.ScreenCard
import io.github.lucasshuan.vocabu.ui.components.SectionLabel
import io.github.lucasshuan.vocabu.ui.components.cardOutline
import io.github.lucasshuan.vocabu.ui.components.rememberHaptics
import io.github.lucasshuan.vocabu.ui.components.smoothEntrance
import io.github.lucasshuan.vocabu.ui.language.UiLanguage
import io.github.lucasshuan.vocabu.ui.languages.displayName
import io.github.lucasshuan.vocabu.ui.languages.languageOf
import io.github.lucasshuan.vocabu.ui.languages.nameResOf
import java.io.File
import kotlin.math.roundToInt

/**
 * Settings — what applies to the whole app rather than to one course.
 *
 * "My language" changes the language cards are generated in; it does not change
 * the interface. The interface language is a separate setting.
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onSwitchNativeLanguage: () -> Unit,
    vm: SettingsViewModel = viewModel(),
) {
    val theme by vm.theme.collectAsStateWithLifecycle()
    val native by vm.native.collectAsStateWithLifecycle()
    val exporting by vm.exporting.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = LocalActivity.current
    val language = languageOf(native)

    // Read once, not observed: on 33+ the store is the system's and on older
    // releases it is a file, and either way changing it recreates the activity.
    val uiTag = remember(context) { UiLanguage.tagOf(context) }
    val uiLanguageName =
        if (uiTag.isEmpty()) stringResource(R.string.settings_system_default)
        else stringResource(nameResOf(uiTag))
    var pickingUiLanguage by remember { mutableStateOf(false) }

    if (pickingUiLanguage) {
        AppLanguagePicker(
            current = uiTag,
            onPick = { tag ->
                pickingUiLanguage = false
                activity?.let { UiLanguage.set(it, tag) }
            },
            onDismiss = { pickingUiLanguage = false },
        )
    }

    val resources = LocalResources.current

    val importFile = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            Toast.makeText(context, resources.getString(R.string.settings_import_soon), Toast.LENGTH_LONG).show()
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        InnerHeader(stringResource(R.string.settings_title), onBack, Modifier.padding(top = 8.dp))

        // Two rows, not one. The interface language and the language cards are
        // written in are independent settings, and the single "Idioma" row that
        // used to be here was read as doing both.
        Section(icon = AppIcons.Globe, title = stringResource(R.string.settings_section_language), index = 0) {
            val colors = MaterialTheme.colorScheme
            ListRow(
                onClick = { pickingUiLanguage = true },
                start = {
                    IconDisc(
                        AppIcons.Globe,
                        null,
                        color = colors.primary,
                        background = colors.primaryContainer,
                    )
                },
                end = { SwitchPill() },
            ) {
                Text(
                    text = stringResource(R.string.settings_app_language),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
                Text(uiLanguageName, style = MaterialTheme.typography.titleSmall)
            }
            SectionNote(stringResource(R.string.settings_app_language_note))

            ListRow(
                onClick = onSwitchNativeLanguage,
                start = { NativeFlag(native) },
                end = { SwitchPill() },
            ) {
                Text(
                    text = stringResource(R.string.settings_native_language),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
                // The name crossfades: the picker closes over this screen, and
                // without the transition the only thing confirming the change is
                // text that was already in place when the screen reappeared.
                AnimatedContent(
                    targetState = language.displayName,
                    transitionSpec = {
                        (fadeIn(tween(Motion.DEFAULT)) + scaleIn(tween(Motion.DEFAULT), initialScale = 0.92f))
                            .togetherWith(fadeOut(tween(Motion.FAST)))
                    },
                    label = "nativeLanguageName",
                ) { name ->
                    Text(name, style = MaterialTheme.typography.titleSmall)
                }
            }
            SectionNote(stringResource(R.string.settings_native_language_note))
        }

        Divider()

        Section(icon = themeIcon(theme), title = stringResource(R.string.settings_section_appearance), index = 1) {
            ScreenCard(filling = PaddingValues(16.dp), modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.settings_theme), style = MaterialTheme.typography.titleSmall)
                Text(
                    text = stringResource(R.string.settings_theme_auto_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
                ThemeSegmented(
                    isSelected = theme,
                    onChoose = vm::chooseTheme,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }

        Divider()

        Section(icon = AppIcons.Export, title = stringResource(R.string.settings_section_data), index = 2) {
            val colors = MaterialTheme.colorScheme
            ListRow(
                onClick = {
                    vm.export(
                        onReady = { file -> share(context, file) },
                        onError = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() },
                    )
                },
                start = { IconDisc(AppIcons.Export, null, color = colors.primary, background = colors.primaryContainer) },
                end = {
                    // The spinner replaces the chevron rather than sitting beside
                    // it: while the ZIP is being built the row opens nothing.
                    AnimatedContent(
                        targetState = exporting,
                        transitionSpec = { fadeIn(tween(Motion.FAST)).togetherWith(fadeOut(tween(Motion.FAST))) },
                        label = "exportEnd",
                    ) { inProgress ->
                        if (inProgress) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        else RowChevron()
                    }
                },
            ) {
                Text(stringResource(R.string.settings_export), style = MaterialTheme.typography.titleSmall)
                SwappingDetail(
                    stringResource(if (exporting) R.string.settings_export_preparing else R.string.settings_export_detail),
                )
            }

            ListRow(
                onClick = { importFile.launch(arrayOf("application/zip")) },
                start = { IconDisc(AppIcons.Import, null, color = colors.tertiary, background = colors.tertiaryContainer) },
                end = { RowChevron() },
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.settings_import), style = MaterialTheme.typography.titleSmall)
                    ComingSoonBadge(Modifier.padding(start = 8.dp))
                }
                SwappingDetail(stringResource(R.string.settings_import_detail))
            }
        }

        Divider()

        Section(icon = AppIcons.Info, title = stringResource(R.string.settings_section_about), index = 3) {
            Signature()
        }

        Spacer(Modifier.navigationBarsPadding().height(110.dp))
    }
}

/**
 * The icon marks where the section starts when scrolling cuts off the rule above,
 * so it is the size of the label and must not compete with the colored discs of
 * the rows below.
 */
@Composable
private fun Section(
    icon: ImageVector,
    title: String,
    index: Int,
    content: @Composable () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.smoothEntrance(index).fillMaxWidth().padding(top = 4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Appearance's icon follows the choice — sun, moon, half disc — and
            // the spring's overshoot is the confirmation left for someone whose
            // finger covered the segmented control while the pill slid under it.
            AnimatedContent(
                targetState = icon,
                transitionSpec = {
                    (scaleIn(Motion.standardSpring(), initialScale = 0.4f) + fadeIn(tween(Motion.FAST)))
                        .togetherWith(scaleOut(tween(Motion.FAST), targetScale = 0.4f) + fadeOut(tween(Motion.FAST)))
                },
                label = "sectionIcon",
            ) { drawing ->
                Icon(
                    imageVector = drawing,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(15.dp),
                )
            }
            SectionLabel(title, Modifier.padding(start = 7.dp))
        }
        content()
    }
}

@Composable
private fun Divider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant,
        modifier = Modifier.padding(vertical = 6.dp),
    )
}

@Composable
private fun SectionNote(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = 2.dp),
    )
}

/**
 * A hard cut between two sentences in the same position reads as a rendering
 * glitch rather than a change.
 */
@Composable
private fun SwappingDetail(text: String) {
    AnimatedContent(
        targetState = text,
        transitionSpec = { fadeIn(tween(Motion.DEFAULT)).togetherWith(fadeOut(tween(Motion.FAST))) },
        label = "rowDetail",
    ) { current ->
        Text(
            text = current,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

/**
 * The ring exists because half the catalog's flags have white at the edge: on the
 * white surface of the light card the disc would look clipped.
 */
/**
 * The interface languages that ship translations, plus "system default".
 *
 * Only the languages with a `values-xx/` folder are offered. Android 13+ shows
 * the same list under Settings > Apps > Vocabu > Language, from
 * `res/xml/locales_config.xml`.
 */
@Composable
private fun AppLanguagePicker(current: String, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_app_language_picker_title)) },
        text = {
            Column {
                LanguageChoice(stringResource(R.string.settings_system_default), current.isEmpty()) {
                    onPick("")
                }
                UiLanguage.SUPPORTED.forEach { tag ->
                    LanguageChoice(stringResource(nameResOf(tag)), tag == current) { onPick(tag) }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.settings_cancel)) }
        },
    )
}

@Composable
private fun LanguageChoice(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 4.dp))
    }
}

@Composable
private fun NativeFlag(code: String) {
    val colors = MaterialTheme.colorScheme
    AnimatedContent(
        targetState = code,
        transitionSpec = {
            (scaleIn(Motion.elasticSpring(), initialScale = 0.5f) + fadeIn(tween(Motion.DEFAULT)))
                .togetherWith(scaleOut(tween(Motion.FAST), targetScale = 0.5f) + fadeOut(tween(Motion.FAST)))
        },
        label = "nativeFlag",
    ) { current ->
        CircularFlag(
            language = languageOf(current),
            size = 34.dp,
            modifier = Modifier.border(1.dp, colors.outline, CircleShape),
        )
    }
}

@Composable
private fun ComingSoonBadge(modifier: Modifier = Modifier) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = modifier) {
        Text(
            text = stringResource(R.string.settings_soon),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

@Composable
private fun SwitchPill() {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 11.dp, end = 7.dp, top = 7.dp, bottom = 7.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_switch),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Icon(
                imageVector = AppIcons.Forward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

/**
 * Left-aligned rather than centered: what ends up at the bottom of this screen
 * sits under the capture `+`, and a centered signature disappears behind it
 * exactly when the scroll reaches the end.
 */
@Composable
private fun Signature() {
    ListRow(
        start = { Image(painterResource(R.drawable.logo_vocabu), null, Modifier.size(34.dp)) },
    ) {
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleSmall)
        Text(
            text = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

private fun share(context: android.content.Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/zip"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.settings_export_chooser)))
}

@Composable
private fun themeLabel(theme: ThemePreference): String = when (theme) {
    ThemePreference.LIGHT -> stringResource(R.string.settings_theme_light)
    ThemePreference.DARK -> stringResource(R.string.settings_theme_dark)
    ThemePreference.SYSTEM -> stringResource(R.string.settings_theme_auto)
}

private fun themeIcon(theme: ThemePreference): ImageVector = when (theme) {
    ThemePreference.LIGHT -> AppIcons.Sun
    ThemePreference.DARK -> AppIcons.Moon
    ThemePreference.SYSTEM -> AppIcons.HalfDisc
}

/**
 * Equal parts rather than text-sized widths — otherwise three choices of the
 * same weight get three different target sizes, and the smallest falls under the
 * touch minimum.
 *
 * The pill is a **single** one, drawn behind the three labels and moved by a
 * spring. Each option painting its own background made a change a hard cut in two
 * places at once, with nothing saying it was one selection moving.
 */
@Composable
private fun ThemeSegmented(
    isSelected: ThemePreference,
    onChoose: (ThemePreference) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val options = ThemePreference.entries
    val shape = RoundedCornerShape(11.dp)

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = colors.surfaceVariant,
        border = cardOutline(),
        modifier = modifier.fillMaxWidth(),
    ) {
        BoxWithConstraints(Modifier.padding(4.dp)) {
            val width = maxWidth / options.size
            val destination by animateFloatAsState(
                targetValue = options.indexOf(isSelected).toFloat(),
                animationSpec = Motion.standardSpring(),
                label = "themeSlide",
            )

            // `offset` with a lambda: read in the placement phase, so the pill
            // crosses the row without recomposing or remeasuring anyone.
            Box(
                Modifier
                    .offset { IntOffset((destination * width.toPx()).roundToInt(), 0) }
                    .width(width)
                    .height(SEGMENT_HEIGHT)
                    .background(colors.primary, shape),
            )

            Row(Modifier.fillMaxWidth()) {
                options.forEach { option ->
                    val isActive = option == isSelected
                    val tint by animateColorAsState(
                        targetValue = if (isActive) colors.onPrimary else colors.onSurfaceVariant,
                        animationSpec = tween(Motion.DEFAULT),
                        label = "segmentTint",
                    )
                    val touch = rememberHaptics()
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .width(width)
                            .height(SEGMENT_HEIGHT)
                            .clip(shape)
                            .clickable(
                                interactionSource = touch,
                                indication = ripple(),
                                onClick = { onChoose(option) },
                            ),
                    ) {
                        Icon(
                            imageVector = themeIcon(option),
                            contentDescription = null,
                            tint = tint,
                            modifier = Modifier.size(15.dp),
                        )
                        Text(
                            text = themeLabel(option),
                            style = MaterialTheme.typography.labelLarge,
                            color = tint,
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                }
            }
        }
    }
}

/** 44 dp: the touch minimum, and the height of the pill and the three targets. */
private val SEGMENT_HEIGHT = 44.dp
