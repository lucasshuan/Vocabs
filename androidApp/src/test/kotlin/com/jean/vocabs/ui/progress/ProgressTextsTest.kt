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
 * The progress screens' text logic, in two halves.
 *
 * The builders still holding Portuguese are pinned as they stand — plural and
 * gender are hand-rolled in them and nothing else covers it, so extracting them
 * would otherwise happen with no before/after signal. Those assertions are a
 * snapshot, not a statement of intent, and go when the strings do.
 *
 * The rest have already moved: they assert which branch or locale was chosen,
 * never the resolved sentence. Asserting resolved text fails on every copy edit
 * with nothing actually wrong.
 */
class ProgressTextsTest {

    private val PT_BR = Locale.forLanguageTag("pt-BR")

    private val languagePair = LanguagePair(native = "pt-BR", target = "en")

    @Test
    fun `one day streak takes no plural`() {
        assertEquals("1 dia seguido", streakLabel(1))
        assertEquals("0 dias seguidos", streakLabel(0))
        assertEquals("5 dias seguidos", streakLabel(5))
    }

    @Test
    fun `a day with nothing due shows a dash, not zero of zero`() {
        assertEquals("—", quotaText(DailyQuota(done = 0, inQueue = 0)))
        assertEquals("6 de 10", quotaText(DailyQuota(done = 6, inQueue = 4)))
        assertEquals("3 de 3", quotaText(DailyQuota(done = 3, inQueue = 0)))
    }

    @Test
    fun `stock spells the number out up to ten, then digits return`() {
        assertEquals("Nenhuma palavra é sua ainda", stockTitle(0))
        assertEquals("Uma palavra já é sua", stockTitle(1))
        assertEquals("Duas palavras já são suas", stockTitle(2))
        assertEquals("Dez palavras já são suas", stockTitle(10))
        assertEquals("11 palavras já são suas", stockTitle(11))
    }

    @Test
    fun `course summary invents no number when there are no words`() {
        assertEquals("nenhuma palavra ainda", courseSummaryText(CourseSummary(languagePair, total = 0, mastered = 0)))
        assertEquals("9 de 24 já são suas", courseSummaryText(CourseSummary(languagePair, total = 24, mastered = 9)))
    }

    /** Guards the copy against [Steps.TOTAL] drifting; the ladder size is baked into the sentence. */
    @Test
    fun `the empty-ring sentence follows the ladder size`() {
        assertEquals(5, Steps.TOTAL)
        assertEquals("aparece depois de quatro revisões", whenItAppearsText())
    }

    @Test
    fun `close to levelling up agrees with the count`() {
        assertEquals("Nenhuma está perto de virar.", closeToLevelingText(0))
        assertEquals("1 está perto de virar.", closeToLevelingText(1))
        assertEquals("3 estão perto de virar.", closeToLevelingText(3))
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
