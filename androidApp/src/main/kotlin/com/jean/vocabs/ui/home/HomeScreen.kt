package com.jean.vocabs.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jean.vocabs.R
import com.jean.vocabs.shared.domain.Entry
import com.jean.vocabs.ui.components.AppIcons
import com.jean.vocabs.ui.components.DashedBox
import com.jean.vocabs.ui.components.IconDisc
import com.jean.vocabs.ui.components.LanguageStrip
import com.jean.vocabs.ui.components.PageDots
import com.jean.vocabs.ui.components.PrimaryButton
import com.jean.vocabs.ui.components.ProgressRing
import com.jean.vocabs.ui.components.ScreenCard
import com.jean.vocabs.ui.components.SectionLabel
import com.jean.vocabs.ui.components.animatedCount
import com.jean.vocabs.ui.components.entryTitle
import com.jean.vocabs.ui.components.smoothEntrance
import com.jean.vocabs.ui.components.timeUntil
import com.jean.vocabs.ui.languages.displayName
import com.jean.vocabs.ui.languages.languageOf

/**
 * Home — one page per course.
 *
 * The **only** tab sliced by language, and the switch is a swipe. The other three
 * always show everything: a filter left on across a tab change would make words
 * disappear without anyone asking.
 *
 * Swiping does not just navigate — it changes the open course. That is why the
 * review button and the `+` sheet follow the visible page without either needing
 * to know a carousel exists.
 */
@Composable
fun HomeScreen(
    onCapture: () -> Unit,
    onReview: () -> Unit,
    onOpenProfile: () -> Unit,
    onAddLanguage: () -> Unit,
    vm: HomeViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val pages = state.pages
    val pager = rememberPagerState(pageCount = { pages.size })

    // Two directions, and the order between them matters. The pager starts on
    // page 0, which is almost never the open course; letting it lead before it is
    // positioned would silently switch the course to English on every launch. So
    // it only leads after the first positioning.
    var positioned by remember { mutableStateOf(false) }

    LaunchedEffect(pages.size, state.activeTarget) {
        if (pages.isEmpty()) return@LaunchedEffect
        if (!positioned) {
            pager.scrollToPage(state.activeIndex)
            positioned = true
        } else if (state.activeIndex != pager.currentPage && !pager.isScrollInProgress) {
            pager.animateScrollToPage(state.activeIndex)
        }
    }

    LaunchedEffect(positioned) {
        if (!positioned) return@LaunchedEffect
        snapshotFlow { pager.settledPage }.collect { index ->
            pages.getOrNull(index)?.let { vm.openCourse(it.target) }
        }
    }

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 14.dp),
        ) {
            Image(painterResource(R.drawable.logo_vocabu), stringResource(R.string.logo_description), Modifier.size(34.dp))
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 9.dp))
            Spacer(Modifier.weight(1f))
            Surface(onClick = onOpenProfile, shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(34.dp)) {
                    Icon(
                        imageVector = AppIcons.Person,
                        contentDescription = stringResource(R.string.a11y_you),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }

        if (state.hasCarousel) {
            LanguageStrip(
                courses = state.courses,
                activeTarget = state.activeTarget,
                onChoose = vm::openCourse,
                onAdd = onAddLanguage,
                modifier = Modifier.padding(bottom = 14.dp),
            )
        }

        HorizontalPager(
            state = pager,
            beyondViewportPageCount = 1,
            modifier = Modifier.weight(1f),
        ) { index ->
            pages.getOrNull(index)?.let { page ->
                CoursePage(
                    page = page,
                    onReview = onReview,
                    onCapture = onCapture,
                )
            }
        }

        // Outside the pager on purpose: the dots belong to no page and no card,
        // and stay put no matter what changes above.
        PageDots(
            total = pages.size,
            current = pager.currentPage,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 10.dp),
        )
        Spacer(Modifier.navigationBarsPadding().height(BAR_SPACING))
    }
}

@Composable
private fun CoursePage(
    page: HomePage,
    onReview: () -> Unit,
    onCapture: () -> Unit,
) {
    val language = languageOf(page.target)
    val summary = page.summary

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        ScreenCard(
            shape = MaterialTheme.shapes.extraLarge,
            filling = PaddingValues(18.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CourseRing(page)
                Column(Modifier.weight(1f).padding(start = 15.dp)) {
                    Text(stringResource(R.string.home_your_language, language.displayName), style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = courseDetail(summary.total, summary.mastered, summary.inQueue),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }

            if (summary.inQueue > 0) {
                PrimaryButton(
                    text = pluralStringResource(R.plurals.home_review_words, summary.inQueue, summary.inQueue),
                    onClick = onReview,
                    modifier = Modifier.padding(top = 15.dp),
                )
            } else {
                UpNextRow(page, Modifier.padding(top = 15.dp))
            }
        }

        // The day's captures enter staggered. This is the list that grows while
        // the app is used, and the only place on Home where the day is seen
        // accumulating — arriving fully assembled would make three captures look
        // like old history instead of what happened today.
        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            SectionLabel(stringResource(R.string.home_captured_today_in, language.displayName))
            if (page.capturedToday.isEmpty()) {
                CaptureInvite(language.displayName.lowercase(), onCapture)
            } else {
                page.capturedToday.forEachIndexed { index, entry ->
                    CapturedRow(entry, Modifier.smoothEntrance(index))
                }
            }
        }

        Spacer(Modifier.height(4.dp))
    }
}

/**
 * The course ring: average strength inside, or the tick when the queue is empty.
 *
 * A course up to date repeats in the ring the same tick the flag shows in the
 * strip — confirmation that the badge up there meant this.
 */
@Composable
private fun CourseRing(page: HomePage) {
    val colors = MaterialTheme.colorScheme
    val upToDate = page.summary.inQueue == 0 && page.summary.total > 0
    ProgressRing(
        fraction = if (upToDate) 1f else page.averageStrength / 100f,
        size = 70.dp,
        thickness = 8.dp,
    ) {
        if (upToDate) {
            Icon(AppIcons.Check, null, tint = colors.tertiary, modifier = Modifier.size(26.dp))
        } else {
            // The percentage rises in the same time the arc runs: they are the
            // same measure, and arriving together is what keeps the ring from
            // looking like decoration around a number.
            Text("${animatedCount(page.averageStrength, "averageStrength")}%", style = MaterialTheme.typography.headlineSmall)
        }
    }
}

/** "Next 5 in 19h · nothing to do today" — the card of a course with no queue. */
@Composable
private fun UpNextRow(page: HomePage, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val next = page.summary.nextInMillis
    ScreenCard(
        shape = MaterialTheme.shapes.medium,
        color = colors.surfaceVariant,
        filling = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconDisc(
                icon = if (next == null) AppIcons.Plus else AppIcons.Clock,
                contentDescription = null,
                color = colors.onSurfaceVariant,
                background = colors.surface,
                size = 34.dp,
            )
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(
                    text = when {
                        next == null -> stringResource(R.string.home_nothing_scheduled)
                        page.nextIn24h > 1 ->
                            stringResource(R.string.home_next_many, page.nextIn24h, timeUntil(next))
                        else -> stringResource(R.string.home_next_one, timeUntil(next))
                    },
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = stringResource(if (next == null) R.string.home_start_capturing else R.string.home_nothing_today),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
        }
    }
}

@Composable
private fun CapturedRow(entry: Entry, modifier: Modifier = Modifier) {
    ScreenCard(
        shape = MaterialTheme.shapes.small,
        filling = PaddingValues(horizontal = 15.dp, vertical = 11.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(entryTitle(entry), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text(
                text = entry.card?.translation.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** A day with no capture becomes an invitation, not an empty screen. */
@Composable
private fun CaptureInvite(language: String, onClick: () -> Unit) {
    DashedBox(
        modifier = Modifier.fillMaxWidth(),
        filling = PaddingValues(18.dp),
        onClick = onClick,
    ) {
        Text(
            text = stringResource(R.string.home_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        PrimaryButton(
            text = stringResource(R.string.home_capture_in, language),
            onClick = onClick,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

@Composable
private fun courseDetail(total: Int, mastered: Int, inQueue: Int): String {
    val stock = if (total == 0) {
        stringResource(R.string.home_no_cards_yet)
    } else {
        stringResource(
            R.string.home_stock,
            pluralStringResource(R.plurals.home_stock_cards, total, total),
            mastered,
        )
    }
    val queue = when {
        total == 0 -> stringResource(R.string.home_capture_first)
        inQueue == 0 -> stringResource(R.string.home_nothing_cooled)
        else -> pluralStringResource(R.plurals.home_cooled_today, inQueue, inQueue)
    }
    return "$stock\n$queue"
}

/** The bottom bar plus the gap for the capture button that overhangs it. */
private val BAR_SPACING = 92.dp
