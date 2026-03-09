package com.elroi.lemurloop.util

import com.elroi.lemurloop.domain.model.Alarm
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

object AlarmUtils {

    fun calculateNextOccurrence(alarm: Alarm, now: LocalDateTime = LocalDateTime.now()): LocalDateTime {
        val alarmTimeToday = now.withHour(alarm.time.hour).withMinute(alarm.time.minute).withSecond(0).withNano(0)
        
        if (alarm.daysOfWeek.isEmpty()) {
            // Non-repeating: if past, schedule for tomorrow
            return if (alarmTimeToday.isBefore(now) || alarmTimeToday.isEqual(now)) {
                alarmTimeToday.plusDays(1)
            } else {
                alarmTimeToday
            }
        } else {
            // Repeating: find next matching day
            // 1 = Monday, 7 = Sunday
            val currentDay = now.dayOfWeek.value 
            
            // Check if today is a selected day and time is in future
            if (alarm.daysOfWeek.contains(currentDay) && alarmTimeToday.isAfter(now)) {
                return alarmTimeToday
            } else {
                // Find next day
                var daysUntilNext = -1
                for (i in 1..7) {
                    val nextDay = (currentDay + i - 1) % 7 + 1
                    if (alarm.daysOfWeek.contains(nextDay)) {
                        daysUntilNext = i
                        break
                    }
                }
                
                return if (daysUntilNext != -1) {
                    alarmTimeToday.plusDays(daysUntilNext.toLong())
                } else {
                    alarmTimeToday.plusDays(1)
                }
            }
        }
    }

    fun formatTimeUntil(target: LocalDateTime?, now: LocalDateTime = LocalDateTime.now()): String {
        if (target == null) return "One-time"
        
        // Truncate both to minutes to handle the "off by 1 min" truncation issue
        val nowTruncated = now.truncatedTo(ChronoUnit.MINUTES)
        val targetTruncated = target.truncatedTo(ChronoUnit.MINUTES)
        
        val duration = Duration.between(nowTruncated, targetTruncated)
        val totalMinutes = duration.toMinutes()
        
        if (totalMinutes <= 0) return "now"
        
        val days = duration.toDays()
        val hours = duration.toHours() % 24
        val minutes = duration.toMinutes() % 60
        
        return buildString {
            append("in ")
            if (days > 0) {
                append("${days}d ")
                if (hours > 0) append("${hours}h")
            } else if (hours > 0) {
                append("${hours}h ")
                if (minutes > 0) append("${minutes}m")
            } else {
                append("${minutes}m")
            }
        }.trim()
    }
}
