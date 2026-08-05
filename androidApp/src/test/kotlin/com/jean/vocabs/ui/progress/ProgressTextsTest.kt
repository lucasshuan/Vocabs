package com.jean.vocabs.ui.progress

import com.jean.vocabs.shared.domain.CourseSummary
import com.jean.vocabs.shared.domain.DailyQuota
import com.jean.vocabs.shared.domain.Event
import com.jean.vocabs.shared.domain.EventType
import com.jean.vocabs.shared.domain.LanguagePair
import com.jean.vocabs.shared.domain.MemoryLevel
import com.jean.vocabs.shared.domain.Steps
import java.time.LocalDate
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * What is left of the progress screens' text logic once the strings moved.
 *
 * The builders themselves are gone from here: they are resource lookups in
 * composition now, and asserting a resolved sentence fails on every copy edit
 * with nothing actually wrong. What stays is the logic that outlived them —
 * branch selection, locale handling, and the one place where copy and a
 * constant have to agree.
 */
class ProgressTextsTest {

    private val PT_BR = Locale.forLanguageTag("pt-BR")

    private val languagePair = LanguagePair(native = "pt-BR", target = "en")

    /**
     * progress_ring_appears_after spells the count out in prose ("appears after
     * four reviews") because Steps.TOTAL is a compile-time constant, not data.
     * That leaves the copy and the constant free to drift apart, so this is what
     * ties them: change the ladder and this fails, pointing at both strings.
     */
    @Test
    fun `the empty-ring copy is written for a five-step ladder`() {
        assertEquals(5, Steps.TOTAL)
    }

    // `whatsLeftText` is now a plurals lookup in composition. Its count-to-form
    // mapping belongs to CLDR and its two keys are covered by MissingTranslation
    // and ImpliedQuantity, so there is nothing left here worth asserting.

    /**
     * Asserts the locale is honoured, not the exact wording: the strings come
     * from the JDK's own data, and pinning "março" here would make a JDK upgrade
     * look like a bug in this app.
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

    // The timeline row now picks a branch here and a resource in composition.
    // These cover the branch choice, which is the part with logic in it.

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
