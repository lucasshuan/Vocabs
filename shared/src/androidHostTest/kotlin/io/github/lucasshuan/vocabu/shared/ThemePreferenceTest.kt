package io.github.lucasshuan.vocabu.shared

import kotlin.test.Test
import kotlin.test.assertEquals

class ThemePreferenceTest {

    @Test
    fun `with nothing recorded the theme follows the device`() {
        // A fresh install: nobody has chosen yet, and choosing for them would
        // ignore what they already set on the system.
        assertEquals(ThemePreference.SYSTEM, ThemePreference.of(null))
    }

    @Test
    fun `an unreadable value falls back to auto, not to light`() {
        // For the day a name changes: an unreadable preference goes back to
        // following the device rather than lighting a white screen at 3am.
        assertEquals(ThemePreference.SYSTEM, ThemePreference.of(""))
        assertEquals(ThemePreference.SYSTEM, ThemePreference.of("SEPIA"))
    }

    @Test
    fun `all three choices survive a round trip through the disk`() {
        ThemePreference.entries.forEach { theme ->
            assertEquals(theme, ThemePreference.of(theme.name))
        }
    }
}
