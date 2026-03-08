package com.elroi.alarmpal.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.elroi.alarmpal.domain.repository.AlarmRepository
import com.elroi.alarmpal.domain.scheduler.AlarmScheduler
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface AlarmReceiverEntryPoint {
        fun alarmRepository(): AlarmRepository
        fun alarmScheduler(): AlarmScheduler
    }

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getStringExtra(com.elroi.alarmpal.service.AlarmService.EXTRA_ALARM_ID) ?: return
        val alarmLabel = intent.getStringExtra(com.elroi.alarmpal.service.AlarmService.EXTRA_ALARM_LABEL)

        Log.d("AlarmReceiver", "Alarm triggered: $alarmId - $alarmLabel")

        // Start the Alarm Service (to play sound and show UI)
        val serviceIntent = Intent(context, com.elroi.alarmpal.service.AlarmService::class.java).apply {
            putExtra(com.elroi.alarmpal.service.AlarmService.EXTRA_ALARM_ID, alarmId)
            putExtra(com.elroi.alarmpal.service.AlarmService.EXTRA_ALARM_LABEL, alarmLabel)
            putExtra(com.elroi.alarmpal.service.AlarmService.EXTRA_MATH_DIFFICULTY, intent.getIntExtra(com.elroi.alarmpal.service.AlarmService.EXTRA_MATH_DIFFICULTY, 0))
            putExtra(com.elroi.alarmpal.service.AlarmService.EXTRA_MATH_PROBLEM_COUNT, intent.getIntExtra(com.elroi.alarmpal.service.AlarmService.EXTRA_MATH_PROBLEM_COUNT, 1))
            putExtra(com.elroi.alarmpal.service.AlarmService.EXTRA_MATH_GRADUAL_DIFFICULTY, intent.getBooleanExtra(com.elroi.alarmpal.service.AlarmService.EXTRA_MATH_GRADUAL_DIFFICULTY, false))
            putExtra(com.elroi.alarmpal.service.AlarmService.EXTRA_SNOOZE_DURATION, intent.getIntExtra(com.elroi.alarmpal.service.AlarmService.EXTRA_SNOOZE_DURATION, 5))
            putExtra(com.elroi.alarmpal.service.AlarmService.EXTRA_SNOOZE_COUNT, intent.getIntExtra(com.elroi.alarmpal.service.AlarmService.EXTRA_SNOOZE_COUNT, 0))
            putExtra(com.elroi.alarmpal.service.AlarmService.EXTRA_SMILE_TO_DISMISS, intent.getBooleanExtra(com.elroi.alarmpal.service.AlarmService.EXTRA_SMILE_TO_DISMISS, false))
            putExtra(com.elroi.alarmpal.service.AlarmService.EXTRA_BRIEFING_ENABLED, intent.getBooleanExtra(com.elroi.alarmpal.service.AlarmService.EXTRA_BRIEFING_ENABLED, true))
            putExtra(com.elroi.alarmpal.service.AlarmService.EXTRA_TTS_ENABLED, intent.getBooleanExtra(com.elroi.alarmpal.service.AlarmService.EXTRA_TTS_ENABLED, true))
            putExtra(com.elroi.alarmpal.service.AlarmService.EXTRA_IS_EVASIVE_SNOOZE, intent.getBooleanExtra(com.elroi.alarmpal.service.AlarmService.EXTRA_IS_EVASIVE_SNOOZE, false))
            putExtra(com.elroi.alarmpal.service.AlarmService.EXTRA_EVASIVE_SNOOZES_BEFORE_MOVING, intent.getIntExtra(com.elroi.alarmpal.service.AlarmService.EXTRA_EVASIVE_SNOOZES_BEFORE_MOVING, 0))
            putExtra(com.elroi.alarmpal.service.AlarmService.EXTRA_SOUND_URI, intent.getStringExtra(com.elroi.alarmpal.service.AlarmService.EXTRA_SOUND_URI))
            putExtra(com.elroi.alarmpal.service.AlarmService.EXTRA_IS_SMOOTH_FADE_OUT, intent.getBooleanExtra(com.elroi.alarmpal.service.AlarmService.EXTRA_IS_SMOOTH_FADE_OUT, true))
            putExtra(com.elroi.alarmpal.service.AlarmService.EXTRA_IS_VIBRATE, intent.getBooleanExtra(com.elroi.alarmpal.service.AlarmService.EXTRA_IS_VIBRATE, true))
            putExtra(com.elroi.alarmpal.service.AlarmService.EXTRA_IS_SOUND_ENABLED, intent.getBooleanExtra(com.elroi.alarmpal.service.AlarmService.EXTRA_IS_SOUND_ENABLED, true))
            putExtra(com.elroi.alarmpal.service.AlarmService.EXTRA_IS_SNOOZE_ENABLED, intent.getBooleanExtra(com.elroi.alarmpal.service.AlarmService.EXTRA_IS_SNOOZE_ENABLED, true))
            putExtra(com.elroi.alarmpal.service.AlarmService.EXTRA_IS_SMART_WAKEUP_ENABLED, intent.getBooleanExtra(com.elroi.alarmpal.service.AlarmService.EXTRA_IS_SMART_WAKEUP_ENABLED, false))
            putExtra(com.elroi.alarmpal.service.AlarmService.EXTRA_WAKEUP_CHECK_DELAY, intent.getIntExtra(com.elroi.alarmpal.service.AlarmService.EXTRA_WAKEUP_CHECK_DELAY, 3))
            putExtra(com.elroi.alarmpal.service.AlarmService.EXTRA_WAKEUP_CHECK_TIMEOUT, intent.getIntExtra(com.elroi.alarmpal.service.AlarmService.EXTRA_WAKEUP_CHECK_TIMEOUT, 60))
            putExtra(com.elroi.alarmpal.service.AlarmService.EXTRA_BRIEFING_TIMEOUT, intent.getIntExtra(com.elroi.alarmpal.service.AlarmService.EXTRA_BRIEFING_TIMEOUT, 30))
            putExtra(com.elroi.alarmpal.service.AlarmService.EXTRA_VIBRATION_PATTERN, intent.getStringExtra(com.elroi.alarmpal.service.AlarmService.EXTRA_VIBRATION_PATTERN))
            putExtra(com.elroi.alarmpal.service.AlarmService.EXTRA_VIBRATION_START_GAP, intent.getIntExtra(com.elroi.alarmpal.service.AlarmService.EXTRA_VIBRATION_START_GAP, 30))
            putExtra(com.elroi.alarmpal.service.AlarmService.EXTRA_DAYS_OF_WEEK, intent.getStringExtra(com.elroi.alarmpal.service.AlarmService.EXTRA_DAYS_OF_WEEK))
            putExtra(com.elroi.alarmpal.service.AlarmService.EXTRA_SMILE_FALLBACK_METHOD, intent.getStringExtra(com.elroi.alarmpal.service.AlarmService.EXTRA_SMILE_FALLBACK_METHOD))
        }
        context.startForegroundService(serviceIntent)

        // Reschedule repeating alarms for the next occurrence
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val entryPoint = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    AlarmReceiverEntryPoint::class.java
                )
                val alarm = entryPoint.alarmRepository().getAlarmById(alarmId)
                if (alarm != null && alarm.daysOfWeek.isNotEmpty()) {
                    Log.d("AlarmReceiver", "Rescheduling repeating alarm: $alarmId")
                    entryPoint.alarmScheduler().schedule(alarm)
                }
            } catch (e: Exception) {
                Log.e("AlarmReceiver", "Failed to reschedule alarm", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
