package com.jean.vocabs.ui.components

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
import com.jean.vocabs.contracts.Language
import com.jean.vocabs.ui.languages.displayName
import com.jean.vocabs.ui.languages.flagOf

/**
 * The flag as a disc.
 *
 * The clipping is Compose's rather than the drawing's: `clip` has real
 * antialiasing, while a `clip-path` inside the VectorDrawable jags the edge on
 * some Android versions. The drawing itself is the original square art.
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
