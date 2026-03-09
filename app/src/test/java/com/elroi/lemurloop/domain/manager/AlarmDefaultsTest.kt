package com.elroi.lemurloop.domain.manager

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * BUG-10: AlarmDefaults data class declares mathDifficulty = 0 (None) as default,
 * but SettingsManager.alarmDefaultsFlow falls back to ?: 1 (Easy) when unset.
 * New users unexpectedly get an Easy math challenge on first launch.
 * Fix: change SettingsManager line 322 from `?: 1` to `?: 0`.
 */
class AlarmDefaultsTest {

    @Test
    fun `AlarmDefaults data class default mathDifficulty is 0 (None)`() {
        val defaults = AlarmDefaults()
        assertEquals(
            "AlarmDefaults.mathDifficulty default should be 0 (None), not 1 (Easy)",
            0,
            defaults.mathDifficulty
        )
    }

    @Test
    fun `AlarmDefaults data class default mathProblemCount is 1`() {
        val defaults = AlarmDefaults()
        assertEquals(1, defaults.mathProblemCount)
    }

    @Test
    fun `AlarmDefaults data class default snoozeDurationMinutes is 5`() {
        val defaults = AlarmDefaults()
        assertEquals(5, defaults.snoozeDurationMinutes)
    }

    @Test
    fun `AlarmDefaults data class default aiPersona is COACH`() {
        val defaults = AlarmDefaults()
        assertEquals("COACH", defaults.aiPersona)
    }

    @Test
    fun `AlarmDefaults data class default isGentleWake is false`() {
        val defaults = AlarmDefaults()
        assertEquals(false, defaults.isGentleWake)
    }
}
