package com.jean.vocabs.shared.domain

import com.jean.vocabs.contracts.TargetType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class RetencaoEDuplicataTest {
    @Test
    fun `error e hit atualizam retention e placar`() {
        val inicial = Retention.inicial(1_000)
        val error = inicial.apos(false, 2_000)
        assertEquals(0.0, error.points)
        assertEquals(1, error.errors)
        val hit = error.apos(true, 3_000)
        assertEquals(100.0, hit.points)
        assertEquals(1, hit.hits)
        assertEquals(2, hit.reviews)
    }

    @Test
    fun `duplicata normaliza caixa e espacos sem bloquear`() {
        val pronta = entry(1, "on the fence", EntryStatus.READY)
        val pendente = entry(2, "ON  THE FENCE", EntryStatus.PENDING)
        assertSame(pronta, duplicateOfTarget("  On the fence ", listOf(pendente, pronta)))
        assertNull(duplicateOfTarget("fence", listOf(pronta)))
    }

    private fun entry(id: Long, target: String, status: EntryStatus) = Entry(
        id = id,
        captureId = id,
        snippet = target,
        target = target,
        start = 0,
        end = target.length,
        type = TargetType.PHRASE,
        source = null,
        createdAt = id,
        status = status,
        format = CaptureFormat.TEXT,
        mediaPath = null,
        card = null,
        retention = null,
        errorCode = null,
        errorDetail = null,
    )
}
