package io.github.lucasshuan.vocabu.shared.domain

import io.github.lucasshuan.vocabu.contracts.TargetType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SelectionTest {
    @Test
    fun `tokenizing keeps inner apostrophes and hyphens and drops outer punctuation`() {
        val tokens = tokenizeSnippet("Wait—don't second-guess 'this'.")
        assertEquals(listOf("Wait", "don't", "second-guess", "this"), tokens.map { it.text })
    }

    @Test
    fun `one token is a WORD and several are a PHRASE`() {
        val snippet = "He is on the fence today."
        val word = assertNotNull(selectTokens(snippet, 4))
        val phrase = assertNotNull(selectTokens(snippet, 2, 4))
        assertEquals("fence", word.text)
        assertEquals(TargetType.WORD, word.type)
        assertEquals("on the fence", phrase.text)
        assertEquals(TargetType.PHRASE, phrase.type)
        // Ranges may overlap without losing any selection.
        assertTrue(word.start >= phrase.start && word.end <= phrase.end)
    }

    @Test
    fun `an answer ignores case and spacing but keeps accents and punctuation`() {
        assertTrue(isAnswerCorrect("  On   The Fence ", "on the fence"))
        assertFalse(isAnswerCorrect("cafe", "café"))
        assertFalse(isAnswerCorrect("dont", "don't"))
    }
}
