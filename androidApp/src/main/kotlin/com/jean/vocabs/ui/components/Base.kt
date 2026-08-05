package com.jean.vocabs.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jean.vocabs.ui.theme.LocalDarkTheme

/**
 * The blocks every screen repeats.
 *
 * Each one exists because the same shape appeared in three or four places and was
 * drifting in each: the card that gained an outline in dark on one screen but not
 * another, the big number centered here and left-aligned there.
 */

/**
 * The card outline — deliberately null in dark.
 *
 * In light the surface is white on a near-white background and needs the line to
 * exist at all; in dark the surface is already lighter than the background and
 * the line would only dirty the edge.
 */
@Composable
fun cardOutline(): BorderStroke? =
    if (LocalDarkTheme.current) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline)

/**
 * The dashed outline of "something else fits here": the box exists, the content
 * does not yet. A solid outline would say "this is a card".
 *
 * Hand-drawn because `BorderStroke` does not accept a `PathEffect`.
 */
fun Modifier.dashedOutline(
    color: Color,
    radius: Dp = 18.dp,
    thickness: Dp = 1.dp,
): Modifier = drawBehind {
    val line = thickness.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(line / 2f, line / 2f),
        size = Size(size.width - line, size.height - line),
        cornerRadius = CornerRadius(radius.toPx()),
        style = Stroke(
            width = line,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(line * 5, line * 4)),
        ),
    )
}

/** The dashed box, ready to use. */
@Composable
fun DashedBox(
    modifier: Modifier = Modifier,
    radius: Dp = 18.dp,
    color: Color = MaterialTheme.colorScheme.outline,
    filling: PaddingValues = PaddingValues(horizontal = 15.dp, vertical = 12.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(radius)
    val toque = rememberHaptics()
    val base = modifier
        .then(if (onClick != null) Modifier.shrinkOnTouch(toque) else Modifier)
        .clip(shape)
        .then(
            if (onClick != null) Modifier.clickable(interactionSource = toque, indication = ripple(), onClick = onClick)
            else Modifier,
        )
        .dashedOutline(color, radius)
        .padding(filling)
    Column(modifier = base, content = content)
}

/**
 * The default content surface.
 *
 * When clickable it gives under the finger. The shrink lives here rather than at
 * each call because it is exactly the kind of detail that only survives if it is
 * free: there are dozens of clickable cards, and no screen would remember to ask
 * for it one at a time.
 */
@Composable
fun ScreenCard(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    color: Color = MaterialTheme.colorScheme.surface,
    filling: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val outline = cardOutline()
    val inner: @Composable () -> Unit = {
        Column(modifier = Modifier.padding(filling), content = content)
    }
    if (onClick == null) {
        Surface(shape = shape, color = color, border = outline, modifier = modifier, content = inner)
    } else {
        val toque = rememberHaptics()
        Surface(
            onClick = onClick,
            shape = shape,
            color = color,
            border = outline,
            interactionSource = toque,
            modifier = modifier.shrinkOnTouch(toque),
            content = inner,
        )
    }
}

/**
 * The supporting label above each block.
 *
 * Normal case at 12 sp: the spaced small-caps of the older design competed in
 * weight with the content it was announcing.
 */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

/** The big-number tile, shared by Home, Profile and the review summary. */
@Composable
fun MetricCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    highlight: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    ScreenCard(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        filling = PaddingValues(horizontal = 14.dp, vertical = 13.dp),
        onClick = onClick,
    ) {
        // Deliberately no transition on the number. The tile takes an already
        // formatted `String`, and a caller wanting it to count up passes the
        // result of `animatedCount`. An `AnimatedContent` here would fire one
        // transition per frame in exactly that case.
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = if (highlight) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

/** The list row with icon and value at the ends. */
@Composable
fun ListRow(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    start: (@Composable () -> Unit)? = null,
    end: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    ScreenCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        filling = PaddingValues(horizontal = 15.dp, vertical = 14.dp),
        onClick = onClick,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            start?.let {
                it()
                Spacer(Modifier.width(13.dp))
            }
            Column(Modifier.weight(1f), content = content)
            end?.let {
                Spacer(Modifier.width(10.dp))
                it()
            }
        }
    }
}

/** The common case of [ListRow]: title plus one supporting line. */
@Composable
fun ListRow(
    title: String,
    modifier: Modifier = Modifier,
    detail: String? = null,
    onClick: (() -> Unit)? = null,
    start: (@Composable () -> Unit)? = null,
    end: (@Composable () -> Unit)? = null,
) {
    ListRow(modifier = modifier, onClick = onClick, start = start, end = end) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        detail?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

/** The colored disc that opens Pending's rows. */
@Composable
fun IconDisc(
    icon: ImageVector,
    contentDescription: String?,
    color: Color,
    background: Color,
    size: Dp = 38.dp,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(size).background(background, CircleShape),
    ) {
        Icon(icon, contentDescription, tint = color, modifier = Modifier.size(size * 0.52f))
    }
}

/** The "this opens another screen" chevron. */
@Composable
fun RowChevron() {
    Icon(
        imageVector = AppIcons.Forward,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(20.dp),
    )
}

/** The screen's primary action: plum, full width, 18 dp corners. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    initialContent: (@Composable RowScope.() -> Unit)? = null,
) {
    val toque = rememberHaptics()
    // Less than the cards (0.97): this button spans the screen, and the same
    // ratio on a target that size reads as a jolt rather than a touch.
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(18.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        interactionSource = toque,
        modifier = modifier.shrinkOnTouch(toque, minimum = 0.985f).fillMaxWidth().height(56.dp),
    ) {
        initialContent?.invoke(this)
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}

/** Words' filter pill and Capture's format tab are the same thing. */
@Composable
fun SelectablePill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
) {
    val colors = MaterialTheme.colorScheme
    val toque = rememberHaptics()
    // The colors transition rather than cutting between frames: in a row of
    // filters a hard cut makes two pills blink at once, and neither says where
    // the selection came from.
    val background by animateColorAsState(
        targetValue = if (isSelected) colors.primary else colors.surface,
        animationSpec = tween(Motion.FAST),
        label = "fundoDaPilula",
    )
    val tinta by animateColorAsState(
        targetValue = if (isSelected) colors.onPrimary else colors.onSurfaceVariant,
        animationSpec = tween(Motion.FAST),
        label = "tintaDaPilula",
    )
    Surface(
        onClick = onClick,
        shape = shape,
        color = background,
        contentColor = tinta,
        border = if (isSelected) null else cardOutline(),
        interactionSource = toque,
        modifier = modifier.shrinkOnTouch(toque, minimum = 0.94f),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp)) {
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = tinta)
        }
    }
}

/**
 * A content pill: related term, "see more", "try again".
 *
 * With [highlight] it turns plum on lilac — the difference between "this is one
 * more word" and "this does something".
 */
@Composable
fun Pill(
    text: String,
    modifier: Modifier = Modifier,
    highlight: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    val background = if (highlight) colors.secondaryContainer else colors.surface
    val color = if (highlight) colors.primary else colors.onSurface
    val outline = if (highlight) null else cardOutline()
    val content: @Composable () -> Unit = {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = color,
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
        )
    }
    if (onClick == null) {
        Surface(shape = CircleShape, color = background, border = outline, modifier = modifier, content = content)
    } else {
        val toque = rememberHaptics()
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = background,
            border = outline,
            interactionSource = toque,
            modifier = modifier.shrinkOnTouch(toque, minimum = 0.94f),
            content = content,
        )
    }
}

/**
 * The empty state of Words, Pending and Review.
 *
 * The disc springs in and the text rises behind it. An empty screen is the one
 * place with no content to hold the eye, and arriving fully assembled makes
 * "Memory up to date" look like a loading failure instead of the good outcome it
 * is.
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    detail: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    var arrived by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { arrived = true }
    val scale by animateFloatAsState(
        targetValue = if (arrived) 1f else 0.6f,
        animationSpec = Motion.elasticSpring(),
        label = "escalaDoVazio",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 60.dp),
    ) {
        Box(Modifier.graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (arrived) 1f else 0f }) {
            IconDisc(
                icon = icon,
                contentDescription = null,
                color = MaterialTheme.colorScheme.tertiary,
                background = MaterialTheme.colorScheme.tertiaryContainer,
                size = 72.dp,
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.smoothEntrance(index = 1).padding(top = 16.dp),
        )
        Text(
            text = detail,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.smoothEntrance(index = 2).padding(top = 4.dp),
        )
        action?.let {
            Box(Modifier.smoothEntrance(index = 3).padding(top = 20.dp)) { it() }
        }
    }
}

/** The round header button (back, close, discard). */
@Composable
fun CircularButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    val toque = rememberHaptics()
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.Transparent,
        contentColor = color,
        interactionSource = toque,
        modifier = Modifier.shrinkOnTouch(toque, minimum = 0.88f),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(44.dp)) {
            Icon(icon, contentDescription, tint = color, modifier = Modifier.size(24.dp))
        }
    }
}

/** The quiet text button, centered under the primary action. */
@Composable
fun SecondaryAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth(),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.height(48.dp)) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
