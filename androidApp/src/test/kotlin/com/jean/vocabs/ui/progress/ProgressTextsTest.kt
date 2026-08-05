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

    @Test
    fun `what's left names the next level`() {
        assertEquals("1 acerto para familiar", whatsLeftText(1, MemoryLevel.FAMILIAR))
        assertEquals("2 acertos para dominada", whatsLeftText(2, MemoryLevel.MASTERED))
        assertEquals("0 acertos para aprendendo", whatsLeftText(0, MemoryLevel.LEARNING))
    }

    @Test
    fun `months come from the hand-written table`() {
        assertEquals("Janeiro", monthName(LocalDate.of(2026, 1, 15)))
        assertEquals("Março", monthName(LocalDate.of(2026, 3, 1)))
        assertEquals("Dezembro", monthName(LocalDate.of(2026, 12, 31)))
    }

    /** `semanaDe` depends on this ordering. */
    @Test
    fun `the week has seven labels and starts on Monday`() {
        assertEquals(7, SIGLAS_DA_SEMANA.size)
        assertEquals("seg", SIGLAS_DA_SEMANA.first())
        assertEquals("dom", SIGLAS_DA_SEMANA.last())
    }

    @Test
    fun `an event with no review number loses the ordinal but keeps the outcome`() {
        assertEquals("revisão certa", eventDescription(evento(EventType.CORRECT, detail = null)))
        assertEquals("revisão errada", eventDescription(evento(EventType.INCORRECT, detail = "not-a-number")))
    }

    @Test
    fun `an event with a number gets the feminine ordinal`() {
        assertEquals("2ª revisão certa", eventDescription(evento(EventType.CORRECT, detail = "2")))
        assertEquals("3ª revisão errada", eventDescription(evento(EventType.INCORRECT, detail = "3")))
    }

    @Test
    fun `levelling up repeats the level label, and an unknown one falls back`() {
        assertEquals("virou dominada", eventDescription(evento(EventType.LEVELED_UP, detail = "MASTERED")))
        assertEquals("virou aprendendo", eventDescription(evento(EventType.LEVELED_UP, detail = "SEPIA")))
    }

    @Test
    fun `capture and card-ready have no variants`() {
        assertEquals("capturada", eventDescription(evento(EventType.CAPTURED, detail = null)))
        assertEquals("ficha pronta", eventDescription(evento(EventType.CARD_READY, detail = null)))
    }

    private fun evento(type: EventType, detail: String?) = Event(
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
