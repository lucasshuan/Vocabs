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
        val tokens = tokenizeSnippet("Wait—don't second-guess 'this'.")
        assertEquals(listOf("Wait", "don't", "second-guess", "this"), tokens.map { it.text })
    }

    @Test
    fun `uma selecao e word e varias formam expressao`() {
        val snippet = "He is on the fence today."
        val word = assertNotNull(selectTokens(snippet, 4))
        val expressao = assertNotNull(selectTokens(snippet, 2, 4))
        assertEquals("fence", word.text)
        assertEquals(TargetType.WORD, word.type)
        assertEquals("on the fence", expressao.text)
        assertEquals(TargetType.PHRASE, expressao.type)
        // Os intervalos podem se sobrepor sem perder nenhuma seleção.
        assertTrue(word.start >= expressao.start && word.end <= expressao.end)
    }

    @Test
    fun `answer ignora caixa e espacos mas preserva acento e pontuacao`() {
        assertTrue(isAnswerCorrect("  On   The Fence ", "on the fence"))
        assertFalse(isAnswerCorrect("cafe", "café"))
        assertFalse(isAnswerCorrect("dont", "don't"))
    }
}
