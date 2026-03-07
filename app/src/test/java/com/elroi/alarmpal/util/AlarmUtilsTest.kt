package com.elroi.alarmpal.util

import com.elroi.alarmpal.domain.model.Alarm
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * BUG-6: AlarmUtils.calculateNextOccurrence edge case when alarmTime == now exactly.
 * The current check `isBefore(now) || isEqual(now)` correctly places it tomorrow,
 * so the existing logic is actually correct for non-repeating alarms.
 * These tests document the correct behaviour and guard against regressions.
 */
class AlarmUtilsTest {

    private fun makeAlarm(
        hour: Int,
        minute: Int,
        daysOfWeek: List<Int> = emptyList()
    ) = Alarm(
        time = LocalTime.of(hour, minute),
        daysOfWeek = daysOfWeek
    )

    // ── Non-repeating alarms ──────────────────────────────────────────────────

    @Test
    fun `non-repeating alarm in the future schedules same day`() {
        val now = LocalDateTime.of(2026, 3, 7, 8, 0, 0)
        val alarm = makeAlarm(9, 0)
        val result = AlarmUtils.calculateNextOccurrence(alarm, now)
        assertEquals(LocalDateTime.of(2026, 3, 7, 9, 0, 0), result)
    }

    @Test
    fun `non-repeating alarm in the past schedules next day`() {
        val now = LocalDateTime.of(2026, 3, 7, 10, 0, 0)
        val alarm = makeAlarm(9, 0)
        val result = AlarmUtils.calculateNextOccurrence(alarm, now)
        assertEquals(LocalDateTime.of(2026, 3, 8, 9, 0, 0), result)
    }

    @Test
    fun `non-repeating alarm at exact current time schedules tomorrow (BUG-6 edge case)`() {
        val now = LocalDateTime.of(2026, 3, 7, 9, 0, 0)
        val alarm = makeAlarm(9, 0) // Same hour:minute as 'now'
        val result = AlarmUtils.calculateNextOccurrence(alarm, now)
        // Should schedule TOMORROW, not today (can't ring for a time that is already now)
        assertEquals(
            "Alarm at exact current time should schedule for tomorrow",
            LocalDateTime.of(2026, 3, 8, 9, 0, 0),
            result
        )
    }

    @Test
    fun `non-repeating alarm 1 minute in the future schedules today`() {
        val now = LocalDateTime.of(2026, 3, 7, 8, 59, 30)
        val alarm = makeAlarm(9, 0)
        val result = AlarmUtils.calculateNextOccurrence(alarm, now)
        assertEquals(LocalDateTime.of(2026, 3, 7, 9, 0, 0), result)
    }

    // ── Repeating alarms ─────────────────────────────────────────────────────

    @Test
    fun `repeating alarm finds next occurrence today if time is in future`() {
        // Friday (5), 8:00 am, alarm at 9:00 am Mon-Fri
        val now = LocalDateTime.of(2026, 3, 6, 8, 0, 0) // Friday
        val alarm = makeAlarm(9, 0, listOf(1, 2, 3, 4, 5)) // Mon-Fri
        val result = AlarmUtils.calculateNextOccurrence(alarm, now)
        assertEquals(LocalDateTime.of(2026, 3, 6, 9, 0, 0), result)
    }

    @Test
    fun `repeating alarm on past time today schedules next matching day`() {
        // Friday (5), 10:00 am, alarm at 9:00 am Mon-Fri (already passed today)
        val now = LocalDateTime.of(2026, 3, 6, 10, 0, 0) // Friday
        val alarm = makeAlarm(9, 0, listOf(1, 2, 3, 4, 5)) // Mon-Fri
        val result = AlarmUtils.calculateNextOccurrence(alarm, now)
        // Next occurrence = Monday
        assertEquals(LocalDateTime.of(2026, 3, 9, 9, 0, 0), result)
    }

    @Test
    fun `repeating alarm for weekend only fires on Saturday from Friday`() {
        val now = LocalDateTime.of(2026, 3, 6, 10, 0, 0) // Friday
        val alarm = makeAlarm(8, 30, listOf(6, 7)) // Sat=6, Sun=7
        val result = AlarmUtils.calculateNextOccurrence(alarm, now)
        assertEquals(LocalDateTime.of(2026, 3, 7, 8, 30, 0), result) // Saturday
    }

    @Test
    fun `repeating alarm wraps around week correctly`() {
        val now = LocalDateTime.of(2026, 3, 8, 10, 0, 0) // Sunday (7)
        val alarm = makeAlarm(8, 30, listOf(1)) // Monday only
        val result = AlarmUtils.calculateNextOccurrence(alarm, now)
        assertEquals(LocalDateTime.of(2026, 3, 9, 8, 30, 0), result) // Monday
    }

    // ── formatTimeUntil ───────────────────────────────────────────────────────

    @Test
    fun `formatTimeUntil shows minutes when less than 1 hour`() {
        val now = LocalDateTime.of(2026, 3, 7, 8, 0, 0)
        val target = LocalDateTime.of(2026, 3, 7, 8, 45, 0)
        assertEquals("in 45m", AlarmUtils.formatTimeUntil(target, now))
    }

    @Test
    fun `formatTimeUntil shows hours and minutes`() {
        val now = LocalDateTime.of(2026, 3, 7, 8, 0, 0)
        val target = LocalDateTime.of(2026, 3, 7, 10, 30, 0)
        assertEquals("in 2h 30m", AlarmUtils.formatTimeUntil(target, now))
    }

    @Test
    fun `formatTimeUntil shows One-time when target is null`() {
        val now = LocalDateTime.of(2026, 3, 7, 10, 0, 0)
        assertEquals("One-time", AlarmUtils.formatTimeUntil(null, now))
    }
}
