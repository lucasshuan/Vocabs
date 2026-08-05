package com.jean.vocabs.ui.progress

import android.content.res.Resources
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jean.vocabs.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jean.vocabs.shared.domain.Event
import com.jean.vocabs.shared.domain.EventType
import com.jean.vocabs.shared.domain.MemoryLevel
import com.jean.vocabs.ui.components.AppIcons
import com.jean.vocabs.ui.components.EmptyState
import com.jean.vocabs.ui.components.InnerHeader
import com.jean.vocabs.ui.components.ScreenCard
import com.jean.vocabs.ui.components.WeekDay
import com.jean.vocabs.ui.components.WeekStrip
import com.jean.vocabs.ui.components.levelLabel
import java.time.LocalDate
import java.util.Locale

/**
 * "Day by day".
 *
 * The same week strip as Progress, and below it what actually happened each day.
 * A day with nothing appears spelled out: skipping empty days would make the
 * list compact and dishonest, with Tuesday against Friday.
 */
@Composable
fun DayByDayScreen(
    target: String?,
    onBack: () -> Unit,
    onOpenCard: (Long) -> Unit,
    vm: ProgressViewModel = viewModel(),
) {
    LaunchedEffect(target) { vm.open(target) }
    val state by vm.state.collectAsStateWithLifecycle()
    val today = remember { LocalDate.now() }
    val resources = LocalResources.current
    // The picker can differ from the device, so the locale comes from the
    // configuration Compose is actually rendering with.
    val locale = LocalConfiguration.current.locales[0]
    val weekdays = remember(locale) { weekdayLabels(locale) }
    val days = remember(state.events, state.quota, locale) {
        groupByDay(
            events = state.events,
            today = today,
            todayQuota = resources.getString(
                R.string.daybyday_quota, state.quota.done, state.quota.total
            ),
            title = { day -> resources.dayTitle(day, today, locale) },
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        InnerHeader(stringResource(R.string.daybyday_title), onBack, Modifier.padding(top = 8.dp))

        ScreenCard(filling = PaddingValues(16.dp), modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.progress_month_this_week, monthNameCapitalised(state.month, locale)),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = streakLabel(state.dayStreak),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
            WeekStrip(
                days = state.week.mapIndexed { index, day ->
                    WeekDay(
                        abbreviation = weekdays[index],
                        number = day.date.dayOfMonth,
                        reviews = day.reviews,
                        today = day.today,
                        future = day.future,
                    )
                },
                modifier = Modifier.padding(top = 13.dp),
            )
        }

        if (days.isEmpty()) {
            EmptyState(
                icon = AppIcons.Clock,
                title = stringResource(R.string.daybyday_empty_title),
                detail = stringResource(R.string.daybyday_empty_detail),
                modifier = Modifier.weight(1f),
            )
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(days, key = { it.day }) { group ->
                    DayGroup(
                        group = group,
                        last = group.day == days.last().day,
                        onOpenCard = onOpenCard,
                    )
                }
                item { Spacer(Modifier.navigationBarsPadding().height(110.dp)) }
            }
        }
    }
}

/**
 * One day of the timeline: the dot on the rail and what happened beside it.
 *
 * The rail is what makes this a timeline rather than a list with subheadings. It
 * connects one day to the next including across idle days, which is exactly where
 * it works — the line crossing a "nothing here" shows the hole instead of hiding
 * it.
 *
 * Drawn behind the whole row rather than stacked as a fixed-height child: only in
 * `drawBehind` is it known how far the day's content went, which is how it lands
 * exactly on the next day's dot with one card or with four.
 */
@Composable
private fun DayGroup(group: EventDay, last: Boolean, onOpenCard: (Long) -> Unit) {
    val colors = MaterialTheme.colorScheme
    // Today is plum even when empty: it is the day something can still be done
    // in, not one more hole in the history.
    val point = when {
        group.today -> colors.primary
        group.events.isEmpty() -> colors.outline
        else -> colors.tertiary
    }
    val rail = colors.outlineVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                // The last day has nowhere to continue: the line would stop in
                // mid-air, suggesting a past the screen cannot show.
                if (last) return@drawBehind
                val middle = TRACK_WIDTH.toPx() / 2f
                val beginning = (DOT_TOP + DOT_SIZE + TRACK_GAP).toPx()
                drawLine(
                    color = rail,
                    start = Offset(middle, beginning),
                    end = Offset(middle, size.height),
                    strokeWidth = 2.dp.toPx(),
                )
            },
    ) {
        Box(contentAlignment = Alignment.TopCenter, modifier = Modifier.width(TRACK_WIDTH)) {
            Box(
                Modifier
                    .padding(top = DOT_TOP)
                    .size(DOT_SIZE)
                    .background(point, CircleShape),
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f).padding(start = 11.dp, bottom = 18.dp),
        ) {
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
                Text(group.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                group.quota?.let {
                    Text(text = it, style = MaterialTheme.typography.bodySmall, color = colors.primary)
                }
            }

            if (group.events.isEmpty()) {
                IdleDay()
            } else {
                group.events.forEach { event ->
                    EventCard(event, onClick = { onOpenCard(event.entryId) })
                }
            }
        }
    }
}

/**
 * The empty day, as a box rather than a loose sentence.
 *
 * Beside white cards, text without a box would read as a caption for the day
 * above. The lilac box takes a card's place and says there was room for
 * something — the day is empty, not the list.
 */
@Composable
private fun IdleDay() {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = "Dia parado. Nada aqui.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 13.dp),
        )
    }
}

@Composable
private fun EventCard(event: Event, onClick: () -> Unit) {
    ScreenCard(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        filling = PaddingValues(horizontal = 15.dp, vertical = 13.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(event.target, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text(
                text = eventDescription(event),
                style = MaterialTheme.typography.bodySmall,
                color = eventColor(event.type),
            )
        }
    }
}

/**
 * The outcome's color, now on the text rather than on a dot.
 *
 * The colored dot left each event and became the day's dot on the rail — two
 * levels of dot in one column would fight over the vertical reading the rail
 * exists to give. The word on the right carries the meaning now.
 */
@Composable
private fun eventColor(type: EventType): Color = when (type) {
    EventType.LEVELED_UP, EventType.CORRECT -> MaterialTheme.colorScheme.tertiary
    EventType.INCORRECT -> MaterialTheme.colorScheme.error
    EventType.CAPTURED, EventType.CARD_READY -> MaterialTheme.colorScheme.onSurfaceVariant
}

/** The day's dot and the line descending from it live in this left column. */
private val TRACK_WIDTH = 20.dp

/** Aligns the dot with the first line of the day's title, not the block's top. */
private val DOT_TOP = 5.dp
private val DOT_SIZE = 10.dp
private val TRACK_GAP = 4.dp

/** One day of the timeline, with what happened in it. */
internal data class EventDay(
    val day: Long,
    val title: String,
    val today: Boolean,
    val quota: String?,
    val events: List<Event>,
)

/**
 * Groups by day and fills the holes.
 *
 * Every day from the most recent one with an event through today enters the list,
 * empty ones included. The floor is the oldest day with an event: inventing empty
 * months before the first capture would fill the scroll with nothing.
 */
internal fun groupByDay(
    events: List<Event>,
    today: LocalDate,
    todayQuota: String? = null,
    /** Passed in, like [todayQuota], so grouping stays free of resources. */
    title: (day: Long) -> String,
): List<EventDay> {
    if (events.isEmpty()) return emptyList()
    val byDay = events.groupBy { it.day }
    val todayDay = today.toEpochDay() + JULIAN_DAY_OF_EPOCH_UI
    val earliest = byDay.keys.min()
    val latest = maxOf(byDay.keys.max(), todayDay)

    return (latest downTo earliest).map { day ->
        EventDay(
            day = day,
            title = title(day),
            today = day == todayDay,
            // The quota is a count over the queue as it is now, and Tuesday's
            // queue is gone — only today can say how much is left.
            quota = todayQuota.takeIf { day == todayDay },
            events = byDay[day].orEmpty(),
        )
    }
}

private const val JULIAN_DAY_OF_EPOCH_UI = 2_440_588L

private fun Resources.dayTitle(day: Long, today: LocalDate, locale: Locale): String {
    val date = LocalDate.ofEpochDay(day - JULIAN_DAY_OF_EPOCH_UI)
    val name = getString(R.string.daybyday_date, monthName(date, locale), date.dayOfMonth)
    val todayDay = today.toEpochDay() + JULIAN_DAY_OF_EPOCH_UI
    return when (day) {
        todayDay -> getString(R.string.daybyday_date_today, name)
        todayDay - 1 -> getString(R.string.daybyday_date_yesterday, name)
        else -> name
    }
}

/**
 * What the row says on the right: "captured", "2nd review correct", "mastered".
 *
 * The ordinal comes from the review number stored on the event; without it the
 * row would say only "review correct" and the timeline would lose the sense of
 * advancing it exists to show.
 */
internal sealed interface EventDescription {
    data object Captured : EventDescription
    data object CardReady : EventDescription
    /** [number] is null when the event predates review numbering or carries junk. */
    data class Review(val number: Int?, val right: Boolean) : EventDescription
    data class LeveledUp(val level: MemoryLevel) : EventDescription
}

/** Kept free of resources so the branch choice stays testable without a device. */
internal fun describeEvent(event: Event): EventDescription = when (event.type) {
    EventType.CAPTURED -> EventDescription.Captured
    EventType.CARD_READY -> EventDescription.CardReady
    EventType.CORRECT -> EventDescription.Review(event.detail?.toIntOrNull(), right = true)
    EventType.INCORRECT -> EventDescription.Review(event.detail?.toIntOrNull(), right = false)
    EventType.LEVELED_UP -> EventDescription.LeveledUp(levelOf(event.detail))
}

@Composable
internal fun eventDescription(event: Event): String = when (val it = describeEvent(event)) {
    EventDescription.Captured -> stringResource(R.string.event_captured)
    EventDescription.CardReady -> stringResource(R.string.event_card_ready)
    is EventDescription.Review -> when {
        it.number == null && it.right -> stringResource(R.string.event_review_correct_unnumbered)
        it.number == null -> stringResource(R.string.event_review_incorrect_unnumbered)
        it.right -> stringResource(R.string.event_review_correct, it.number)
        else -> stringResource(R.string.event_review_incorrect, it.number)
    }
    is EventDescription.LeveledUp ->
        stringResource(R.string.event_leveled_up, levelLabel(it.level))
}

private fun levelOf(detail: String?): MemoryLevel =
    MemoryLevel.entries.firstOrNull { it.name == detail } ?: MemoryLevel.LEARNING
