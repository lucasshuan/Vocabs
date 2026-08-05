package com.jean.vocabs.ui.progresso

import com.jean.vocabs.shared.domain.Degraus
import com.jean.vocabs.shared.domain.Evento
import com.jean.vocabs.shared.domain.MemoryLevel
import com.jean.vocabs.shared.domain.ParIdiomas
import com.jean.vocabs.shared.domain.QuotaDoDia
import com.jean.vocabs.shared.domain.ResumoCurso
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

    private val par = ParIdiomas(nativo = "pt-BR", alvo = "en")

    @Test
    fun `one day streak takes no plural`() {
        assertEquals("1 dia seguido", rotuloDeSequencia(1))
        assertEquals("0 dias seguidos", rotuloDeSequencia(0))
        assertEquals("5 dias seguidos", rotuloDeSequencia(5))
    }

    @Test
    fun `a day with nothing due shows a dash, not zero of zero`() {
        assertEquals("—", textoDaQuota(QuotaDoDia(feita = 0, naFila = 0)))
        assertEquals("6 de 10", textoDaQuota(QuotaDoDia(feita = 6, naFila = 4)))
        assertEquals("3 de 3", textoDaQuota(QuotaDoDia(feita = 3, naFila = 0)))
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
        assertEquals("nenhuma palavra ainda", resumoDoCurso(ResumoCurso(par, total = 0, dominadas = 0)))
        assertEquals("9 de 24 já são suas", resumoDoCurso(ResumoCurso(par, total = 24, dominadas = 9)))
    }

    /** Guards the copy against [Degraus.TOTAL] drifting; the ladder size is baked into the sentence. */
    @Test
    fun `the empty-ring sentence follows the ladder size`() {
        assertEquals(5, Degraus.TOTAL)
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
        assertEquals("revisão certa", descricaoDoEvento(evento(EventType.CORRECT, detalhe = null)))
        assertEquals("revisão errada", descricaoDoEvento(evento(EventType.INCORRECT, detalhe = "not-a-number")))
    }

    @Test
    fun `an event with a number gets the feminine ordinal`() {
        assertEquals("2ª revisão certa", descricaoDoEvento(evento(EventType.CORRECT, detalhe = "2")))
        assertEquals("3ª revisão errada", descricaoDoEvento(evento(EventType.INCORRECT, detalhe = "3")))
    }

    @Test
    fun `levelling up repeats the level label, and an unknown one falls back`() {
        assertEquals("virou dominada", descricaoDoEvento(evento(EventType.LEVELED_UP, detalhe = "MASTERED")))
        assertEquals("virou aprendendo", descricaoDoEvento(evento(EventType.LEVELED_UP, detalhe = "SEPIA")))
    }

    @Test
    fun `capture and card-ready have no variants`() {
        assertEquals("capturada", descricaoDoEvento(evento(EventType.CAPTURED, detalhe = null)))
        assertEquals("ficha pronta", descricaoDoEvento(evento(EventType.CARD_READY, detalhe = null)))
    }

    private fun evento(tipo: EventType, detalhe: String?) = Evento(
        id = 1L,
        entradaId = 1L,
        dia = 2_460_000L,
        instante = 0L,
        tipo = tipo,
        alvo = "haywire",
        par = par,
        detalhe = detalhe,
    )
}
