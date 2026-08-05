package com.jean.vocabs.shared.domain

import com.jean.vocabs.contracts.TargetType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class RetencaoEDuplicataTest {
    @Test
    fun `erro e acerto atualizam retencao e placar`() {
        val inicial = Retencao.inicial(1_000)
        val erro = inicial.apos(false, 2_000)
        assertEquals(0.0, erro.pontos)
        assertEquals(1, erro.erros)
        val acerto = erro.apos(true, 3_000)
        assertEquals(100.0, acerto.pontos)
        assertEquals(1, acerto.acertos)
        assertEquals(2, acerto.revisoes)
    }

    @Test
    fun `duplicata normaliza caixa e espacos sem bloquear`() {
        val pronta = entrada(1, "on the fence", EntryStatus.READY)
        val pendente = entrada(2, "ON  THE FENCE", EntryStatus.PENDING)
        assertSame(pronta, duplicataDeAlvo("  On the fence ", listOf(pendente, pronta)))
        assertNull(duplicataDeAlvo("fence", listOf(pronta)))
    }

    private fun entrada(id: Long, alvo: String, status: EntryStatus) = Entrada(
        id = id,
        capturaId = id,
        trecho = alvo,
        alvo = alvo,
        inicio = 0,
        fim = alvo.length,
        tipo = TargetType.PHRASE,
        origem = null,
        criadoEm = id,
        status = status,
        formato = CaptureFormat.TEXT,
        midiaCaminho = null,
        ficha = null,
        retencao = null,
        errorCode = null,
        errorDetail = null,
    )
}
