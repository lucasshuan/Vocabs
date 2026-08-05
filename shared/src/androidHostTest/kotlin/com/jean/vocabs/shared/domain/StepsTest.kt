package com.jean.vocabs.shared.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class StepsTest {

    private val now = 1_000_000L

    @Test
    fun `cada hit sobe exactly um step, ate o fifth`() {
        var retention = Retention.initial(now)
        assertEquals(1, Steps.of(retention))

        val rise = (1..6).map {
            retention = retention.after(correct = true, now = now)
            Steps.of(retention)
        }
        assertEquals(listOf(2, 3, 4, 5, 5, 5), rise)
    }

    @Test
    fun `os names dos steps batem com o handoff`() {
        assertEquals(MemoryLevel.LEARNING, Steps.level(1))
        assertEquals(MemoryLevel.LEARNING, Steps.level(2))
        assertEquals(MemoryLevel.LEARNING, Steps.level(3))
        assertEquals(MemoryLevel.FAMILIAR, Steps.level(4))
        assertEquals(MemoryLevel.MASTERED, Steps.level(5))

        // "haywire · degrau 3 de 5 · 1 acerto para familiar"
        assertEquals(1, Steps.hitsToLevelUp(3))
        // "on the fence · degrau 2 de 5 · 2 acertos para familiar"
        assertEquals(2, Steps.hitsToLevelUp(2))
        // "verdant · degrau 4 de 5 · 1 acerto para dominada"
        assertEquals(1, Steps.hitsToLevelUp(4))
        assertEquals(0, Steps.hitsToLevelUp(5))
    }

    @Test
    fun `miss derruba a ladder, e o step nao cai com o time`() {
        var retention = Retention.initial(now)
        repeat(3) { retention = retention.after(correct = true, now = now) }
        assertEquals(4, Steps.of(retention))

        // Um mês parado zera a força de memória, mas o degrau é o que já foi
        // feito — ele não anda para trás sozinho.
        val oneMonth = now + 30L * 86_400_000L
        assertEquals(0.0, retention.pointsAt(oneMonth))
        assertEquals(4, Steps.of(retention))

        // Um erro desfaz 2,7 acertos (MULTIPLICADOR_ERRO = 3 contra DIVISOR_ACERTO
        // = 1,5), então quem estava no quarto degrau volta para o primeiro. A
        // escada herda o rigor do modelo de retenção em vez de ter o seu próprio.
        retention = retention.after(correct = false, now = oneMonth)
        assertEquals(1, Steps.of(retention))
    }

    @Test
    fun `best streak acha a largest run de days consecutive`() {
        // Em ordem decrescente e sem repetição, como sai do banco.
        assertEquals(0, bestStreakOf(emptyList()))
        assertEquals(1, bestStreakOf(listOf(10L)))
        assertEquals(3, bestStreakOf(listOf(20L, 19L, 18L, 15L, 13L, 12L)))
        assertEquals(4, bestStreakOf(listOf(30L, 20L, 19L, 18L, 17L)))
    }

    @Test
    fun `quota empty nao divide por zero e ja nasce met`() {
        val empty = DailyQuota(done = 0, inQueue = 0)
        assertEquals(0, empty.total)
        assertEquals(1f, empty.fraction)
        assertEquals(true, empty.met)
    }
}
