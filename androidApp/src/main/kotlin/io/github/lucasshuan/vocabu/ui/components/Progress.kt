package io.github.lucasshuan.vocabu.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lucasshuan.vocabu.R

/**
 * A fraction drawn as an arc, with the number inside.
 *
 * The core is a slot rather than text formatted here, because Home's average
 * strength and Progress's word stock are the same shape with different content.
 * The fraction is read inside the `Canvas`, so each frame invalidates the drawing
 * and nothing else.
 */
@Composable
fun ProgressRing(
    fraction: Float,
    modifier: Modifier = Modifier,
    size: Dp = 74.dp,
    thickness: Dp = 8.dp,
    color: Color = MaterialTheme.colorScheme.tertiary,
    core: @Composable ColumnScope.() -> Unit,
) {
    val track = MaterialTheme.colorScheme.outlineVariant
    val animated = animatedFraction(fraction, "arcoDoAnel")
    Box(contentAlignment = Alignment.Center, modifier = modifier.size(size)) {
        Canvas(Modifier.fillMaxSize()) {
            val line = Stroke(thickness.toPx(), cap = StrokeCap.Round)
            drawArc(track, -90f, 360f, false, style = line)
            drawArc(color, -90f, 360f * animated.value, false, style = line)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, content = core)
    }
}

/**
 * [future] is kept apart from "no reviews" on purpose: a Saturday with nothing is
 * an empty day, a Sunday that has not arrived yet is not a failure, and painting
 * them the same would turn every Monday into a report card of lost days.
 */
data class WeekDay(
    val abbreviation: String,
    val number: Int,
    val reviews: Int,
    val today: Boolean,
    val future: Boolean,
)

/**
 * The seven days of the current week.
 *
 * [dashed] is the same week for a course with no words yet: the anatomy does not
 * change, the squares just keep their outline. Today stays marked — it is the
 * only date that exists before there is any history.
 */
@Composable
fun WeekStrip(
    days: List<WeekDay>,
    modifier: Modifier = Modifier,
    dashed: Boolean = false,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = modifier.fillMaxWidth()) {
        days.forEachIndexed { index, day ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f).smoothEntrance(index, offset = 8.dp),
            ) {
                Text(
                    text = if (day.today) stringResource(R.string.week_today) else day.abbreviation,
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
                    color = when {
                        day.today -> MaterialTheme.colorScheme.primary
                        dashed || day.future -> MaterialTheme.colorScheme.outline
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                if (dashed && !day.today) EmptySquare() else DaySquare(day)
            }
        }
    }
}

/**
 * Deliberately without the number. A faded "27" inside an empty square is the
 * date of a day when nothing happened, and seven in a row read as a lost week.
 */
@Composable
private fun EmptySquare() {
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .dashedOutline(MaterialTheme.colorScheme.outline, radius = 12.dp),
    )
}

@Composable
private fun DaySquare(day: WeekDay) {
    val colors = MaterialTheme.colorScheme
    // Two shades of mint rather than a gradient: at seven 40 dp squares a fine
    // scale is not legible. What has to read is "worked" against "worked a lot".
    val targetBackground = when {
        day.today -> colors.secondaryContainer
        day.future -> colors.surfaceVariant
        day.reviews >= FULL_DAY_REVIEWS -> colors.tertiary
        day.reviews > 0 -> colors.tertiaryContainer
        else -> colors.outlineVariant
    }
    // Today's square changes color mid-session, at the third review. The
    // transition is what makes that step get noticed.
    val background by animateColorAsState(targetBackground, tween(Motion.DEFAULT), label = "dayBackground")
    val text = when {
        day.today -> colors.primary
        day.future -> colors.outline
        day.reviews >= FULL_DAY_REVIEWS -> colors.onTertiary
        day.reviews > 0 -> colors.onTertiaryContainer
        else -> colors.onSurfaceVariant
    }

    // Resolved out here because `semantics {}` is not a composable scope.
    val description = when {
        day.future -> stringResource(R.string.a11y_day_future, day.number)
        day.reviews == 0 -> stringResource(R.string.a11y_day_no_reviews, day.number)
        else -> pluralStringResource(R.plurals.a11y_day_reviews, day.reviews, day.number, day.reviews)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(background, RoundedCornerShape(12.dp))
            .then(
                if (day.today) Modifier.border(2.dp, colors.primary, RoundedCornerShape(12.dp)) else Modifier,
            )
            .semantics { contentDescription = description },
    ) {
        Text(
            text = day.number.toString(),
            style = MaterialTheme.typography.bodySmall,
            color = text,
            textAlign = TextAlign.Center,
        )
    }
}

/** From here the day earns the strong mint. It is the steady-state daily load. */
private const val FULL_DAY_REVIEWS = 3

/**
 * Zero-weight bands disappear rather than becoming a 1 px thread, which would
 * only say a category is empty — and the legend beside it already says that.
 *
 * The stroke is `scaleX` on a `graphicsLayer` rather than animated width, so the
 * three bands are measured once and the animation stays in the draw phase.
 */
@Composable
fun BandBars(strips: List<Pair<Int, Color>>, modifier: Modifier = Modifier, height: Dp = 8.dp) {
    val visible = strips.filter { it.first > 0 }
    val stroke = animatedFraction(1f, "tracadoDaBarra")
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .graphicsLayer {
                scaleX = stroke.value
                transformOrigin = TransformOrigin(0f, 0.5f)
            },
    ) {
        if (visible.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxSize().background(MaterialTheme.colorScheme.outlineVariant, CircleShape))
            return@Row
        }
        visible.forEach { (peso, color) ->
            Box(Modifier.weight(peso.toFloat()).fillMaxSize().background(color, CircleShape))
        }
    }
}

/**
 * Progress, Day-by-day, What's left, Settings and New language open over a tab
 * and return to it. Without this arrow they would be dead ends, because the
 * bottom bar keeps marking the tab they came from.
 */
@Composable
fun InnerHeader(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    end: (@Composable () -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        CircularButton(
            icon = AppIcons.Back,
            contentDescription = stringResource(R.string.back),
            onClick = onBack,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.weight(1f).padding(start = 4.dp),
        )
        end?.invoke()
    }
}

/**
 * An informative counter with no consequence when it runs over. It is not a
 * safety quota, which is why the bar is a supporting element rather than an alert.
 */
@Composable
fun AiUsageRow(used: Int, limit: Int, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val fraction by animatedFraction(
        target = used.toFloat() / limit.coerceAtLeast(1),
        label = "aiUsageFraction",
    )
    ListRow(
        title = stringResource(R.string.ai_usage_title),
        detail = stringResource(R.string.ai_usage_detail, used, limit),
        modifier = modifier,
        start = { IconDisc(AppIcons.Brightness, null, color = colors.primary, background = colors.primaryContainer) },
        end = {
            Box(
                Modifier
                    .width(52.dp)
                    .height(6.dp)
                    .background(colors.outlineVariant, CircleShape),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(fraction)
                        .height(6.dp)
                        .background(colors.primary, CircleShape),
                )
            }
        },
    )
}

@Composable
fun SectionCount(text: String, modifier: Modifier = Modifier) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = modifier) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}
