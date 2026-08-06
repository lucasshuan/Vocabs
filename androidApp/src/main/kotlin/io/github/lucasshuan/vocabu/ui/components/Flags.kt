package io.github.lucasshuan.vocabu.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.lucasshuan.vocabu.contracts.Language
import io.github.lucasshuan.vocabu.ui.languages.displayName
import io.github.lucasshuan.vocabu.ui.languages.flagOf

/**
 * Clipped by Compose, not the drawable: `clip` antialiases, while a `clip-path`
 * inside a VectorDrawable jags the edge on some Android versions.
 */
@Composable
fun CircularFlag(language: Language, modifier: Modifier = Modifier, size: Dp = 20.dp) {
    Image(
        painter = painterResource(flagOf(language)),
        contentDescription = language.displayName,
        contentScale = ContentScale.Crop,
        modifier = modifier.size(size).clip(CircleShape),
    )
}
