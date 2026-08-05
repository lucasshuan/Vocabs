package com.jean.vocabs.shared

import kotlin.test.Test
import kotlin.test.assertEquals

class ThemePreferenceTest {

    @Test
    fun `sem nada recorded o theme e o do device`() {
        // A fresh install: nobody has chosen yet, and choosing for them would
        // ignore what they already set on the system.
        assertEquals(ThemePreference.SYSTEM, ThemePreference.de(null))
    }

    @Test
    fun `value invalid cai no auto, e nao no light`() {
        // For the day a name changes: an unreadable preference goes back to
        // following the device rather than lighting a white screen at 3am.
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
