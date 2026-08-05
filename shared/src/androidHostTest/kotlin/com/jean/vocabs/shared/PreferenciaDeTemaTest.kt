package com.jean.vocabs.shared

import kotlin.test.Test
import kotlin.test.assertEquals

class PreferenciaDeTemaTest {

    @Test
    fun `sem nada gravado o theme e o do aparelho`() {
        // Instalação nova: ninguém escolheu ainda, e escolher pela pessoa seria
        // ignorar o que ela já configurou no sistema.
        assertEquals(ThemePreference.SYSTEM, ThemePreference.de(null))
    }

    @Test
    fun `value invalido cai no auto, e nao no claro`() {
        // Vale para o dia em que um nome mudar: uma preferência ilegível volta a
        // seguir o aparelho, em vez de acender a tela branca de madrugada.
        assertEquals(ThemePreference.SYSTEM, ThemePreference.de(""))
        assertEquals(ThemePreference.SYSTEM, ThemePreference.de("SEPIA"))
    }

    @Test
    fun `as tres escolhas sobrevivem a ida e volta do disco`() {
        ThemePreference.entries.forEach { theme ->
            assertEquals(theme, ThemePreference.de(theme.name))
        }
    }
}
