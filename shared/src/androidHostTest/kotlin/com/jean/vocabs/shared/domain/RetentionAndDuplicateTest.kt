package com.jean.vocabs.shared.domain

import com.jean.vocabs.contracts.TargetType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class RetentionAndDuplicateTest {
    @Test
    fun `a miss and a hit both update retention and the tally`() {
        val initial = Retention.initial(1_000)
        val error = initial.after(false, 2_000)
        assertEquals(0.0, error.points)
        assertEquals(1, error.misses)
        val hit = error.after(true, 3_000)
        assertEquals(100.0, hit.points)
        assertEquals(1, hit.hits)
        assertEquals(2, hit.reviews)
    }

    @Test
    fun `duplicate detection folds case and spaces without blocking`() {
        val isReady = entry(1, "on the fence", EntryStatus.READY)
        val pending = entry(2, "ON  THE FENCE", EntryStatus.PENDING)
        assertSame(isReady, duplicateOfTarget("  On the fence ", listOf(pending, isReady)))
        assertNull(duplicateOfTarget("fence", listOf(isReady)))
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
