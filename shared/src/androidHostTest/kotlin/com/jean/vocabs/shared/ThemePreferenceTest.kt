package com.jean.vocabs.shared

import kotlin.test.Test
import kotlin.test.assertEquals

class ThemePreferenceTest {

    @Test
    fun `sem nada recorded o theme e o do device`() {
        // Instalação nova: ninguém escolheu ainda, e escolher pela pessoa seria
        // ignorar o que ela já configurou no sistema.
        assertEquals(ThemePreference.SYSTEM, ThemePreference.de(null))
    }

    @Test
    fun `value invalid cai no auto, e nao no light`() {
        // Vale para o dia em que um nome mudar: uma preferência ilegível volta a
        // seguir o aparelho, em vez de acender a tela branca de madrugada.
        assertEquals(ThemePreference.SYSTEM, ThemePreference.de(""))
        assertEquals(ThemePreference.SYSTEM, ThemePreference.de("SEPIA"))
    }

    @Test
    fun `as three choices sobrevivem a outbound e back do disk`() {
        ThemePreference.entries.forEach { theme ->
            assertEquals(theme, ThemePreference.de(theme.name))
        }
    }
}
