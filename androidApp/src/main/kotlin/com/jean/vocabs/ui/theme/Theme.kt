package com.jean.vocabs.ui.theme

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
import com.jean.vocabs.R

/** Cores semânticas que não têm um papel próprio no ColorScheme do Material. */
object TagararaColors {
    val Mint = Color(0xFF8FDC5E)
    val MintDark = Color(0xFF3F8A22)
}

/**
 * Se o tema corrente é o escuro.
 *
 * Não é preferência de estilo: no handoff o cartão claro tem contorno de 1 px
 * para se separar do fundo quase branco, e o escuro não tem nenhum — a própria
 * superfície já se destaca. Sem esse sinal cada tela adivinharia sozinha, que é
 * como metade delas acabou com contorno no escuro.
 */
val LocalTemaEscuro = staticCompositionLocalOf { false }

private val LightColors = lightColorScheme(
    primary = Color(0xFF8C34B4),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEED9F7),
    onPrimaryContainer = Color(0xFF4C1264),
    secondary = Color(0xFF6C6178),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF1EBF8),
    onSecondaryContainer = Color(0xFF33273F),
    tertiary = TagararaColors.MintDark,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFDDF3D1),
    onTertiaryContainer = Color(0xFF19430A),
    background = Color(0xFFFAF7FD),
    onBackground = Color(0xFF221A2C),
    surface = Color.White,
    onSurface = Color(0xFF221A2C),
    surfaceVariant = Color(0xFFF1EBF8),
    onSurfaceVariant = Color(0xFF6C6178),
    outline = Color(0xFFE4DAEE),
    outlineVariant = Color(0xFFEDE6F4),
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFBC7BE8),
    onPrimary = Color(0xFF1A1222),
    primaryContainer = Color(0xFF4A275E),
    onPrimaryContainer = Color(0xFFEFD9FF),
    secondary = Color(0xFFB8ACCA),
    onSecondary = Color(0xFF271F30),
    secondaryContainer = Color(0xFF332A40),
    onSecondaryContainer = Color(0xFFE6DDF0),
    tertiary = TagararaColors.Mint,
    onTertiary = Color(0xFF183109),
    tertiaryContainer = Color(0xFF294B1A),
    onTertiaryContainer = Color(0xFFD9F4CA),
    background = Color(0xFF14101A),
    onBackground = Color(0xFFF0EAF6),
    surface = Color(0xFF1E1826),
    onSurface = Color(0xFFF0EAF6),
    surfaceVariant = Color(0xFF2A2234),
    onSurfaceVariant = Color(0xFF9A8FA8),
    outline = Color(0xFF332A40),
    outlineVariant = Color(0xFF2A2234),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
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

@Composable
fun VocabsTheme(
    temaEscuro: Boolean = isSystemInDarkTheme(),
    conteudo: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalTemaEscuro provides temaEscuro) {
        MaterialTheme(
            colorScheme = if (temaEscuro) DarkColors else LightColors,
            typography = Type,
            shapes = Shapes,
            content = conteudo,
        )
    }
}
