package com.jean.vocabs.shared.domain

import com.jean.vocabs.contracts.TargetType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SelectionTest {
    @Test
    fun `tokenization preserva apostrophes e hyphens inners e remove score external`() {
        val tokens = tokenizeSnippet("Wait—don't second-guess 'this'.")
        assertEquals(listOf("Wait", "don't", "second-guess", "this"), tokens.map { it.text })
    }

    @Test
    fun `uma selection e word e several formam phrase`() {
        val snippet = "He is on the fence today."
        val word = assertNotNull(selectTokens(snippet, 4))
        val phrase = assertNotNull(selectTokens(snippet, 2, 4))
        assertEquals("fence", word.text)
        assertEquals(TargetType.WORD, word.type)
        assertEquals("on the fence", phrase.text)
        assertEquals(TargetType.PHRASE, phrase.type)
        // Os intervalos podem se sobrepor sem perder nenhuma seleção.
        assertTrue(word.start >= phrase.start && word.end <= phrase.end)
    }

    @Test
    fun `answer ignora box e spaces mas preserva accentMark e score`() {
        assertTrue(isAnswerCorrect("  On   The Fence ", "on the fence"))
        assertFalse(isAnswerCorrect("cafe", "café"))
        assertFalse(isAnswerCorrect("dont", "don't"))
    }
}
