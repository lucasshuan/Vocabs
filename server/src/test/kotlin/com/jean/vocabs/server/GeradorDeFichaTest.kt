package com.jean.vocabs.server

import com.jean.vocabs.contracts.FichaResponse
import com.jean.vocabs.contracts.GerarFichaRequest
import com.jean.vocabs.contracts.TipoAlvo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GeradorDeFichaTest {
    @Test
    fun `servidor injeta tipo local e normaliza de tres a seis relacionados`() {
        val pedido = GerarFichaRequest(
            trecho = "He is on the fence",
            alvo = "on the fence",
            tipo = TipoAlvo.EXPRESSAO,
            idiomaNativo = "pt-BR",
            idiomaAlvo = "en",
        )
        val modelo = FichaResponse(
            tipo = TipoAlvo.PALAVRA,
            traducao = "indeciso",
            definicoes = listOf("Sem tomar uma decisão"),
            exemplo = "She remains on the fence.",
            pronuncia = "ɒn ðə fens",
            relacionadas = listOf(" undecided ", "hesitant", "uncertain", "hesitant", "wary", "doubtful", "unsure", "extra"),
        )

        val final = aplicarDecisoesLocais(pedido, modelo)
        assertEquals(TipoAlvo.EXPRESSAO, final.tipo)
        assertEquals(listOf("undecided", "hesitant", "uncertain", "wary", "doubtful", "unsure"), final.relacionadas)
    }

    @Test
    fun `a notacao de pronuncia segue o idioma alvo, com IPA como padrao`() {
        assertEquals("IPA, without slashes", ParDeIdiomas.de("pt-BR", "en")!!.alvo.notacaoDePronuncia)
        assertEquals("Hanyu Pinyin with tone marks", ParDeIdiomas.de("pt-BR", "zh")!!.alvo.notacaoDePronuncia)
        assertEquals("Revised Romanization of Korean", ParDeIdiomas.de("pt-BR", "ko")!!.alvo.notacaoDePronuncia)
        // Idioma sem entrada própria cai no IPA, que serve à maioria.
        assertEquals("IPA, without slashes", ParDeIdiomas.de("pt-BR", "sv")!!.alvo.notacaoDePronuncia)
    }

    @Test
    fun `par desconhecido nao vira o par padrao`() {
        // Recusar é o ponto: cair no padrão devolveria uma ficha em inglês para
        // uma palavra alemã, e a pessoa só descobriria lendo.
        assertNull(ParDeIdiomas.de("pt-BR", "klingon"))
        assertNull(ParDeIdiomas.de("elfico", "en"))
    }

    @Test
    fun `o prompt cita os dois idiomas pelo nome em ingles`() {
        val prompt = GeradorDeFicha.promptDe(ParDeIdiomas.de("pt-BR", "de")!!)
        assertTrue(prompt.contains("Brazilian Portuguese"), prompt)
        assertTrue(prompt.contains("German"), prompt)
        assertTrue(prompt.contains("`pronuncia`"), prompt)
    }
}
