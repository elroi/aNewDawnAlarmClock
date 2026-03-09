package com.elroi.lemurloop.domain.algorithm

import com.elroi.lemurloop.domain.manager.CalendarManager
import java.time.LocalTime
import javax.inject.Inject

class SmartWakeAlgorithm @Inject constructor(
    private val calendarManager: CalendarManager
) {

    fun suggestWakeTime(prepTimeMinutes: Long = 60, commuteTimeMinutes: Long = 30): LocalTime? {
        val firstEvent = calendarManager.getFirstEventOfNextDay() ?: return null
        
        // Logic: Event Time - (Prep + Commute)
        val eventTime = java.time.Instant.ofEpochMilli(firstEvent.startTime)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalTime()
            
        return eventTime
            .minusMinutes(prepTimeMinutes)
            .minusMinutes(commuteTimeMinutes)
    }
}
