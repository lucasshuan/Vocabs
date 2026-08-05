package com.jean.vocabs.ui.progress

import com.jean.vocabs.shared.domain.CourseSummary
import com.jean.vocabs.shared.domain.DailyQuota
import com.jean.vocabs.shared.domain.Event
import com.jean.vocabs.shared.domain.EventType
import com.jean.vocabs.shared.domain.LanguagePair
import com.jean.vocabs.shared.domain.MemoryLevel
import com.jean.vocabs.shared.domain.Steps
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Snapshot of the Portuguese text these builders produce today.
 *
 * These pin what the builders produce today; they are not a statement of what
 * they should produce. Plural, gender and ordinal are hand-rolled here and
 * nothing covered them, so the move to resources would otherwise happen with no
 * before/after signal.
 *
 * Expect to replace them once the strings are resources: assertions on resolved
 * text get noisy, since every copy edit fails a test with nothing actually
 * wrong. What replaces them is open.
 */
class ProgressTextsTest {

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

    @Test
    fun `months come from the hand-written table`() {
        assertEquals("Janeiro", monthName(LocalDate.of(2026, 1, 15)))
        assertEquals("Março", monthName(LocalDate.of(2026, 3, 1)))
        assertEquals("Dezembro", monthName(LocalDate.of(2026, 12, 31)))
    }

    /** `semanaDe` depends on this ordering. */
    @Test
    fun `the week has seven labels and starts on Monday`() {
        assertEquals(7, WEEKDAY_LABELS.size)
        assertEquals("seg", WEEKDAY_LABELS.first())
        assertEquals("dom", WEEKDAY_LABELS.last())
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
