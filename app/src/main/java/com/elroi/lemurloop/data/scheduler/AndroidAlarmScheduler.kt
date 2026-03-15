package com.elroi.lemurloop.data.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.elroi.lemurloop.domain.model.Alarm
import com.elroi.lemurloop.domain.scheduler.AlarmScheduler
import com.elroi.lemurloop.receiver.AlarmReceiver
import com.elroi.lemurloop.service.AlarmIntentExtras
import java.time.LocalDateTime
import java.time.ZoneId
import androidx.work.WorkManager
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.ExistingWorkPolicy
import java.time.Duration
import com.elroi.lemurloop.domain.worker.BriefingWorker
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers

class AndroidAlarmScheduler(
    private val context: Context,
    private val briefingGenerator: com.elroi.lemurloop.domain.generator.BriefingGenerator
) : AlarmScheduler {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val alarmManager = context.getSystemService(AlarmManager::class.java)
    private val workManager = WorkManager.getInstance(context)

    @android.annotation.SuppressLint("ScheduleExactAlarm")
    override fun schedule(alarm: Alarm) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmIntentExtras.EXTRA_ALARM_ID, alarm.id)
            putExtra(AlarmIntentExtras.EXTRA_ALARM_LABEL, alarm.label)
            putExtra(AlarmIntentExtras.EXTRA_MATH_DIFFICULTY, alarm.mathDifficulty)
            putExtra(AlarmIntentExtras.EXTRA_MATH_PROBLEM_COUNT, alarm.mathProblemCount)
            putExtra(
                AlarmIntentExtras.EXTRA_MATH_GRADUAL_DIFFICULTY,
                alarm.mathGraduallyIncreaseDifficulty
            )
            putExtra(AlarmIntentExtras.EXTRA_SNOOZE_DURATION, alarm.snoozeDurationMinutes)
            putExtra(AlarmIntentExtras.EXTRA_SMILE_TO_DISMISS, alarm.smileToDismiss)
            putExtra(AlarmIntentExtras.EXTRA_SMILE_FALLBACK_METHOD, alarm.smileFallbackMethod)
            putExtra(AlarmIntentExtras.EXTRA_BRIEFING_ENABLED, alarm.isBriefingEnabled)
            putExtra(AlarmIntentExtras.EXTRA_TTS_ENABLED, alarm.isTtsEnabled)
            putExtra(AlarmIntentExtras.EXTRA_IS_EVASIVE_SNOOZE, alarm.isEvasiveSnooze)
            putExtra(
                AlarmIntentExtras.EXTRA_EVASIVE_SNOOZES_BEFORE_MOVING,
                alarm.evasiveSnoozesBeforeMoving
            )
            putExtra(AlarmIntentExtras.EXTRA_SOUND_URI, alarm.soundUri)
            putExtra(AlarmIntentExtras.EXTRA_IS_SMOOTH_FADE_OUT, alarm.isSmoothFadeOut)
            putExtra(AlarmIntentExtras.EXTRA_IS_VIBRATE, alarm.isVibrate)
            putExtra(AlarmIntentExtras.EXTRA_IS_SOUND_ENABLED, alarm.isSoundEnabled)
            putExtra(AlarmIntentExtras.EXTRA_IS_SNOOZE_ENABLED, alarm.isSnoozeEnabled)
            putExtra(AlarmIntentExtras.EXTRA_IS_SMART_WAKEUP_ENABLED, alarm.isSmartWakeupEnabled)
            putExtra(AlarmIntentExtras.EXTRA_WAKEUP_CHECK_DELAY, alarm.wakeupCheckDelayMinutes)
            putExtra(AlarmIntentExtras.EXTRA_WAKEUP_CHECK_TIMEOUT, alarm.wakeupCheckTimeoutSeconds)
            putExtra(AlarmIntentExtras.EXTRA_BRIEFING_TIMEOUT, alarm.briefingTimeoutSeconds)
            putExtra(AlarmIntentExtras.EXTRA_DAYS_OF_WEEK, alarm.daysOfWeek.joinToString(","))
            putExtra(AlarmIntentExtras.EXTRA_VIBRATION_PATTERN, alarm.vibrationPattern)
            putExtra(
                AlarmIntentExtras.EXTRA_VIBRATION_START_GAP,
                alarm.vibrationCrescendoStartGapSeconds
            )
        }
        
        // Calculate next alarm time using utility
        val alarmTime = com.elroi.lemurloop.util.AlarmUtils.calculateNextOccurrence(alarm)

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAtMillis = alarmTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        // For an alarm clock app, setAlarmClock is the preferred way to guarantee the device
        // wakes from Doze at the exact minute and shows the alarm in status bar.
        // If we don't have exact alarm permission (e.g. user denied on Android 12+), fall back
        // to setAndAllowWhileIdle so we don't crash; the alarm still fires but may be inexact.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            alarmManager.canScheduleExactAlarms()) {
            val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerAtMillis, pendingIntent)
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
        
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
