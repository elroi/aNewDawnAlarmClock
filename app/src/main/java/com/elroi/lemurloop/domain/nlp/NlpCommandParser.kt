package com.elroi.lemurloop.domain.nlp

import com.elroi.lemurloop.domain.model.Alarm
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern
import javax.inject.Inject

class NlpCommandParser @Inject constructor() {

    sealed class NlpResult {
        data class SetAlarm(val time: LocalTime, val label: String? = null) : NlpResult()
        data class SetTimer(val minutes: Int) : NlpResult() // Future scope
        object ClearAlarms : NlpResult()
        data class Error(val message: String) : NlpResult()
    }

    fun parse(input: String): NlpResult {
        val lowerInput = input.lowercase()

        // 1. Check for "Wake me up at X" or "Alarm at X"
        // Regex for time: \b(\d{1,2})(:(\d{2}))?\s*(am|pm)?\b
        val timePattern = Pattern.compile("at\\s+(\\d{1,2})(:(\\d{2}))?\\s*(am|pm)?")
        val matcher = timePattern.matcher(lowerInput)

        if (matcher.find()) {
            try {
                val hourStr = matcher.group(1)
                val minuteStr = matcher.group(3) ?: "00"
                val amPm = matcher.group(4)

                var hour = hourStr?.toIntOrNull() ?: return NlpResult.Error("Invalid hour")
                val minute = minuteStr.toIntOrNull() ?: 0

                if (amPm != null) {
                    if (amPm == "pm" && hour < 12) hour += 12
                    if (amPm == "am" && hour == 12) hour = 0
                }

                if (hour !in 0..23 || minute !in 0..59) {
                    return NlpResult.Error("Invalid time range")
                }

                return NlpResult.SetAlarm(LocalTime.of(hour, minute), "NLP Alarm")
            } catch (e: Exception) {
                return NlpResult.Error("Failed to parse time")
            }
        }

        // 2. Check for "Set alarm for X minutes"
        val durationPattern = Pattern.compile("in\\s+(\\d+)\\s*min")
        val durationMatcher = durationPattern.matcher(lowerInput)
        if (durationMatcher.find()) {
             val minutes = durationMatcher.group(1)?.toIntOrNull()
             if (minutes != null) {
                 val time = LocalTime.now().plusMinutes(minutes.toLong())
                 return NlpResult.SetAlarm(time, "Timer Alarm")
             }
        }
        
        // 3. Check for specific commands
        if (lowerInput.contains("clear all") || lowerInput.contains("delete all")) {
            return NlpResult.ClearAlarms
        }

        return NlpResult.Error("Could not understand command")
    }
}
