package com.elroi.lemurloop.domain.manager

import com.elroi.lemurloop.domain.model.Alarm
import com.elroi.lemurloop.domain.repository.AlarmRepository
import com.elroi.lemurloop.domain.scheduler.AlarmScheduler
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

class DemoAlarmSeederTest {

    private class FakeAlarmRepository(
        initialAlarms: List<Alarm> = emptyList()
    ) : AlarmRepository {
        private val state = MutableStateFlow(initialAlarms.toList())

        override fun getAllAlarms(): Flow<List<Alarm>> = state

        override suspend fun getAlarmById(id: String): Alarm? =
            state.value.firstOrNull { it.id == id }

        override suspend fun insertAlarm(alarm: Alarm) {
            state.value = state.value + alarm
        }

        override suspend fun deleteAlarm(alarm: Alarm) {
            state.value = state.value.filterNot { it.id == alarm.id }
        }

        override suspend fun updateAlarmToggle(id: String, isEnabled: Boolean) {
            state.value = state.value.map {
                if (it.id == id) it.copy(isEnabled = isEnabled) else it
            }
        }

        fun current(): List<Alarm> = state.value
    }

    private class FakeAlarmScheduler : AlarmScheduler {
        val scheduled = mutableListOf<Alarm>()

        override fun schedule(alarm: Alarm) {
            scheduled += alarm
        }

        override fun cancel(alarm: Alarm) {
            // no-op for tests
        }
    }

    @Test
    fun `seedDemoAlarms inserts rich demo set when empty`() = runBlocking {
        val repo = FakeAlarmRepository()
        val scheduler = FakeAlarmScheduler()
        val seeder = DemoAlarmSeeder(repo, scheduler)

        val insertedCount = seeder.seedDemoAlarms()

        val alarms = repo.current()
        assertEquals(insertedCount, alarms.size)
        // We expect a small, rich set (currently 5) of demo alarms
        assertEquals(5, alarms.size)

        // Basic shape checks – labels and times are as expected
        val labels = alarms.mapNotNull { it.label }.toSet()
        org.junit.Assert.assertTrue(labels.contains("Weekday Wake-Up"))
        org.junit.Assert.assertTrue(labels.contains("Gym Mission"))
        org.junit.Assert.assertTrue(labels.contains("Weekend Sleep-In"))
        org.junit.Assert.assertTrue(labels.contains("Smart Wake-Up Check"))
        org.junit.Assert.assertTrue(labels.contains("Face Game Challenge"))

        // Ensure alarms were scheduled
        assertEquals(5, scheduler.scheduled.size)
    }

    @Test
    fun `seedDemoAlarms is additive but avoids duplicate demo shapes`() = runBlocking {
        val existingAlarm = Alarm(
            time = LocalTime.of(7, 0),
            label = "Weekday Wake-Up",
            daysOfWeek = listOf(1, 2, 3, 4, 5)
        )
        val repo = FakeAlarmRepository(listOf(existingAlarm))
        val scheduler = FakeAlarmScheduler()
        val seeder = DemoAlarmSeeder(repo, scheduler)

        val insertedFirst = seeder.seedDemoAlarms()
        val totalAfterFirst = repo.current().size

        val insertedSecond = seeder.seedDemoAlarms()
        val totalAfterSecond = repo.current().size

        // First run should add all demo alarms except the existing matching one
        assertEquals(4, insertedFirst)
        assertEquals(5, totalAfterFirst)

        // Second run should detect all demo shapes as already present and add nothing
        assertEquals(0, insertedSecond)
        assertEquals(totalAfterFirst, totalAfterSecond)
    }
}

