package com.jean.vocabs.shared.domain

import com.jean.vocabs.contracts.TargetType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SelecaoTest {
    @Test
    fun `tokenizacao preserva apostrofos e hifens internos e remove pontuacao externa`() {
        val tokens = tokenizarTrecho("Wait—don't second-guess 'this'.")
        assertEquals(listOf("Wait", "don't", "second-guess", "this"), tokens.map { it.texto })
    }

    @Test
    fun `uma selecao e palavra e varias formam expressao`() {
        val trecho = "He is on the fence today."
        val palavra = assertNotNull(selecionarTokens(trecho, 4))
        val expressao = assertNotNull(selecionarTokens(trecho, 2, 4))
        assertEquals("fence", palavra.texto)
        assertEquals(TargetType.WORD, palavra.tipo)
        assertEquals("on the fence", expressao.texto)
        assertEquals(TargetType.PHRASE, expressao.tipo)
        // Os intervalos podem se sobrepor sem perder nenhuma seleção.
        assertTrue(palavra.inicio >= expressao.inicio && palavra.fim <= expressao.fim)
    }

    @Test
    fun `resposta ignora caixa e espacos mas preserva acento e pontuacao`() {
        assertTrue(respostaCorreta("  On   The Fence ", "on the fence"))
        assertFalse(respostaCorreta("cafe", "café"))
        assertFalse(respostaCorreta("dont", "don't"))
    }
}
