package com.jean.vocabs.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jean.vocabs.shared.domain.CaptureFormat
import com.jean.vocabs.ui.theme.LocalDarkTheme
import com.jean.vocabs.ui.theme.VocabuColors

/**
 * One color per capture type, and the same three everywhere.
 *
 * Text is plum, audio is mint, photo is the parrot red. The association forms on
 * the `+` sheet, where all three appear side by side, and is collected in
 * Pending, where the disc at the left of each row is the only thing saying where
 * that capture came from. It only works if both screens use the same source,
 * which is why this file exists instead of a `when` per screen.
 *
 * The red is **not** `error`: a photo in the queue is not a broken photo.
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
 * Each format's name. There is no separate enum for the tabs: they **are** the
 * formats, and a second enum with the same three names would only create the
 * chance of one gaining a case the other lacks.
 */
fun formatLabel(format: CaptureFormat): String = when (format) {
    CaptureFormat.TEXT -> "Texto"
    CaptureFormat.AUDIO -> "Áudio"
    CaptureFormat.PHOTO -> "Foto"
}

/** The category disc, with the right colors — the start of every Pending row. */
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
