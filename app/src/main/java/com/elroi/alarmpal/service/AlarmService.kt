package com.elroi.alarmpal.service

import com.elroi.alarmpal.util.BriefingUtils

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.elroi.alarmpal.R
import com.elroi.alarmpal.receiver.AlarmReceiver
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

@AndroidEntryPoint
class AlarmService : Service() {

    @Inject
    lateinit var repository: com.elroi.alarmpal.domain.repository.AlarmRepository

    @Inject
    lateinit var ttsManager: com.elroi.alarmpal.domain.manager.TextToSpeechManager

    @Inject
    lateinit var briefingGenerator: com.elroi.alarmpal.domain.generator.BriefingGenerator

    @Inject
    lateinit var accountabilityManager: com.elroi.alarmpal.domain.manager.AccountabilityManager

    @Inject
    lateinit var diagnosticLogger: com.elroi.alarmpal.domain.manager.DiagnosticLogger

    private var ringtone: Ringtone? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var volumeAnimator: android.animation.ValueAnimator? = null
    private var accountabilityJob: kotlinx.coroutines.Job? = null
    private var ttsJob: kotlinx.coroutines.Job? = null
    private var precomputedBriefing: kotlinx.coroutines.Deferred<String>? = null
    private var vibrator: android.os.Vibrator? = null
    private val vibrationPattern = longArrayOf(0, 500, 500) // 0 delay, 0.5s on, 0.5s off
    private var vibrationJob: kotlinx.coroutines.Job? = null

    // Retained for use in snooze action
    private var currentAlarmId: String? = null
    private var currentIsRepeating: Boolean = false
    private var currentAlarmLabel: String = "Alarm"
    private var currentSnoozeDuration: Int = 5
    private var currentMathDifficulty: Int = 0
    private var currentMathProblemCount: Int = 1
    private var currentMathGraduallyIncreaseDifficulty: Boolean = false
    private var currentSmileToDismiss: Boolean = false
    private var currentSmileFallbackMethod: String = "MATH"
    private var currentSoundUri: String? = null
    private var currentDaysOfWeek: String? = null
    private var currentBriefingEnabled: Boolean = true
    private var currentTtsEnabled: Boolean = true
    private var currentSnoozeCount: Int = 0
    private var currentIsEvasiveSnooze: Boolean = false
    private var currentEvasiveSnoozesBeforeMoving: Int = 0
    private var currentIsSmoothFadeOut: Boolean = true
    private var currentIsVibrate: Boolean = true
    private var currentIsSoundEnabled: Boolean = true
    private var currentIsSnoozeEnabled: Boolean = true
    private var currentIsSmartWakeupEnabled: Boolean = false
    private var currentWakeupCheckDelayMinutes: Int = 3
    private var currentWakeupCheckTimeoutSeconds: Int = 60
    private var currentBriefingTimeoutSeconds: Int = 30
    private var currentVibrationPattern: String = "BASIC"
    private var currentVibrationStartGapSeconds: Int = 30
    
    // TTS audio state tracking
    private var originalMediaVolume: Int = 0
    private var volumeWasBoosted: Boolean = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DISMISS -> {
                handleDismiss()
                return START_NOT_STICKY
            }
            ACTION_SNOOZE -> {
                val alarmId = intent.getStringExtra(EXTRA_ALARM_ID) ?: currentAlarmId
                val snoozeMins = intent.getIntExtra(EXTRA_SNOOZE_DURATION, currentSnoozeDuration)
                handleSnooze(alarmId, snoozeMins)
                return START_NOT_STICKY
            }
            ACTION_MUTE_RINGTONE -> {
                handleMute()
                return START_NOT_STICKY
            }
            ACTION_STOP_TTS -> {
                handleStopTts()
                return START_NOT_STICKY
            }
            ACTION_PAUSE_TTS -> {
                Log.d("AlarmService", "Pausing TTS")
                ttsManager.stop()
                return START_NOT_STICKY
            }
            ACTION_RESUME_TTS -> {
                Log.d("AlarmService", "Resuming TTS")
                val text = (com.elroi.alarmpal.domain.manager.BriefingStateManager.briefingState.value as? com.elroi.alarmpal.domain.manager.BriefingState.Ready)?.text
                if (!text.isNullOrBlank()) {
                    serviceScope.launch { ttsManager.speak(text) }
                }
                return START_NOT_STICKY
            }
        }

        val alarmId = intent?.getStringExtra(EXTRA_ALARM_ID) ?: return START_NOT_STICKY
        val alarmLabel = intent.getStringExtra(EXTRA_ALARM_LABEL) ?: "Alarm"
        val mathDifficulty = intent.getIntExtra(EXTRA_MATH_DIFFICULTY, 0)
        val mathProblemCount = intent.getIntExtra(EXTRA_MATH_PROBLEM_COUNT, 1)
        val mathGradualDifficulty = intent.getBooleanExtra(EXTRA_MATH_GRADUAL_DIFFICULTY, false)
        val snoozeDuration = intent.getIntExtra(EXTRA_SNOOZE_DURATION, 5)
        val snoozeCount = intent.getIntExtra(EXTRA_SNOOZE_COUNT, 0)
        val smileToDismiss = intent.getBooleanExtra(EXTRA_SMILE_TO_DISMISS, false)
        val smileFallbackMethod = intent.getStringExtra(EXTRA_SMILE_FALLBACK_METHOD) ?: "MATH"
        val briefingEnabled = intent.getBooleanExtra(EXTRA_BRIEFING_ENABLED, true)
        val ttsEnabled = intent.getBooleanExtra(EXTRA_TTS_ENABLED, true)
        val isEvasiveSnooze = intent.getBooleanExtra(EXTRA_IS_EVASIVE_SNOOZE, false)
        val evasiveSnoozesBeforeMoving = intent.getIntExtra(EXTRA_EVASIVE_SNOOZES_BEFORE_MOVING, 0)
        val isSmoothFadeOut = intent.getBooleanExtra(EXTRA_IS_SMOOTH_FADE_OUT, true)
        val soundUriStr = intent.getStringExtra(EXTRA_SOUND_URI)
        val isVibrate = intent.getBooleanExtra(EXTRA_IS_VIBRATE, true)
        val isSoundEnabled = intent.getBooleanExtra(EXTRA_IS_SOUND_ENABLED, true)
        val isSnoozeEnabled = intent.getBooleanExtra(EXTRA_IS_SNOOZE_ENABLED, true)
        val isSmartWakeupEnabled = intent.getBooleanExtra(EXTRA_IS_SMART_WAKEUP_ENABLED, false)
        val wakeupCheckDelay = intent.getIntExtra(EXTRA_WAKEUP_CHECK_DELAY, 3)
        val wakeupCheckTimeout = intent.getIntExtra(EXTRA_WAKEUP_CHECK_TIMEOUT, 60)
        val briefingTimeout = intent.getIntExtra(EXTRA_BRIEFING_TIMEOUT, 30)
        val vibrationPattern = intent.getStringExtra(EXTRA_VIBRATION_PATTERN) ?: "BASIC"
        val vibrationStartGap = intent.getIntExtra(EXTRA_VIBRATION_START_GAP, 30)
        val daysOfWeekStr = intent.getStringExtra(EXTRA_DAYS_OF_WEEK)

        // Stash for later use in snooze/dismiss helpers
        currentAlarmId = alarmId
        currentIsRepeating = intent.getStringExtra(EXTRA_DAYS_OF_WEEK)?.isNotEmpty() ?: false
        currentAlarmLabel = alarmLabel
        currentSnoozeDuration = snoozeDuration
        currentMathDifficulty = mathDifficulty
        currentMathProblemCount = mathProblemCount
        currentMathGraduallyIncreaseDifficulty = mathGradualDifficulty
        currentSmileToDismiss = smileToDismiss
        currentSmileFallbackMethod = smileFallbackMethod
        currentSoundUri = soundUriStr
        currentDaysOfWeek = daysOfWeekStr
        currentBriefingEnabled = briefingEnabled
        currentTtsEnabled = ttsEnabled
        currentSnoozeCount = snoozeCount
        currentIsEvasiveSnooze = isEvasiveSnooze
        currentEvasiveSnoozesBeforeMoving = evasiveSnoozesBeforeMoving
        currentIsSmoothFadeOut = isSmoothFadeOut
        currentIsVibrate = isVibrate
        currentIsSoundEnabled = isSoundEnabled
        currentIsSnoozeEnabled = isSnoozeEnabled
        currentIsSmartWakeupEnabled = isSmartWakeupEnabled
        currentWakeupCheckDelayMinutes = wakeupCheckDelay
        currentWakeupCheckTimeoutSeconds = wakeupCheckTimeout
        currentBriefingTimeoutSeconds = briefingTimeout
        currentVibrationPattern = vibrationPattern
        currentVibrationStartGapSeconds = vibrationStartGap

        val activityIntent = Intent(this, com.elroi.alarmpal.ui.activity.AlarmActivity::class.java).apply {
            putExtra(EXTRA_ALARM_ID, alarmId)
            putExtra(EXTRA_ALARM_LABEL, alarmLabel)
            putExtra(EXTRA_MATH_DIFFICULTY, mathDifficulty)
            putExtra(EXTRA_MATH_PROBLEM_COUNT, mathProblemCount)
            putExtra(EXTRA_MATH_GRADUAL_DIFFICULTY, mathGradualDifficulty)
            putExtra(EXTRA_SNOOZE_DURATION, snoozeDuration)
            putExtra(EXTRA_SNOOZE_COUNT, snoozeCount)
            putExtra(EXTRA_SMILE_TO_DISMISS, smileToDismiss)
            putExtra(EXTRA_SMILE_FALLBACK_METHOD, smileFallbackMethod)
            putExtra(EXTRA_IS_EVASIVE_SNOOZE, isEvasiveSnooze)
            putExtra(EXTRA_EVASIVE_SNOOZES_BEFORE_MOVING, evasiveSnoozesBeforeMoving)
            putExtra(EXTRA_IS_SMOOTH_FADE_OUT, isSmoothFadeOut)
            putExtra(EXTRA_IS_VIBRATE, isVibrate)
            putExtra(EXTRA_IS_SOUND_ENABLED, isSoundEnabled)
            putExtra(EXTRA_IS_SNOOZE_ENABLED, isSnoozeEnabled)
            putExtra(EXTRA_IS_SMART_WAKEUP_ENABLED, isSmartWakeupEnabled)
            putExtra(EXTRA_WAKEUP_CHECK_DELAY, wakeupCheckDelay)
            putExtra(EXTRA_WAKEUP_CHECK_TIMEOUT, wakeupCheckTimeout)
            putExtra(EXTRA_BRIEFING_ENABLED, briefingEnabled)
            putExtra(EXTRA_TTS_ENABLED, ttsEnabled)
            putExtra(EXTRA_BRIEFING_TIMEOUT, briefingTimeout)
            putExtra(EXTRA_VIBRATION_PATTERN, vibrationPattern)
            putExtra(EXTRA_VIBRATION_START_GAP, vibrationStartGap)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or 
                     Intent.FLAG_ACTIVITY_CLEAR_TOP or 
                     Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        // With SYSTEM_ALERT_WINDOW (Display over other apps) permission, we can launch from the background.
        // We catch any potential exception just in case the permission was revoked,
        // relying on the FullScreenIntent notification as a fallback.
        try {
            diagnosticLogger.debug("AlarmService", "Launching AlarmActivity with flags: ${activityIntent.flags}")
            startActivity(activityIntent)
        } catch (e: Exception) {
            diagnosticLogger.error("AlarmService", "Failed to start AlarmActivity from background: ${e.message}")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID, 
                buildNotification(alarmLabel, snoozeDuration, activityIntent),
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, buildNotification(alarmLabel, snoozeDuration, activityIntent))
        }

        serviceScope.launch {
            // BUG-3 FIX: fetch alarm once and reuse — the previous code had a shadowed
            // `val alarm` inside the `if (isVibrate)` block causing a redundant DB call.
            val alarm = repository.getAlarmById(alarmId)
            
            if (isSoundEnabled) {
                playRingtone(
                    soundUriStr = soundUriStr,
                    isGentleWake = alarm?.isGentleWake ?: false,
                    crescendoMinutes = alarm?.crescendoDurationMinutes ?: 1
                )
            }
            
            if (isVibrate) {
                startVibration(
                    isGentleWake = alarm?.isGentleWake ?: false,
                    crescendoMinutes = alarm?.crescendoDurationMinutes ?: 1,
                    pattern = alarm?.vibrationPattern ?: "BASIC",
                    startGapSeconds = alarm?.vibrationCrescendoStartGapSeconds ?: 30
                )
            }

            if (!alarm?.buddyPhoneNumber.isNullOrBlank()) {
                startAccountabilityTimer(
                    phoneNumber = alarm!!.buddyPhoneNumber!!,
                    delayMinutes = alarm.buddyAlertDelayMinutes,
                    alarmLabel = alarm.label,
                    userName = alarm.userName,
                    customMessage = alarm.buddyMessage
                )
            }
        }
        
        if (intent?.action == null && currentBriefingEnabled) {
            Log.d("TTS_DEBUG", "Initial alarm start: Pre-computing briefing")
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
            originalMediaVolume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
            
            com.elroi.alarmpal.domain.manager.BriefingStateManager.startGenerating()
            ttsManager.initializeIfNeeded()
            precomputedBriefing = serviceScope.async {
                briefingGenerator.generateBriefing()
            }
            
            // Listen for TTS speech completion
            if (currentTtsEnabled) {
                serviceScope.launch {
                    ttsManager.onSpeechCompleted.collect {
                        Log.d("TTS_DEBUG", "Received onSpeechCompleted in AlarmService, updating state to Completed")
                        com.elroi.alarmpal.domain.manager.BriefingStateManager.markCompleted()
                    }
                }
            }
        }

        return START_STICKY
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Notification
    // ──────────────────────────────────────────────────────────────────────────

    private fun buildNotification(label: String, snoozeDuration: Int, activityIntent: Intent): Notification {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val channelId = if (currentIsVibrate) CHANNEL_ID_VIBRATE else CHANNEL_ID_SILENT

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Re-create the specific channel.
            // Android does NOT update channel properties after first creation, so we need two distinct channels.
            val channel = NotificationChannel(channelId, "LemurLoop Alarm ($channelId)", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Ringing alarm controls"
                setSound(null, null)
                enableVibration(currentIsVibrate)
                if (currentIsVibrate) {
                    vibrationPattern = longArrayOf(0, 500, 500)
                }
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)
            }
            nm.createNotificationChannel(channel)
        }

        // Tap → re-open AlarmActivity
        val contentPendingIntent = PendingIntent.getActivity(
            this, 0, activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )


        // Build PendingIntent for FullScreenIntent launch
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this, 1, activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(label.ifBlank { "Alarm" })
            .setContentText("Tap to open alarm and dismiss")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Tap to open alarm and dismiss")
                .setBigContentTitle("⏰ ${label.ifBlank { "Alarm" }}"))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(contentPendingIntent)
            .setOngoing(true)      // stays in shade, can't be manually swiped away
            .setAutoCancel(false)
            .setShowWhen(true)
            .build()
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Actions
    // ──────────────────────────────────────────────────────────────────────────

    private fun handleMute() {
        stopRingtone(useFadeOut = currentIsSmoothFadeOut)
        stopVibration()
    }

    private fun handleDismiss() {
        Log.d("TTS_DEBUG", "handleDismiss called")
        
        // Fetch the alarm directly to verify if it's repeating, rather than just relying on the intent.
        serviceScope.launch(kotlinx.coroutines.NonCancellable) {
            val alarmId = currentAlarmId ?: return@launch
            val alarm = repository.getAlarmById(alarmId)
            if (alarm != null && alarm.daysOfWeek.isEmpty()) {
                Log.d("TTS_DEBUG", "One-time alarm dismissed, disabling: $alarmId")
                repository.updateAlarmToggle(alarmId, false)
            }
        }

        accountabilityJob?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopVibration()

        if (currentBriefingEnabled) {
            // Start the briefing retrieval and UI update IMMEDIATELY, don't wait for ringtone fade-out
            ttsJob = serviceScope.launch {
                Log.d("TTS_DEBUG", "Awaiting pre-computed briefing...")
                val briefing = precomputedBriefing?.await() ?: briefingGenerator.generateBriefing()
                Log.d("TTS_DEBUG", "Briefing ready: $briefing")
                
                com.elroi.alarmpal.domain.manager.BriefingStateManager.onBriefingReady(briefing)
                
                // Now handle ringtone fadeout and TTS concurrently
                stopRingtone(useFadeOut = currentIsSmoothFadeOut) {
                    if (currentTtsEnabled && currentIsSoundEnabled) {
                        Log.d("TTS_DEBUG", "TTS is enabled, using cached original volume: $originalMediaVolume")
                        val audioManager = getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
                        val maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
                        val targetVolume = (maxVolume * 0.7f).toInt()
                        
                        volumeWasBoosted = originalMediaVolume < targetVolume
                        if (volumeWasBoosted) {
                            audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, targetVolume, 0)
                        }
                        serviceScope.launch {
                            val filteredBriefing = BriefingUtils.filterBriefingForTts(briefing)
                            ttsManager.speak(filteredBriefing)
                        }
                    } else if (currentBriefingEnabled) {
                        // If briefing is enabled but TTS is off (or sound is off), 
                        // mark completed after the configured reading timeout
                        serviceScope.launch {
                            Log.d("TTS_DEBUG", "TTS disabled, waiting ${currentBriefingTimeoutSeconds}s for reading...")
                            delay(currentBriefingTimeoutSeconds * 1000L)
                            com.elroi.alarmpal.domain.manager.BriefingStateManager.markCompleted()
                        }
                    }
                }
            }
        } else {
            Log.d("TTS_DEBUG", "Briefing disabled, stopping service")
            com.elroi.alarmpal.domain.manager.BriefingStateManager.onBriefingReady(null) // Empty null means auto-close UI immediately
            
            stopRingtone(useFadeOut = currentIsSmoothFadeOut) { 
                if (currentIsSmartWakeupEnabled) {
                    scheduleWakeupCheck()
                }
                stopSelf() 
            }
        }
    }

    private fun scheduleWakeupCheck() {
        Log.d("WakeupCheck", "Scheduling wakeup check for alarm $currentAlarmId")
        val alarmId = currentAlarmId ?: return
        val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        val intent = Intent(this, com.elroi.alarmpal.receiver.WakeupCheckReceiver::class.java).apply {
            putExtra(EXTRA_ALARM_ID, alarmId)
            putExtra(EXTRA_ALARM_LABEL, currentAlarmLabel)
            putExtra(EXTRA_MATH_DIFFICULTY, currentMathDifficulty)
            putExtra(EXTRA_MATH_PROBLEM_COUNT, currentMathProblemCount)
            putExtra(EXTRA_MATH_GRADUAL_DIFFICULTY, currentMathGraduallyIncreaseDifficulty)
            putExtra(EXTRA_SNOOZE_DURATION, currentSnoozeDuration)
            putExtra(EXTRA_SMILE_TO_DISMISS, currentSmileToDismiss)
            putExtra(EXTRA_TTS_ENABLED, currentTtsEnabled)
            putExtra(EXTRA_IS_EVASIVE_SNOOZE, currentIsEvasiveSnooze)
            putExtra(EXTRA_EVASIVE_SNOOZES_BEFORE_MOVING, currentEvasiveSnoozesBeforeMoving)
            putExtra(EXTRA_SOUND_URI, currentSoundUri)
            putExtra(EXTRA_DAYS_OF_WEEK, currentDaysOfWeek)
            putExtra(EXTRA_SMILE_FALLBACK_METHOD, currentSmileFallbackMethod)
            putExtra(EXTRA_IS_SMOOTH_FADE_OUT, currentIsSmoothFadeOut)
            putExtra(EXTRA_IS_VIBRATE, currentIsVibrate)
            putExtra(EXTRA_IS_SOUND_ENABLED, currentIsSoundEnabled)
            putExtra(EXTRA_IS_SNOOZE_ENABLED, currentIsSnoozeEnabled)
            putExtra(EXTRA_IS_SMART_WAKEUP_ENABLED, true)
            putExtra(EXTRA_WAKEUP_CHECK_DELAY, currentWakeupCheckDelayMinutes)
            putExtra(EXTRA_WAKEUP_CHECK_TIMEOUT, currentWakeupCheckTimeoutSeconds)
            putExtra(EXTRA_VIBRATION_PATTERN, currentVibrationPattern)
            putExtra(EXTRA_VIBRATION_START_GAP, currentVibrationStartGapSeconds)
        }
        
        val pi = PendingIntent.getBroadcast(
            this,
            alarmId.hashCode() + 777,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val triggerAt = System.currentTimeMillis() + (currentWakeupCheckDelayMinutes * 60 * 1000L)
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
    }

    private fun handleStopTts() {
        Log.d("TTS_DEBUG", "handleStopTts called")
        precomputedBriefing?.cancel()
        ttsJob?.cancel()
        ttsManager.shutdown()
        com.elroi.alarmpal.domain.manager.BriefingStateManager.clear()
        
        if (volumeWasBoosted) {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
            audioManager.setStreamVolume(
                android.media.AudioManager.STREAM_MUSIC,
                originalMediaVolume,
                0
            )
            Log.d("TTS_DEBUG", "Restored volume back to $originalMediaVolume")
        }
        
        if (currentIsSmartWakeupEnabled) {
            scheduleWakeupCheck()
        }
        
        serviceScope.launch(kotlinx.coroutines.NonCancellable) {
            val alarmId = currentAlarmId ?: return@launch
            val alarm = repository.getAlarmById(alarmId)
            if (alarm != null && alarm.daysOfWeek.isEmpty()) {
                Log.d("TTS_DEBUG", "One-time alarm TTS finished, disabling: $alarmId")
                repository.updateAlarmToggle(alarmId, false)
            }
        }

        stopSelf()
    }

    private fun handleSnooze(alarmId: String?, snoozeMins: Int) {
        accountabilityJob?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopVibration()

        if (alarmId != null) {
            val triggerMs = LocalDateTime.now()
                .plusMinutes(snoozeMins.toLong())
                .atZone(ZoneId.systemDefault())
                .toEpochSecond() * 1_000L

            val intent = Intent(this, AlarmReceiver::class.java).apply {
                putExtra(EXTRA_ALARM_ID, alarmId)
                putExtra(EXTRA_ALARM_LABEL, currentAlarmLabel)
                putExtra(EXTRA_SNOOZE_DURATION, snoozeMins)
                putExtra(EXTRA_SNOOZE_COUNT, currentSnoozeCount + 1)
                putExtra(EXTRA_MATH_DIFFICULTY, currentMathDifficulty)
                putExtra(EXTRA_MATH_PROBLEM_COUNT, currentMathProblemCount)
                putExtra(EXTRA_MATH_GRADUAL_DIFFICULTY, currentMathGraduallyIncreaseDifficulty)
                putExtra(EXTRA_SMILE_TO_DISMISS, currentSmileToDismiss)
                putExtra(EXTRA_TTS_ENABLED, currentTtsEnabled)
                putExtra(EXTRA_IS_EVASIVE_SNOOZE, currentIsEvasiveSnooze)
                putExtra(EXTRA_EVASIVE_SNOOZES_BEFORE_MOVING, currentEvasiveSnoozesBeforeMoving)
                putExtra(EXTRA_SOUND_URI, currentSoundUri)
                putExtra(EXTRA_DAYS_OF_WEEK, currentDaysOfWeek)
                putExtra(EXTRA_SMILE_FALLBACK_METHOD, currentSmileFallbackMethod)
                putExtra(EXTRA_IS_SMOOTH_FADE_OUT, currentIsSmoothFadeOut)
                putExtra(EXTRA_IS_VIBRATE, currentIsVibrate)
                putExtra(EXTRA_IS_SOUND_ENABLED, currentIsSoundEnabled)
                putExtra(EXTRA_IS_SNOOZE_ENABLED, currentIsSnoozeEnabled)
                // BUG-8 FIX: forward smart-wakeup and briefing extras so snoozed alarms
                // preserve these settings instead of silently resetting to defaults.
                putExtra(EXTRA_IS_SMART_WAKEUP_ENABLED, currentIsSmartWakeupEnabled)
                putExtra(EXTRA_WAKEUP_CHECK_DELAY, currentWakeupCheckDelayMinutes)
                putExtra(EXTRA_WAKEUP_CHECK_TIMEOUT, currentWakeupCheckTimeoutSeconds)
                putExtra(EXTRA_BRIEFING_ENABLED, currentBriefingEnabled)
                putExtra(EXTRA_VIBRATION_PATTERN, currentVibrationPattern)
                putExtra(EXTRA_VIBRATION_START_GAP, currentVibrationStartGapSeconds)
            }
            val pi = PendingIntent.getBroadcast(
                this,
                alarmId.hashCode() + 999,   // distinct request code
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pi)
        }

        stopRingtone(useFadeOut = currentIsSmoothFadeOut) { stopSelf() }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Ringtone helpers
    // ──────────────────────────────────────────────────────────────────────────

    private fun startAccountabilityTimer(
        phoneNumber: String,
        delayMinutes: Int = 5,
        alarmLabel: String? = null,
        userName: String? = null,
        customMessage: String? = null
    ) {
        accountabilityJob = serviceScope.launch {
            kotlinx.coroutines.delay(delayMinutes * 60_000L)
            accountabilityManager.sendMissedAlarmMessage(
                phoneNumber = phoneNumber,
                alarmLabel = alarmLabel,
                userName = userName,
                customMessage = customMessage
            )
        }
    }

    private fun playRingtone(soundUriStr: String?, isGentleWake: Boolean, crescendoMinutes: Int = 1) {
        val uri = if (soundUriStr != null) {
            try {
                android.net.Uri.parse(soundUriStr)
            } catch (e: Exception) {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            }
        } else {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        }
        ringtone = RingtoneManager.getRingtone(this, uri)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ringtone?.isLooping = true
            ringtone?.volume = if (isGentleWake) 0.0f else 1.0f
        }
        ringtone?.audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        
        if (currentIsSoundEnabled) {
            ringtone?.play()
        }

        if (isGentleWake && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            startVolumeCrescendo(crescendoMinutes)
        }
    }

    private fun startVolumeCrescendo(durationMinutes: Int) {
        // 0 minutes = jump straight to full volume immediately
        val durationMs = if (durationMinutes <= 0) 1L else durationMinutes * 60_000L
        volumeAnimator = android.animation.ValueAnimator.ofFloat(0.0f, 1.0f).apply {
            duration = durationMs
            addUpdateListener { animation ->
                val volume = animation.animatedValue as Float
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ringtone?.volume = volume
                }
            }
            start()
        }
    }

    private fun stopRingtone(useFadeOut: Boolean = true, onComplete: () -> Unit = {}) {
        volumeAnimator?.cancel()
        
        if (useFadeOut && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && ringtone != null && ringtone!!.isPlaying) {
            val startVolume = ringtone!!.volume
            if (startVolume > 0f) {
                volumeAnimator = android.animation.ValueAnimator.ofFloat(startVolume, 0f).apply {
                    duration = 1000L // 1 second gentle fade out
                    addUpdateListener { animation ->
                        val volume = animation.animatedValue as Float
                        ringtone?.volume = volume
                    }
                    addListener(object : android.animation.AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: android.animation.Animator) {
                            ringtone?.stop()
                            onComplete()
                        }
                    })
                    start()
                }
                return
            }
        }
        
        ringtone?.stop()
        onComplete()
    }

    private fun startVibration(
        isGentleWake: Boolean = false, 
        crescendoMinutes: Int = 1,
        pattern: String = "BASIC",
        startGapSeconds: Int = 30
    ) {
        val vibratorService = getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
        vibrator = vibratorService
        
        if (!isGentleWake || Build.VERSION.SDK_INT < Build.VERSION_CODES.O || !vibratorService.hasAmplitudeControl()) {
            // Default legacy vibration
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(android.os.VibrationEffect.createWaveform(vibrationPattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(vibrationPattern, 0)
            }
            return
        }

        if (!currentIsVibrate) return

        // Gentle Wake: Intensity ramping + Pulse manipulation
        vibrationJob = serviceScope.launch {
            // 0 minutes = jump straight to full pattern immediately
            val durationMs = if (crescendoMinutes <= 0) 1L else crescendoMinutes * 60_000L
            val startTime = System.currentTimeMillis()
            
            while (true) {
                val elapsed = System.currentTimeMillis() - startTime
                if (elapsed >= durationMs) {
                    // Constant max intensity after crescendo
                    vibrator?.vibrate(android.os.VibrationEffect.createWaveform(vibrationPattern, 0))
                    break
                }
                
                val progress = elapsed.toFloat() / durationMs.toFloat()
                
                // Interpolate variables:
                // Amplitude: 50 -> 255
                val amplitude = (50 + (205 * progress)).toInt().coerceIn(0, 255)
                // Pulse duration: 100ms -> 800ms
                val pulseLen = (100 + (700 * progress)).toLong()
                // Pause duration: startGap -> 400ms
                val pauseLen = (startGapSeconds * 1000L - ((startGapSeconds * 1000L - 400L) * progress)).toLong().coerceAtLeast(100L)
                
                Log.d("AlarmService", "Vibration ramping: amp=$amplitude, pLen=$pulseLen, gap=$pauseLen, pattern=$pattern")

                when (pattern) {
                    "PULSE" -> {
                        vibrator?.vibrate(android.os.VibrationEffect.createOneShot(pulseLen / 2, amplitude))
                        delay((pulseLen / 4).coerceAtLeast(50L))
                        vibrator?.vibrate(android.os.VibrationEffect.createOneShot(pulseLen / 2, amplitude))
                    }
                    "HEARTBEAT" -> {
                        vibrator?.vibrate(android.os.VibrationEffect.createOneShot(pulseLen / 3, (amplitude * 0.7f).toInt().coerceIn(0, 255)))
                        delay(150L)
                        vibrator?.vibrate(android.os.VibrationEffect.createOneShot(pulseLen / 2, amplitude))
                    }
                    "STACCATO" -> {
                        repeat(3) {
                            vibrator?.vibrate(android.os.VibrationEffect.createOneShot(80, amplitude))
                            delay(120L)
                        }
                    }
                    else -> { // BASIC
                        vibrator?.vibrate(android.os.VibrationEffect.createOneShot(pulseLen, amplitude))
                    }
                }
                
                delay(pauseLen)
            }
        }
    }

    private fun stopVibration() {
        vibrationJob?.cancel()
        vibrationJob = null
        vibrator?.cancel()
        vibrator = null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopVibration()
        ringtone?.stop()

        // BUG-4 FIX: When TTS completes naturally (user doesn't tap Stop), the service
        // self-destructs via stopSelf() but scheduleWakeupCheck() was never called.
        // We schedule it here in onDestroy() so it fires regardless of the dismissal path.
        // Guard: only schedule if smart-wakeup is enabled AND briefing was active
        // (handleStopTts already calls this when user manually stops TTS).
        if (currentIsSmartWakeupEnabled && currentBriefingEnabled && ttsJob?.isCancelled == false) {
            scheduleWakeupCheck()
        }

        serviceScope.cancel()
        ttsManager.shutdown()
        accountabilityJob?.cancel()
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Constants
    // ──────────────────────────────────────────────────────────────────────────

    companion object {
        const val ACTION_SNOOZE = "com.elroi.alarmpal.SNOOZE"
        const val ACTION_DISMISS = "com.elroi.alarmpal.DISMISS"
        const val ACTION_MUTE_RINGTONE = "com.elroi.alarmpal.MUTE_RINGTONE"
        const val ACTION_PAUSE_TTS = "com.elroi.alarmpal.PAUSE_TTS"
        const val ACTION_RESUME_TTS = "com.elroi.alarmpal.RESUME_TTS"
        const val ACTION_STOP_TTS = "com.elroi.alarmpal.STOP_TTS"
        const val ACTION_BRIEFING_READY = "com.elroi.alarmpal.BRIEFING_READY"

        const val EXTRA_ALARM_ID = "ALARM_ID"
        const val EXTRA_ALARM_LABEL = "ALARM_LABEL"
        const val EXTRA_MATH_DIFFICULTY = "ALARM_MATH_DIFFICULTY"
        const val EXTRA_MATH_PROBLEM_COUNT = "ALARM_MATH_PROBLEM_COUNT"
        const val EXTRA_MATH_GRADUAL_DIFFICULTY = "ALARM_MATH_GRADUAL_DIFFICULTY"
        const val EXTRA_SNOOZE_DURATION = "ALARM_SNOOZE_DURATION"
        const val EXTRA_SNOOZE_COUNT = "ALARM_SNOOZE_COUNT"
        const val EXTRA_SMILE_TO_DISMISS = "ALARM_SMILE_TO_DISMISS"
        const val EXTRA_SMILE_FALLBACK_METHOD = "ALARM_SMILE_FALLBACK_METHOD"
        const val EXTRA_BRIEFING_ENABLED = "ALARM_BRIEFING_ENABLED"
        const val EXTRA_TTS_ENABLED = "ALARM_TTS_ENABLED"
        const val EXTRA_IS_EVASIVE_SNOOZE = "ALARM_IS_EVASIVE_SNOOZE"
        const val EXTRA_EVASIVE_SNOOZES_BEFORE_MOVING = "ALARM_EVASIVE_SNOOZES_BEFORE_MOVING"
        const val EXTRA_BRIEFING_TEXT = "EXTRA_BRIEFING_TEXT"
        const val EXTRA_SOUND_URI = "ALARM_SOUND_URI"
        const val EXTRA_IS_SMOOTH_FADE_OUT = "ALARM_IS_SMOOTH_FADE_OUT"
        const val EXTRA_IS_VIBRATE = "ALARM_IS_VIBRATE"
        const val EXTRA_IS_SOUND_ENABLED = "ALARM_IS_SOUND_ENABLED"
        const val EXTRA_IS_SNOOZE_ENABLED = "ALARM_IS_SNOOZE_ENABLED"
        const val EXTRA_IS_SMART_WAKEUP_ENABLED = "ALARM_IS_SMART_WAKEUP_ENABLED"
        const val EXTRA_WAKEUP_CHECK_DELAY = "ALARM_WAKEUP_CHECK_DELAY"
        const val EXTRA_WAKEUP_CHECK_TIMEOUT = "ALARM_WAKEUP_CHECK_TIMEOUT"
        const val EXTRA_DAYS_OF_WEEK = "ALARM_DAYS_OF_WEEK"
        const val EXTRA_BRIEFING_TIMEOUT = "ALARM_BRIEFING_TIMEOUT"
        const val EXTRA_VIBRATION_PATTERN = "ALARM_VIBRATION_PATTERN"
        const val EXTRA_VIBRATION_START_GAP = "ALARM_VIBRATION_START_GAP"

        private const val CHANNEL_ID_VIBRATE = "alarm_ringing_channel_vibrate"
        private const val CHANNEL_ID_SILENT = "alarm_ringing_channel_silent"
        private const val NOTIFICATION_ID = 111
        private const val REQ_SNOOZE = 1001
        private const val REQ_DISMISS = 1002
    }
}
