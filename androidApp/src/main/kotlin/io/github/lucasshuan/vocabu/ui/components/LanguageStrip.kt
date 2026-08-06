package io.github.lucasshuan.vocabu.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.lucasshuan.vocabu.R
import io.github.lucasshuan.vocabu.contracts.Language
import io.github.lucasshuan.vocabu.shared.domain.CourseBadge
import io.github.lucasshuan.vocabu.shared.domain.CourseSummary
import io.github.lucasshuan.vocabu.ui.languages.displayName
import io.github.lucasshuan.vocabu.ui.languages.languageOf

/**
 * Fixed order, never reordered by what is due: a swipe is only cheap while
 * muscle memory knows Spanish sits right of English.
 *
 * Every flag is badged, never empty and never a written "0": zero is the strip's
 * only good news, and as a number it reads as a scoreboard of nothing done.
 */
@Composable
fun LanguageStrip(
    courses: List<CourseSummary>,
    activeTarget: String,
    onChoose: (String) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
    filling: PaddingValues = PaddingValues(horizontal = 20.dp),
) {
    val state = rememberLazyListState()
    val activeIndex = courses.indexOfFirst { it.target == activeTarget }

    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0) state.animateScrollToItem(activeIndex)
    }

    LazyRow(
        state = state,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = filling,
        modifier = modifier.fillMaxWidth(),
    ) {
        items(courses.size, key = { courses[it].target }) { index ->
            val course = courses[index]
            LanguageChip(
                language = languageOf(course.target),
                badge = course.badge,
                selected = course.target == activeTarget,
                onClick = { onChoose(course.target) },
            )
        }
        item(key = "add") { AddLanguageChip(onAdd) }
    }
}

/**
 * The active chip inverts its badge: the chip itself went plum, and a plum badge
 * on plum vanishes on the one page that is open.
 */
@Composable
fun LanguageChip(
    language: Language,
    badge: CourseBadge,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val background by animateColorAsState(
        targetValue = if (selected) colors.primary else colors.surface,
        animationSpec = tween(Motion.FAST),
        label = "chipBackground",
    )
    val text by animateColorAsState(
        targetValue = if (selected) colors.onPrimary else colors.onSurfaceVariant,
        animationSpec = tween(Motion.FAST),
        label = "chipText",
    )

    val touch = rememberHaptics()
    // Hoisted: `semantics` is not a composable scope.
    val name = language.displayName
    val description = badgeDescription(name, badge)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .shrinkOnTouch(touch, minimum = 0.94f)
            .height(38.dp)
            .clip(CircleShape)
            .background(background)
            .then(
                if (selected) Modifier
                else Modifier.border(1.dp, colors.outline, CircleShape),
            )
            .clickable(interactionSource = touch, indication = ripple(), onClick = onClick)
            .padding(start = 8.dp, end = 10.dp)
            .semantics { contentDescription = description },
    ) {
        CircularFlag(language, size = 24.dp)
        Text(text = name, style = MaterialTheme.typography.titleSmall, color = text)
        CourseBadgeView(badge, inverted = selected)
    }
}

/**
 * The number changes on a short vertical transition: the "3" rises and the "2"
 * enters underneath, which says it went down without anything else moving.
 */
@Composable
private fun CourseBadgeView(badge: CourseBadge, inverted: Boolean) {
    val colors = MaterialTheme.colorScheme
    val background = when {
        badge is CourseBadge.Review && inverted -> colors.onPrimary
        badge is CourseBadge.Review -> colors.secondaryContainer
        badge is CourseBadge.UpToDate -> colors.tertiaryContainer
        else -> colors.surfaceVariant
    }
    val tint = when {
        badge is CourseBadge.Review && inverted -> colors.primary
        badge is CourseBadge.Review -> colors.primary
        badge is CourseBadge.UpToDate -> colors.tertiary
        else -> colors.onSurfaceVariant
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .defaultMinSize(minWidth = 20.dp)
            .height(20.dp)
            .background(background, CircleShape),
    ) {
        when (badge) {
            is CourseBadge.Review -> AnimatedContent(
                targetState = badge.count,
                transitionSpec = {
                    val rising = targetState < initialState
                    val entry = slideInVertically { height -> if (rising) height else -height } + fadeIn(tween(140))
                    val exit = slideOutVertically { height -> if (rising) -height else height } + fadeOut(tween(140))
                    entry togetherWith exit
                },
                label = "badgeCount",
            ) { count ->
                Text(
                    text = count.coerceAtMost(99).toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = tint,
                    modifier = Modifier.padding(horizontal = 6.dp),
                )
            }
            CourseBadge.UpToDate -> Icon(AppIcons.Check, null, tint = tint, modifier = Modifier.size(12.dp))
            CourseBadge.Empty -> Icon(AppIcons.Hourglass, null, tint = tint, modifier = Modifier.size(12.dp))
        }
    }
}

@Composable
private fun AddLanguageChip(onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val addLabel = stringResource(R.string.a11y_add_language)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .height(38.dp)
            .width(42.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .dashedOutline(colors.outline, radius = 19.dp)
            .semantics { contentDescription = addLabel },
    ) {
        Icon(AppIcons.Plus, null, tint = colors.primary, modifier = Modifier.size(20.dp))
    }
}

/**
 * "All" is the only one without a flag: it is the absence of a slice, not one
 * more language in the row.
 */
@Composable
fun LanguageFilterPill(
    label: String,
    language: Language?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val background by animateColorAsState(
        targetValue = if (selected) colors.primary else colors.surface,
        animationSpec = tween(Motion.FAST),
        label = "filterBackground",
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        modifier = modifier
            .height(32.dp)
            .clip(CircleShape)
            .background(background)
            .then(if (selected) Modifier else Modifier.border(1.dp, colors.outline, CircleShape))
            .clickable(onClick = onClick)
            .padding(start = if (language == null) 13.dp else 6.dp, end = 13.dp),
    ) {
        language?.let { CircularFlag(it, size = 20.dp) }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) colors.onPrimary else colors.onSurfaceVariant,
        )
    }
}

@Composable
fun LanguageMark(
    language: Language,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    flagSize: androidx.compose.ui.unit.Dp = 15.dp,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier,
    ) {
        CircularFlag(language, size = flagSize)
        Text(language.displayName, style = MaterialTheme.typography.bodySmall, color = color)
    }
}

@Composable
fun PageDots(total: Int, current: Int, modifier: Modifier = Modifier) {
    if (total <= 1) return
    val colors = MaterialTheme.colorScheme
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = modifier) {
        repeat(total) { index ->
            val isCurrent = index == current
            val width by androidx.compose.animation.core.animateDpAsState(
                targetValue = if (isCurrent) 16.dp else 5.dp,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "dotWidth",
            )
            val color by animateColorAsState(
                targetValue = if (isCurrent) colors.primary else colors.outlineVariant,
                animationSpec = tween(Motion.FAST),
                label = "dotColor",
            )
            Box(Modifier.width(width).height(5.dp).background(color, CircleShape))
        }
    }
}

@Composable
private fun badgeDescription(name: String, badge: CourseBadge): String = when (badge) {
    is CourseBadge.Review -> stringResource(R.string.a11y_course_review, name, badge.count)
    CourseBadge.UpToDate -> stringResource(R.string.a11y_course_up_to_date, name)
    CourseBadge.Empty -> stringResource(R.string.a11y_course_empty, name)
}
