package io.github.lucasshuan.vocabu.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.lucasshuan.vocabu.R
import io.github.lucasshuan.vocabu.shared.domain.CaptureFormat
import io.github.lucasshuan.vocabu.ui.theme.LocalDarkTheme
import io.github.lucasshuan.vocabu.ui.theme.VocabuColors

/**
 * One source rather than a `when` per screen: the association forms on the `+`
 * sheet, where all three sit side by side, and is spent in Pending, where the
 * disc is the only thing saying where a capture came from.
 *
 * The red is not `error` — a photo in the queue is not a broken photo.
 */
data class CategoryColors(val color: Color, val background: Color)

@Composable
fun formatColors(format: CaptureFormat): CategoryColors {
    val colors = MaterialTheme.colorScheme
    val dark = LocalDarkTheme.current
    return when (format) {
        CaptureFormat.TEXT -> CategoryColors(colors.primary, colors.secondaryContainer)
        CaptureFormat.AUDIO -> CategoryColors(colors.tertiary, colors.tertiaryContainer)
        CaptureFormat.PHOTO -> if (dark) {
            CategoryColors(VocabuColors.ParrotDark, VocabuColors.ParrotContainerDark)
        } else {
            CategoryColors(VocabuColors.Parrot, VocabuColors.ParrotContainer)
        }
    }
}

fun formatIcon(format: CaptureFormat): ImageVector = when (format) {
    CaptureFormat.TEXT -> AppIcons.Pencil
    CaptureFormat.AUDIO -> AppIcons.Microphone
    CaptureFormat.PHOTO -> AppIcons.Camera
}

/**
 * No separate enum for the tabs: they are the formats, and a second one with the
 * same three names is only a chance for one to gain a case the other lacks.
 */
@Composable
fun formatLabel(format: CaptureFormat): String = stringResource(
    when (format) {
        CaptureFormat.TEXT -> R.string.capture_text
        CaptureFormat.AUDIO -> R.string.capture_audio
        CaptureFormat.PHOTO -> R.string.capture_photo
    }
)

@Composable
fun CategoryDisc(format: CaptureFormat, size: Dp = 38.dp) {
    val palette = formatColors(format)
    IconDisc(
        icon = formatIcon(format),
        contentDescription = formatLabel(format),
        color = palette.color,
        background = palette.background,
        size = size,
    )
}
