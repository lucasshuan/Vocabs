package com.jean.vocabs.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jean.vocabs.R
import com.jean.vocabs.shared.domain.CourseSummary
import com.jean.vocabs.ui.components.AiUsageRow
import com.jean.vocabs.ui.components.AppIcons
import com.jean.vocabs.ui.components.CircularFlag
import com.jean.vocabs.ui.components.DashedBox
import com.jean.vocabs.ui.components.IconDisc
import com.jean.vocabs.ui.components.ListRow
import com.jean.vocabs.ui.components.RowChevron
import com.jean.vocabs.ui.components.ScreenCard
import com.jean.vocabs.ui.components.SectionLabel
import com.jean.vocabs.ui.components.animatedCount
import com.jean.vocabs.ui.components.animatedFraction
import com.jean.vocabs.ui.languages.displayName
import com.jean.vocabs.ui.languages.languageOf

/**
 * "You".
 *
 * Totals before the breakdown: streak and stock are habit, and habit does not
 * belong to a course. Each language row opens "Your progress" for **that** course
 * without switching the open course underneath someone who only wanted a look.
 *
 * Switching language left this screen — it is Home's swipe now. The native
 * language left later, for Settings: it is about no course at all, and at the
 * foot of a list where every row opens one it read as one more of them.
 */
@Composable
fun ProfileScreen(
    onOpenProgress: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenNewLanguage: () -> Unit,
    vm: ProfileViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        Text(stringResource(R.string.profile_title), style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(top = 22.dp))

        ScreenCard(filling = PaddingValues(16.dp), modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.profile_total), style = MaterialTheme.typography.titleLarge)
            Text(
                text = stringResource(R.string.profile_across_languages),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // All three count up from zero. They are the balance of the whole
            // habit across every language, and the only screen where these
            // numbers are the subject rather than a supporting detail.
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 14.dp)) {
                SummaryNumber(
                    value = "${animatedCount(state.totalMastered, "masteredTotal")}",
                    label = stringResource(if (state.totalMastered == 1) R.string.profile_mastered_one else R.string.profile_mastered_other),
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f),
                )
                VerticalDivider()
                SummaryNumber(
                    value = "${animatedCount(state.dayStreak, "totalDayStreak")}",
                    label = stringResource(if (state.dayStreak == 1) R.string.profile_day_streak_one else R.string.profile_day_streak_other),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                VerticalDivider()
                SummaryNumber(
                    value = "${animatedCount(state.totalCards, "cardsTotal")}",
                    label = stringResource(if (state.totalCards == 1) R.string.profile_cards_one else R.string.profile_cards_other),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        ProgressByLanguage(
            courses = state.courses,
            onOpen = onOpenProgress,
            onAdd = onOpenNewLanguage,
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        AiUsageRow(used = state.aiUsage.used, limit = state.aiUsage.limit)

        // The subtext is not decoration: the native language used to be a row on
        // this screen and is now one tap deeper. Without naming it here, anyone
        // looking where it was has no clue where to look instead.
        ListRow(
            title = stringResource(R.string.profile_settings),
            detail = stringResource(R.string.profile_settings_detail),
            onClick = onOpenSettings,
            start = { IconDisc(AppIcons.Gear, null, color = MaterialTheme.colorScheme.onSurfaceVariant, background = MaterialTheme.colorScheme.surfaceVariant) },
            end = { RowChevron() },
        )
        Spacer(Modifier.navigationBarsPadding().height(110.dp))
    }
}

/**
 * The course list in a box with its own scroll.
 *
 * Without the height cap, studying six languages would push "AI generations" and
 * "Settings" off the first screen — and those are exactly the rows nobody finds
 * by scrolling, because they never change.
 */
@Composable
private fun ProgressByLanguage(
    courses: List<CourseSummary>,
    onOpen: (String) -> Unit,
    onAdd: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val scrollable = courses.size > 3

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
            SectionLabel(stringResource(R.string.profile_progress_by_language), Modifier.weight(1f))
            if (scrollable) {
                Text(stringResource(R.string.profile_scroll_hint), style = MaterialTheme.typography.bodySmall, color = colors.outline)
            }
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surfaceVariant, RoundedCornerShape(22.dp))
                .heightIn(max = MAX_LIST_HEIGHT)
                .verticalScroll(rememberScrollState())
                .padding(8.dp),
        ) {
            courses.forEach { course ->
                CourseRow(course) { onOpen(course.target) }
            }
            DashedBox(
                modifier = Modifier.fillMaxWidth(),
                onClick = onAdd,
                filling = PaddingValues(horizontal = 15.dp, vertical = 11.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(30.dp).background(colors.surface, CircleShape),
                    ) {
                        Icon(AppIcons.Plus, null, tint = colors.primary, modifier = Modifier.size(18.dp))
                    }
                    Text(
                        text = stringResource(R.string.add_language),
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.primary,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun CourseRow(course: CourseSummary, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val fraction by animatedFraction(
        target = if (course.total == 0) 0f else course.mastered.toFloat() / course.total,
        label = "courseFraction",
    )

    ListRow(
        onClick = onClick,
        start = { CircularFlag(languageOf(course.target), size = 30.dp) },
        end = { RowChevron() },
    ) {
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = languageOf(course.target).displayName,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.fraction, course.mastered, course.total),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .padding(top = 5.dp)
                .height(6.dp)
                .background(colors.outlineVariant, CircleShape),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .height(6.dp)
                    .background(colors.tertiary, CircleShape),
            )
        }
    }
}

@Composable
private fun SummaryNumber(value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(value, style = MaterialTheme.typography.headlineMedium, color = color)
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun VerticalDivider() {
    Box(Modifier.width(1.dp).height(34.dp).background(MaterialTheme.colorScheme.outlineVariant))
}

/** Three and a half rows: the half says there is more. */
private val MAX_LIST_HEIGHT = 232.dp
