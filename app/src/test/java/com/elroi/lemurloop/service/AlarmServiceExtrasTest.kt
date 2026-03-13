package com.elroi.lemurloop.service
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * BUG-2: AlarmActivity reads intent extra "ALARM_EVASIVE_SNOOZE_BEFORE_MOVING" (no S before MOVING)
 * but the shared key is \"ALARM_EVASIVE_SNOOZES_BEFORE_MOVING\" (with S).
 *
 * BUG-8: AlarmService.handleSnooze() does not forward Smart-Wakeup extras or BRIEFING_ENABLED
 * to the snoozed alarm PendingIntent, so those features silently reset to defaults after snooze.
 *
 * These tests document the correct constant values used as extra keys, ensuring the keys
 * stay in sync across the codebase.
 */
class AlarmServiceExtrasTest {

    @Test
    fun `EXTRA_EVASIVE_SNOOZES_BEFORE_MOVING constant has correct key with S`() {
        // BUG-2: The key must end in "SNOOZES_BEFORE_MOVING" (plural with S), not "SNOOZE_BEFORE_MOVING"
        assertEquals(
            "ALARM_EVASIVE_SNOOZES_BEFORE_MOVING",
            AlarmIntentExtras.EXTRA_EVASIVE_SNOOZES_BEFORE_MOVING
        )
    }

    @Test
    fun `EXTRA_IS_SMART_WAKEUP_ENABLED constant exists and has expected value`() {
        // BUG-8: This extra must be forwarded by handleSnooze()
        assertEquals(
            "ALARM_IS_SMART_WAKEUP_ENABLED",
            AlarmIntentExtras.EXTRA_IS_SMART_WAKEUP_ENABLED
        )
    }

    @Test
    fun `EXTRA_WAKEUP_CHECK_DELAY constant exists and has expected value`() {
        assertEquals("ALARM_WAKEUP_CHECK_DELAY", AlarmIntentExtras.EXTRA_WAKEUP_CHECK_DELAY)
    }

    @Test
    fun `EXTRA_WAKEUP_CHECK_TIMEOUT constant exists and has expected value`() {
        assertEquals("ALARM_WAKEUP_CHECK_TIMEOUT", AlarmIntentExtras.EXTRA_WAKEUP_CHECK_TIMEOUT)
    }

    @Test
    fun `EXTRA_BRIEFING_ENABLED constant exists and has expected value`() {
        // BUG-8: This extra must be forwarded by handleSnooze()
        assertEquals("ALARM_BRIEFING_ENABLED", AlarmIntentExtras.EXTRA_BRIEFING_ENABLED)
    }

    @Test
    fun `EXTRA_ALARM_ID key matches what AlarmReceiver also uses`() {
        // Consistency check: all components use the same string key
        assertEquals("ALARM_ID", AlarmIntentExtras.EXTRA_ALARM_ID)
    }

    @Test
    fun `EXTRA_SOUND_URI constant exists and has expected value`() {
        assertEquals("ALARM_SOUND_URI", AlarmIntentExtras.EXTRA_SOUND_URI)
    }

    @Test
    fun `EXTRA_DAYS_OF_WEEK constant exists and has expected value`() {
        assertEquals("ALARM_DAYS_OF_WEEK", AlarmIntentExtras.EXTRA_DAYS_OF_WEEK)
    }

    @Test
    fun `EXTRA_SMILE_FALLBACK_METHOD constant exists and has expected value`() {
        assertEquals("ALARM_SMILE_FALLBACK_METHOD", AlarmIntentExtras.EXTRA_SMILE_FALLBACK_METHOD)
    }
}
