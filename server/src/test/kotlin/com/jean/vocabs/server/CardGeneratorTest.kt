package com.jean.vocabs.server

import com.jean.vocabs.contracts.CardResponse
import com.jean.vocabs.contracts.GenerateCardRequest
import com.jean.vocabs.contracts.TargetType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.elementNames

class GeradorDeFichaTest {
    @Test
    fun `servidor injeta type local e normaliza de tres a seis relacionados`() {
        val request = GenerateCardRequest(
            snippet = "He is on the fence",
            target = "on the fence",
            type = TargetType.PHRASE,
            nativeLanguage = "pt-BR",
            targetLanguage = "en",
        )
        val model = CardResponse(
            type = TargetType.WORD,
            translation = "indeciso",
            definitions = listOf("Sem tomar uma decisão"),
            example = "She remains on the fence.",
            pronunciation = "ɒn ðə fens",
            related = listOf(" undecided ", "hesitant", "uncertain", "hesitant", "wary", "doubtful", "unsure", "extra"),
        )

        val final = applyLocalDecisions(request, model)
        assertEquals(TargetType.PHRASE, final.type)
        assertEquals(listOf("undecided", "hesitant", "uncertain", "wary", "doubtful", "unsure"), final.related)
    }

    @Test
    fun `a notacao de pronuncia segue o language target, com IPA como padrao`() {
        assertEquals("IPA, without slashes", LanguagePairSpec.de("pt-BR", "en")!!.target.pronunciationNotation)
        assertEquals("Hanyu Pinyin with tone marks", LanguagePairSpec.de("pt-BR", "zh")!!.target.pronunciationNotation)
        assertEquals("Revised Romanization of Korean", LanguagePairSpec.de("pt-BR", "ko")!!.target.pronunciationNotation)
        // Language sem entrada própria cai no IPA, que serve à maioria.
        assertEquals("IPA, without slashes", LanguagePairSpec.de("pt-BR", "sv")!!.target.pronunciationNotation)
    }

    @Test
    fun `languagePair desconhecido nao vira o languagePair padrao`() {
        // Refusing is the point: falling back would return an English card for
        // a German word, and the person would only find out by reading it.
        assertNull(LanguagePairSpec.de("pt-BR", "klingon"))
        assertNull(LanguagePairSpec.de("elfico", "en"))
    }

    @Test
    fun `o prompt cita os dois languages pelo name em ingles`() {
        val prompt = CardGenerator.promptFor(LanguagePairSpec.de("pt-BR", "de")!!)
        assertTrue(prompt.contains("Brazilian Portuguese"), prompt)
        assertTrue(prompt.contains("German"), prompt)
        assertTrue(prompt.contains("`pronunciation`"), prompt)
    }

    /**
     * The schema is hand-written JSON mirroring a data class, and nothing but
     * this test connects them. Drift does not fail to compile — it fails to
     * decode, on every card, at runtime.
     */
    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `the JSON schema matches every field of CardResponse`() {
        val fields = CardResponse.serializer().descriptor.elementNames.toSet()

        @Suppress("UNCHECKED_CAST")
        val properties = CardGenerator.SCHEMA_MAP["properties"] as Map<String, Any>
        @Suppress("UNCHECKED_CAST")
        val required = (CardGenerator.SCHEMA_MAP["required"] as List<String>).toSet()

        assertEquals(fields, properties.keys, "schema properties drifted from CardResponse")
        assertEquals(fields, required, "schema `required` drifted from CardResponse")
    }

    /** The app copies `type` through verbatim, so the two enums have to agree. */
    @Test
    fun `the schema enum matches TargetType`() {
        @Suppress("UNCHECKED_CAST")
        val properties = CardGenerator.SCHEMA_MAP["properties"] as Map<String, Map<String, Any>>
        assertEquals(
            TargetType.entries.map { it.name },
            properties.getValue("type").getValue("enum"),
        )
    }
}
