package com.elroi.alarmpal.data.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.elroi.alarmpal.domain.model.Alarm
import com.elroi.alarmpal.domain.scheduler.AlarmScheduler
import com.elroi.alarmpal.receiver.AlarmReceiver
import java.time.LocalDateTime
import java.time.ZoneId
import androidx.work.WorkManager
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.ExistingWorkPolicy
import java.time.Duration
import com.elroi.alarmpal.domain.worker.BriefingWorker
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers

class AndroidAlarmScheduler(
    private val context: Context,
    private val briefingGenerator: com.elroi.alarmpal.domain.generator.BriefingGenerator
) : AlarmScheduler {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val alarmManager = context.getSystemService(AlarmManager::class.java)
    private val workManager = WorkManager.getInstance(context)

    @android.annotation.SuppressLint("ScheduleExactAlarm")
    override fun schedule(alarm: Alarm) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("ALARM_ID", alarm.id)
            putExtra("ALARM_LABEL", alarm.label)
            putExtra("ALARM_MATH_DIFFICULTY", alarm.mathDifficulty)
            putExtra("ALARM_MATH_PROBLEM_COUNT", alarm.mathProblemCount)
            putExtra("ALARM_MATH_GRADUAL_DIFFICULTY", alarm.mathGraduallyIncreaseDifficulty)
            putExtra("ALARM_SNOOZE_DURATION", alarm.snoozeDurationMinutes)
            putExtra("ALARM_SMILE_TO_DISMISS", alarm.smileToDismiss)
            putExtra(com.elroi.alarmpal.service.AlarmService.EXTRA_SMILE_FALLBACK_METHOD, alarm.smileFallbackMethod)
            putExtra("ALARM_BRIEFING_ENABLED", alarm.isBriefingEnabled)
            putExtra("ALARM_TTS_ENABLED", alarm.isTtsEnabled)
            putExtra("ALARM_IS_EVASIVE_SNOOZE", alarm.isEvasiveSnooze)
            putExtra("ALARM_EVASIVE_SNOOZES_BEFORE_MOVING", alarm.evasiveSnoozesBeforeMoving)
            putExtra("ALARM_SOUND_URI", alarm.soundUri)
            putExtra("ALARM_IS_SMOOTH_FADE_OUT", alarm.isSmoothFadeOut)
            putExtra("ALARM_IS_VIBRATE", alarm.isVibrate)
            putExtra("ALARM_IS_SOUND_ENABLED", alarm.isSoundEnabled)
            putExtra("ALARM_IS_SNOOZE_ENABLED", alarm.isSnoozeEnabled)
            putExtra(com.elroi.alarmpal.service.AlarmService.EXTRA_IS_SMART_WAKEUP_ENABLED, alarm.isSmartWakeupEnabled)
            putExtra(com.elroi.alarmpal.service.AlarmService.EXTRA_WAKEUP_CHECK_DELAY, alarm.wakeupCheckDelayMinutes)
            putExtra(com.elroi.alarmpal.service.AlarmService.EXTRA_WAKEUP_CHECK_TIMEOUT, alarm.wakeupCheckTimeoutSeconds)
            putExtra(com.elroi.alarmpal.service.AlarmService.EXTRA_DAYS_OF_WEEK, alarm.daysOfWeek.joinToString(","))
        }
        
        // Calculate next alarm time using utility
        val alarmTime = com.elroi.alarmpal.util.AlarmUtils.calculateNextOccurrence(alarm)

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // For an alarm clock app, setAlarmClock is the ONLY way to guarantee the device
        // wakes up from deep Doze mode at the exact minute. 
        val alarmClockInfo = AlarmManager.AlarmClockInfo(
            alarmTime.atZone(ZoneId.systemDefault()).toEpochSecond() * 1000,
            pendingIntent 
        )
        alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
        
        // Schedule AI Briefing pre-generation 30 minutes before the alarm
        if (alarm.isBriefingEnabled) {
            val briefingTime = alarmTime.minusMinutes(30)
            val delayMillis = Duration.between(LocalDateTime.now(), briefingTime).toMillis().coerceAtLeast(0)
            
            val workRequest = OneTimeWorkRequestBuilder<BriefingWorker>()
                .setInitialDelay(Duration.ofMillis(delayMillis))
                .build()
                
            workManager.enqueueUniqueWork(
                "briefing_${alarm.id}",
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
            
            // Proactively trigger generation if it's "now" or "soon" (< 30 mins)
            if (delayMillis == 0L) {
                android.util.Log.d("AndroidAlarmScheduler", "Alarm is imminent. Triggering immediate briefing generation...")
                scope.launch {
                    briefingGenerator.refreshBriefing()
                }
            }
        }
    }

    override fun cancel(alarm: Alarm) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        
        // Also cancel any pending briefing work
        workManager.cancelUniqueWork("briefing_${alarm.id}")
    }
}
