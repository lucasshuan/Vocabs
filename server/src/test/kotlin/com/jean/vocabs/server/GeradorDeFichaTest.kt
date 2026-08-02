package com.jean.vocabs.server

import com.jean.vocabs.contracts.FichaResponse
import com.jean.vocabs.contracts.GerarFichaRequest
import com.jean.vocabs.contracts.TipoAlvo
import kotlin.test.Test
import kotlin.test.assertEquals

class GeradorDeFichaTest {
    @Test
    fun `servidor injeta tipo local e normaliza de tres a seis relacionados`() {
        val pedido = GerarFichaRequest("He is on the fence", "on the fence", TipoAlvo.EXPRESSAO)
        val modelo = FichaResponse(
            tipo = TipoAlvo.PALAVRA,
            traducao = "indeciso",
            definicoes = listOf("Sem tomar uma decisão"),
            exemplo = "She remains on the fence.",
            ipa = "/ɒn ðə fens/",
            relacionadas = listOf(" undecided ", "hesitant", "uncertain", "hesitant", "wary", "doubtful", "unsure", "extra"),
        )

        val final = aplicarDecisoesLocais(pedido, modelo)
        assertEquals(TipoAlvo.EXPRESSAO, final.tipo)
        assertEquals(listOf("undecided", "hesitant", "uncertain", "wary", "doubtful", "unsure"), final.relacionadas)
    }
}
