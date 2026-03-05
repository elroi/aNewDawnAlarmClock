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
        val alarmId = intent.getStringExtra("ALARM_ID") ?: return
        val alarmLabel = intent.getStringExtra("ALARM_LABEL")

        Log.d("AlarmReceiver", "Alarm triggered: $alarmId - $alarmLabel")

        // Start the Alarm Service (to play sound and show UI)
        val serviceIntent = Intent(context, com.elroi.alarmpal.service.AlarmService::class.java).apply {
            putExtra("ALARM_ID", alarmId)
            putExtra("ALARM_LABEL", alarmLabel)
            putExtra("ALARM_MATH_DIFFICULTY", intent.getIntExtra("ALARM_MATH_DIFFICULTY", 0))
            putExtra("ALARM_MATH_PROBLEM_COUNT", intent.getIntExtra("ALARM_MATH_PROBLEM_COUNT", 1))
            putExtra("ALARM_MATH_GRADUAL_DIFFICULTY", intent.getBooleanExtra("ALARM_MATH_GRADUAL_DIFFICULTY", false))
            putExtra("ALARM_SNOOZE_DURATION", intent.getIntExtra("ALARM_SNOOZE_DURATION", 5))
            putExtra("ALARM_SNOOZE_COUNT", intent.getIntExtra("ALARM_SNOOZE_COUNT", 0))
            putExtra("ALARM_SMILE_TO_DISMISS", intent.getBooleanExtra("ALARM_SMILE_TO_DISMISS", false))
            putExtra("ALARM_TTS_ENABLED", intent.getBooleanExtra("ALARM_TTS_ENABLED", true))
            putExtra("ALARM_IS_EVASIVE_SNOOZE", intent.getBooleanExtra("ALARM_IS_EVASIVE_SNOOZE", false))
            putExtra("ALARM_EVASIVE_SNOOZES_BEFORE_MOVING", intent.getIntExtra("ALARM_EVASIVE_SNOOZES_BEFORE_MOVING", 0))
            putExtra("ALARM_SOUND_URI", intent.getStringExtra("ALARM_SOUND_URI"))
            putExtra("ALARM_IS_SMOOTH_FADE_OUT", intent.getBooleanExtra("ALARM_IS_SMOOTH_FADE_OUT", true))
            putExtra("ALARM_IS_VIBRATE", intent.getBooleanExtra("ALARM_IS_VIBRATE", true))
            putExtra("ALARM_IS_SOUND_ENABLED", intent.getBooleanExtra("ALARM_IS_SOUND_ENABLED", true))
            putExtra("ALARM_IS_SNOOZE_ENABLED", intent.getBooleanExtra("ALARM_IS_SNOOZE_ENABLED", true))
            putExtra(com.elroi.alarmpal.service.AlarmService.EXTRA_IS_SMART_WAKEUP_ENABLED, intent.getBooleanExtra(com.elroi.alarmpal.service.AlarmService.EXTRA_IS_SMART_WAKEUP_ENABLED, false))
            putExtra(com.elroi.alarmpal.service.AlarmService.EXTRA_WAKEUP_CHECK_DELAY, intent.getIntExtra(com.elroi.alarmpal.service.AlarmService.EXTRA_WAKEUP_CHECK_DELAY, 3))
            putExtra(com.elroi.alarmpal.service.AlarmService.EXTRA_WAKEUP_CHECK_TIMEOUT, intent.getIntExtra(com.elroi.alarmpal.service.AlarmService.EXTRA_WAKEUP_CHECK_TIMEOUT, 60))
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
