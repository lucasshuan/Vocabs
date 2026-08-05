package com.jean.vocabs.shared.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class DegrausTest {

    private val agora = 1_000_000L

    @Test
    fun `cada acerto sobe exatamente um degrau, ate o quinto`() {
        var retencao = Retencao.inicial(agora)
        assertEquals(1, Degraus.de(retencao))

        val subida = (1..6).map {
            retencao = retencao.apos(acertou = true, agora = agora)
            Degraus.de(retencao)
        }
        assertEquals(listOf(2, 3, 4, 5, 5, 5), subida)
    }

    @Test
    fun `os nomes dos degraus batem com o handoff`() {
        assertEquals(MemoryLevel.LEARNING, Degraus.nivel(1))
        assertEquals(MemoryLevel.LEARNING, Degraus.nivel(2))
        assertEquals(MemoryLevel.LEARNING, Degraus.nivel(3))
        assertEquals(MemoryLevel.FAMILIAR, Degraus.nivel(4))
        assertEquals(MemoryLevel.MASTERED, Degraus.nivel(5))

        // "haywire · degrau 3 de 5 · 1 acerto para familiar"
        assertEquals(1, Degraus.acertosParaSubirDeNivel(3))
        // "on the fence · degrau 2 de 5 · 2 acertos para familiar"
        assertEquals(2, Degraus.acertosParaSubirDeNivel(2))
        // "verdant · degrau 4 de 5 · 1 acerto para dominada"
        assertEquals(1, Degraus.acertosParaSubirDeNivel(4))
        assertEquals(0, Degraus.acertosParaSubirDeNivel(5))
    }

    @Test
    fun `errar derruba a escada, e o degrau nao cai com o tempo`() {
        var retencao = Retencao.inicial(agora)
        repeat(3) { retencao = retencao.apos(acertou = true, agora = agora) }
        assertEquals(4, Degraus.de(retencao))

        // Um mês parado zera a força de memória, mas o degrau é o que já foi
        // feito — ele não anda para trás sozinho.
        val umMes = agora + 30L * 86_400_000L
        assertEquals(0.0, retencao.pontosEm(umMes))
        assertEquals(4, Degraus.de(retencao))

        // Um erro desfaz 2,7 acertos (MULTIPLICADOR_ERRO = 3 contra DIVISOR_ACERTO
        // = 1,5), então quem estava no quarto degrau volta para o primeiro. A
        // escada herda o rigor do modelo de retenção em vez de ter o seu próprio.
        retencao = retencao.apos(acertou = false, agora = umMes)
        assertEquals(1, Degraus.de(retencao))
    }

    @Test
    fun `melhor sequencia acha a maior corrida de dias seguidos`() {
        // Em ordem decrescente e sem repetição, como sai do banco.
        assertEquals(0, melhorSequenciaDe(emptyList()))
        assertEquals(1, melhorSequenciaDe(listOf(10L)))
        assertEquals(3, melhorSequenciaDe(listOf(20L, 19L, 18L, 15L, 13L, 12L)))
        assertEquals(4, melhorSequenciaDe(listOf(30L, 20L, 19L, 18L, 17L)))
    }

    @Test
    fun `quota vazia nao divide por zero e ja nasce batida`() {
        val vazia = QuotaDoDia(feita = 0, naFila = 0)
        assertEquals(0, vazia.total)
        assertEquals(1f, vazia.fracao)
        assertEquals(true, vazia.batida)
    }
}
