package ch.gryphus.creditdebittracker

import ch.gryphus.creditdebittracker.data.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeModeTest {

    @Test
    fun fromName_knownValues() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromName("SYSTEM"))
        assertEquals(ThemeMode.LIGHT, ThemeMode.fromName("LIGHT"))
        assertEquals(ThemeMode.DARK, ThemeMode.fromName("DARK"))
    }

    @Test
    fun fromName_defaultsToSystem() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromName(null))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromName(""))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromName("nonsense"))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromName("light")) // case-sensitive by name
    }

    @Test
    fun labels_areHumanReadable() {
        assertEquals("System", ThemeMode.SYSTEM.label)
        assertEquals("Light", ThemeMode.LIGHT.label)
        assertEquals("Dark", ThemeMode.DARK.label)
    }
}
