package io.github.lucasshuan.vocabu.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lucasshuan.vocabu.R
import io.github.lucasshuan.vocabu.shared.ThemePreference

/** Semantic colors with no role of their own in Material's ColorScheme. */
object VocabuColors {
    val Mint = Color(0xFF5FB33C)
    val MintDark = Color(0xFF3F8A22)

    /**
     * Photo's category colour. Never `error`: that would make every queued photo
     * look like a broken one.
     */
    val Parrot = Color(0xFFC4243C)
    val ParrotContainer = Color(0xFFFBEAEC)
    val ParrotDark = Color(0xFFF08696)
    val ParrotContainerDark = Color(0xFF3A2028)

    /**
     * The page dot nobody is on. Darker than the ring's empty track, which sits
     * on a card: these sit on the background, where the track disappears.
     */
    val Quartz = Color(0xFFDCD2E6)
    val QuartzDark = Color(0xFF3E3350)
}

/**
 * The light card has a 1px outline against the near-white background and the
 * dark one has none. Without this signal each screen guesses, which is how half
 * of them ended up outlined in dark.
 */
val LocalDarkTheme = staticCompositionLocalOf { false }

private val LightColors = lightColorScheme(
    primary = Color(0xFF8C34B4),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF7EDFC),
    onPrimaryContainer = Color(0xFF6B1E8C),
    secondary = Color(0xFF6C6178),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDE6F4),
    onSecondaryContainer = Color(0xFF33273F),
    tertiary = VocabuColors.MintDark,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFDDF3D1),
    onTertiaryContainer = Color(0xFF19430A),
    background = Color(0xFFFAF7FD),
    onBackground = Color(0xFF221A2C),
    surface = Color.White,
    onSurface = Color(0xFF221A2C),
    surfaceVariant = Color(0xFFEDE6F4),
    onSurfaceVariant = Color(0xFF6C6178),
    outline = Color(0xFFE4DAEE),
    outlineVariant = Color(0xFFEDE6F4),
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB45CDA),
    onPrimary = Color(0xFF1B1322),
    primaryContainer = Color(0xFF2E1E3C),
    onPrimaryContainer = Color(0xFFE3B6F7),
    secondary = Color(0xFFA497B4),
    onSecondary = Color(0xFF1B1322),
    secondaryContainer = Color(0xFF332942),
    onSecondaryContainer = Color(0xFFF4EFF8),
    tertiary = VocabuColors.Mint,
    onTertiary = Color(0xFF1B1322),
    tertiaryContainer = Color(0xFF294B1A),
    onTertiaryContainer = Color(0xFFD9F4CA),
    background = Color(0xFF17121D),
    onBackground = Color(0xFFF4EFF8),
    surface = Color(0xFF241C2E),
    onSurface = Color(0xFFF4EFF8),
    surfaceVariant = Color(0xFF332942),
    onSurfaceVariant = Color(0xFFA497B4),
    outline = Color(0xFF362B44),
    outlineVariant = Color(0xFF332942),
    error = Color(0xFFF2837C),
    onError = Color(0xFF1B1322),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

private val Figtree = FontFamily(
    Font(R.font.figtree_variable, FontWeight.Normal),
    Font(R.font.figtree_variable, FontWeight.Medium),
    Font(R.font.figtree_variable, FontWeight.SemiBold),
    Font(R.font.figtree_variable, FontWeight.Bold),
)

val Bricolage = FontFamily(
    Font(R.font.bricolage_grotesque_variable, FontWeight.Medium),
    Font(R.font.bricolage_grotesque_variable, FontWeight.Bold),
    Font(R.font.bricolage_grotesque_variable, FontWeight.ExtraBold),
)

private val Type = Typography(
    displaySmall = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold, fontSize = 36.sp, lineHeight = 42.sp),
    headlineLarge = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp, lineHeight = 35.sp),
    headlineMedium = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp),
    headlineSmall = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 21.sp, lineHeight = 27.sp),
    titleLarge = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.Bold, fontSize = 10.sp, lineHeight = 14.sp, letterSpacing = 0.8.sp),
)

private val Shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(30.dp),
)

/**
 * `SYSTEM` is the only value that consults the device: `LIGHT` and `DARK` are
 * decisions, and do not reverse when the system changes mood at night.
 */
@Composable
fun darkAccordingTo(preference: ThemePreference): Boolean = when (preference) {
    ThemePreference.LIGHT -> false
    ThemePreference.DARK -> true
    ThemePreference.SYSTEM -> isSystemInDarkTheme()
}

@Composable
fun VocabsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = Type,
            shapes = Shapes,
            content = content,
        )
    }
}
