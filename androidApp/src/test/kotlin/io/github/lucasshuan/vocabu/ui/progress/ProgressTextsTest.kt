package io.github.lucasshuan.vocabu.ui.progress

import io.github.lucasshuan.vocabu.shared.domain.CourseSummary
import io.github.lucasshuan.vocabu.shared.domain.DailyQuota
import io.github.lucasshuan.vocabu.shared.domain.Event
import io.github.lucasshuan.vocabu.shared.domain.EventType
import io.github.lucasshuan.vocabu.shared.domain.LanguagePair
import io.github.lucasshuan.vocabu.shared.domain.MemoryLevel
import io.github.lucasshuan.vocabu.shared.domain.Steps
import java.time.LocalDate
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * The builders are resource lookups in composition now, and asserting a resolved
 * sentence fails on every copy edit with nothing wrong. What stays is branch
 * selection, locale handling, and the one place copy and a constant must agree.
 */
class ProgressTextsTest {

    private val PT_BR = Locale.forLanguageTag("pt-BR")

    private val languagePair = LanguagePair(native = "pt-BR", target = "en")

    /**
     * `progress_ring_appears_after` spells the count out in prose, so nothing
     * ties it to `Steps.TOTAL` but this: change the ladder and it fails.
     */
    @Test
    fun `the empty-ring copy is written for a five-step ladder`() {
        assertEquals(5, Steps.TOTAL)
    }

    // `whatsLeftText` is a plurals lookup: its count-to-form mapping is CLDR's,
    // and its keys are covered by MissingTranslation and ImpliedQuantity.

    /**
     * The locale, not the wording: the names come from the JDK's own data, and
     * pinning "março" would make a JDK upgrade look like a bug here.
     */
    @Test
    fun `month names follow the locale they are given`() {
        val march = LocalDate.of(2026, 3, 1)
        assertEquals("March", monthName(march, Locale.ENGLISH))
        assertNotEquals(monthName(march, Locale.ENGLISH), monthName(march, PT_BR))
    }

    @Test
    fun `the capitalised form differs only in the first character`() {
        val march = LocalDate.of(2026, 3, 1)
        assertEquals(
            monthName(march, PT_BR).replaceFirstChar { it.uppercase(PT_BR) },
            monthNameCapitalised(march, PT_BR),
        )
    }

    /** `weekOf` builds Monday-first, so the labels have to start there too. */
    @Test
    fun `the week has seven labels, starts on Monday, and follows the locale`() {
        val english = weekdayLabels(Locale.ENGLISH)
        assertEquals(7, english.size)
        assertEquals("Mon", english.first())
        assertEquals(7, weekdayLabels(PT_BR).size)
        assertNotEquals(english, weekdayLabels(PT_BR))
    }

    // The timeline row picks a branch here and a resource in composition; the
    // branch is the part with logic in it.

    @Test
    fun `an unparsable review number is treated as absent`() {
        assertEquals(
            EventDescription.Review(number = null, right = true),
            describeEvent(event(EventType.CORRECT, detail = null)),
        )
        assertEquals(
            EventDescription.Review(number = null, right = false),
            describeEvent(event(EventType.INCORRECT, detail = "not-a-number")),
        )
    }

    @Test
    fun `a review number is carried through as a number`() {
        assertEquals(
            EventDescription.Review(number = 2, right = true),
            describeEvent(event(EventType.CORRECT, detail = "2")),
        )
        assertEquals(
            EventDescription.Review(number = 3, right = false),
            describeEvent(event(EventType.INCORRECT, detail = "3")),
        )
    }

    @Test
    fun `an unknown level falls back rather than dropping the row`() {
        assertEquals(
            EventDescription.LeveledUp(MemoryLevel.MASTERED),
            describeEvent(event(EventType.LEVELED_UP, detail = "MASTERED")),
        )
        assertEquals(
            EventDescription.LeveledUp(MemoryLevel.LEARNING),
            describeEvent(event(EventType.LEVELED_UP, detail = "SEPIA")),
        )
    }

    @Test
    fun `capture and card-ready have no variants`() {
        assertEquals(EventDescription.Captured, describeEvent(event(EventType.CAPTURED, detail = null)))
        assertEquals(EventDescription.CardReady, describeEvent(event(EventType.CARD_READY, detail = null)))
    }

    private fun event(type: EventType, detail: String?) = Event(
        id = 1L,
        entryId = 1L,
        day = 2_460_000L,
        instant = 0L,
        type = type,
        target = "haywire",
        languagePair = languagePair,
        detail = detail,
    )
}
