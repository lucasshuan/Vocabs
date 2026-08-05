package com.jean.vocabs.ui.progresso

import com.jean.vocabs.shared.domain.Steps
import com.jean.vocabs.shared.domain.Event
import com.jean.vocabs.shared.domain.MemoryLevel
import com.jean.vocabs.shared.domain.LanguagePair
import com.jean.vocabs.shared.domain.DailyQuota
import com.jean.vocabs.shared.domain.CourseSummary
import com.jean.vocabs.shared.domain.EventType
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
class TextosDeProgressoTest {

    private val languagePair = LanguagePair(native = "pt-BR", target = "en")

    @Test
    fun `one day streak takes no plural`() {
        assertEquals("1 dia seguido", rotuloDeSequencia(1))
        assertEquals("0 dias seguidos", rotuloDeSequencia(0))
        assertEquals("5 dias seguidos", rotuloDeSequencia(5))
    }

    @Test
    fun `a day with nothing due shows a dash, not zero of zero`() {
        assertEquals("—", textoDaQuota(DailyQuota(done = 0, inQueue = 0)))
        assertEquals("6 de 10", textoDaQuota(DailyQuota(done = 6, inQueue = 4)))
        assertEquals("3 de 3", textoDaQuota(DailyQuota(done = 3, inQueue = 0)))
    }

    @Test
    fun `stock spells the number out up to ten, then digits return`() {
        assertEquals("Nenhuma palavra é sua ainda", tituloDoEstoque(0))
        assertEquals("Uma palavra já é sua", tituloDoEstoque(1))
        assertEquals("Duas palavras já são suas", tituloDoEstoque(2))
        assertEquals("Dez palavras já são suas", tituloDoEstoque(10))
        assertEquals("11 palavras já são suas", tituloDoEstoque(11))
    }

    @Test
    fun `course summary invents no number when there are no words`() {
        assertEquals("nenhuma palavra ainda", resumoDoCurso(CourseSummary(languagePair, total = 0, mastered = 0)))
        assertEquals("9 de 24 já são suas", resumoDoCurso(CourseSummary(languagePair, total = 24, mastered = 9)))
    }

    /** Guards the copy against [Steps.TOTAL] drifting; the ladder size is baked into the sentence. */
    @Test
    fun `the empty-ring sentence follows the ladder size`() {
        assertEquals(5, Steps.TOTAL)
        assertEquals("aparece depois de quatro revisões", textoDeQuandoAparece())
    }

    @Test
    fun `close to levelling up agrees with the count`() {
        assertEquals("Nenhuma está perto de virar.", textoDoQuePertoDeVirar(0))
        assertEquals("1 está perto de virar.", textoDoQuePertoDeVirar(1))
        assertEquals("3 estão perto de virar.", textoDoQuePertoDeVirar(3))
    }

    @Test
    fun `what's left names the next level`() {
        assertEquals("1 acerto para familiar", textoDoQueFalta(1, MemoryLevel.FAMILIAR))
        assertEquals("2 acertos para dominada", textoDoQueFalta(2, MemoryLevel.MASTERED))
        assertEquals("0 acertos para aprendendo", textoDoQueFalta(0, MemoryLevel.LEARNING))
    }

    @Test
    fun `months come from the hand-written table`() {
        assertEquals("Janeiro", nomeDoMes(LocalDate.of(2026, 1, 15)))
        assertEquals("Março", nomeDoMes(LocalDate.of(2026, 3, 1)))
        assertEquals("Dezembro", nomeDoMes(LocalDate.of(2026, 12, 31)))
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
        assertEquals("revisão certa", descricaoDoEvento(evento(EventType.CORRECT, detail = null)))
        assertEquals("revisão errada", descricaoDoEvento(evento(EventType.INCORRECT, detail = "not-a-number")))
    }

    @Test
    fun `an event with a number gets the feminine ordinal`() {
        assertEquals("2ª revisão certa", descricaoDoEvento(evento(EventType.CORRECT, detail = "2")))
        assertEquals("3ª revisão errada", descricaoDoEvento(evento(EventType.INCORRECT, detail = "3")))
    }

    @Test
    fun `levelling up repeats the level label, and an unknown one falls back`() {
        assertEquals("virou dominada", descricaoDoEvento(evento(EventType.LEVELED_UP, detail = "MASTERED")))
        assertEquals("virou aprendendo", descricaoDoEvento(evento(EventType.LEVELED_UP, detail = "SEPIA")))
    }

    @Test
    fun `capture and card-ready have no variants`() {
        assertEquals("capturada", descricaoDoEvento(evento(EventType.CAPTURED, detail = null)))
        assertEquals("ficha pronta", descricaoDoEvento(evento(EventType.CARD_READY, detail = null)))
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
