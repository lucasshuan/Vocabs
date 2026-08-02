package com.jean.vocabs.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * Linguagem visual do app: fundo neutro, cartões brancos, tinta quase preta,
 * azul para ação/palavra e laranja para expressão. As palavras capturadas
 * aparecem sempre em serif itálico — é a assinatura tipográfica do app.
 */

private val CoresClaras = lightColorScheme(
    primary = Color(0xFF189BEF),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDCF0FE),
    onPrimaryContainer = Color(0xFF085A8C),
    secondary = Color(0xFF4B5563),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE8EAEE),
    onSecondaryContainer = Color(0xFF2A3038),
    tertiary = Color(0xFFEF8318),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFCEBD6),
    onTertiaryContainer = Color(0xFF8A4A04),
    background = Color(0xFFF3F4F6),
    onBackground = Color(0xFF15181D),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF15181D),
    surfaceVariant = Color(0xFFEAECEF),
    onSurfaceVariant = Color(0xFF697077),
    outline = Color(0xFFD8DCE1),
    outlineVariant = Color(0xFFE7EAED),
    inverseSurface = Color(0xFF1A1D23),
    inverseOnSurface = Color(0xFFF2F3F5),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFDE7E1),
    onErrorContainer = Color(0xFF7E2B18),
)

private val CoresEscuras = darkColorScheme(
    primary = Color(0xFF58BEF9),
    onPrimary = Color(0xFF00344F),
    primaryContainer = Color(0xFF0B4A70),
    onPrimaryContainer = Color(0xFFCBE9FD),
    secondary = Color(0xFFB6BCC6),
    onSecondary = Color(0xFF232831),
    secondaryContainer = Color(0xFF2B313A),
    onSecondaryContainer = Color(0xFFDDE1E8),
    tertiary = Color(0xFFF7A455),
    onTertiary = Color(0xFF4A2800),
    tertiaryContainer = Color(0xFF643D0A),
    onTertiaryContainer = Color(0xFFFCDEBE),
    background = Color(0xFF0F1115),
    onBackground = Color(0xFFE8EAED),
    surface = Color(0xFF181B21),
    onSurface = Color(0xFFE8EAED),
    surfaceVariant = Color(0xFF242830),
    onSurfaceVariant = Color(0xFF9AA1AA),
    outline = Color(0xFF3A4048),
    outlineVariant = Color(0xFF2B3038),
    inverseSurface = Color(0xFFE8EAED),
    inverseOnSurface = Color(0xFF181B21),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

/**
 * Serif itálico é reservado para as palavras capturadas e títulos de tela;
 * todo o resto fica no sans padrão. Um estilo só de fonte já diferencia o
 * conteúdo (a palavra) da interface ao redor dele.
 */
private val Fontes: Typography = Typography().let { base ->
    val palavra = base.headlineLarge.copy(
        fontFamily = FontFamily.Serif,
        fontStyle = FontStyle.Italic,
        fontWeight = FontWeight.Bold,
    )
    base.copy(
        displaySmall = palavra.copy(fontSize = 38.sp, lineHeight = 44.sp),
        headlineLarge = palavra.copy(fontSize = 32.sp, lineHeight = 38.sp),
        headlineMedium = palavra.copy(fontSize = 24.sp, lineHeight = 30.sp),
        titleLarge = palavra.copy(fontSize = 21.sp, lineHeight = 26.sp),
        labelSmall = base.labelSmall.copy(
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.4.sp,
            fontSize = 11.sp,
        ),
    )
}

private val Formas = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun VocabsTheme(
    temaEscuro: Boolean = isSystemInDarkTheme(),
    conteudo: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (temaEscuro) CoresEscuras else CoresClaras,
        typography = Fontes,
        shapes = Formas,
        content = conteudo,
    )
}
