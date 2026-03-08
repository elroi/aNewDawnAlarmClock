package com.elroi.alarmpal.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.elroi.alarmpal.domain.model.Alarm
import java.time.LocalTime

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey
    val id: String,
    val hour: Int,
    val minute: Int,
    val label: String?,
    val isEnabled: Boolean,
    val daysOfWeek: String, // Comma separated integers
    val isVibrate: Boolean,
    val soundUri: String?,
    val isGentleWake: Boolean,
    @ColumnInfo(defaultValue = "1")
    val crescendoDurationMinutes: Int = 1,
    val mathDifficulty: Int,
    @ColumnInfo(defaultValue = "1")
    val mathProblemCount: Int = 1,
    @ColumnInfo(defaultValue = "0")
    val mathGraduallyIncreaseDifficulty: Boolean = false,
    val snoozeDurationMinutes: Int,
    val buddyPhoneNumber: String?,
    val buddyAlertDelayMinutes: Int = 5,
    val buddyName: String? = null,
    val userName: String? = null,
    val buddyMessage: String? = null,
    val smileToDismiss: Boolean = false,
    @ColumnInfo(defaultValue = "MATH")
    val smileFallbackMethod: String = "MATH",
    @ColumnInfo(defaultValue = "1")
    val isBriefingEnabled: Boolean = true,
    @ColumnInfo(defaultValue = "1")
    val isTtsEnabled: Boolean = true,
    @ColumnInfo(defaultValue = "0")
    val isEvasiveSnooze: Boolean = false,
    @ColumnInfo(defaultValue = "0")
    val evasiveSnoozesBeforeMoving: Int = 0,
    @ColumnInfo(defaultValue = "1")
    val isSmoothFadeOut: Boolean = true,
    @ColumnInfo(defaultValue = "1")
    val isSoundEnabled: Boolean = true,
    @ColumnInfo(defaultValue = "0")
    // BUG-11: isVibrateOnly has no domain model counterpart and is always stored as false.
    // Keeping the column to avoid a schema migration in this PR; remove in a future DB version bump.
    @Deprecated("Always false — has no domain model counterpart. Schedule for removal with next DB migration.")
    val isVibrateOnly: Boolean = false,
    @ColumnInfo(defaultValue = "1")
    val isSnoozeEnabled: Boolean = true,
    @ColumnInfo(defaultValue = "0")
    val isSmartWakeupEnabled: Boolean = false,
    @ColumnInfo(defaultValue = "3")
    val wakeupCheckDelayMinutes: Int = 3,
    @ColumnInfo(defaultValue = "60")
    val wakeupCheckTimeoutSeconds: Int = 60,
    @ColumnInfo(defaultValue = "30")
    val briefingTimeoutSeconds: Int = 30
) {
    fun toDomain(): Alarm {
        return Alarm(
            id = id,
            time = LocalTime.of(hour, minute),
            label = label,
            isEnabled = isEnabled,
            daysOfWeek = if (daysOfWeek.isBlank()) emptyList() else daysOfWeek.split(",").map { it.toInt() },
            isVibrate = isVibrate,
            soundUri = soundUri,
            isGentleWake = isGentleWake,
            crescendoDurationMinutes = crescendoDurationMinutes,
            mathDifficulty = mathDifficulty,
            mathProblemCount = mathProblemCount,
            mathGraduallyIncreaseDifficulty = mathGraduallyIncreaseDifficulty,
            snoozeDurationMinutes = snoozeDurationMinutes,
            buddyPhoneNumber = buddyPhoneNumber,
            buddyAlertDelayMinutes = buddyAlertDelayMinutes,
            buddyName = buddyName,
            userName = userName,
            buddyMessage = buddyMessage,
            smileToDismiss = smileToDismiss,
            smileFallbackMethod = smileFallbackMethod,
            isBriefingEnabled = isBriefingEnabled,
            isTtsEnabled = isTtsEnabled,
            isEvasiveSnooze = isEvasiveSnooze,
            evasiveSnoozesBeforeMoving = evasiveSnoozesBeforeMoving,
            isSmoothFadeOut = isSmoothFadeOut,
            isSoundEnabled = isSoundEnabled,
            isSnoozeEnabled = isSnoozeEnabled,
            isSmartWakeupEnabled = isSmartWakeupEnabled,
            wakeupCheckDelayMinutes = wakeupCheckDelayMinutes,
            wakeupCheckTimeoutSeconds = wakeupCheckTimeoutSeconds,
            briefingTimeoutSeconds = briefingTimeoutSeconds
            // isVibrateOnly is ignored in domain
        )
    }

    companion object {
        fun fromDomain(alarm: Alarm): AlarmEntity {
            return AlarmEntity(
                id = alarm.id,
                hour = alarm.time.hour,
                minute = alarm.time.minute,
                label = alarm.label,
                isEnabled = alarm.isEnabled,
                daysOfWeek = alarm.daysOfWeek.joinToString(","),
                isVibrate = alarm.isVibrate,
                soundUri = alarm.soundUri,
                isGentleWake = alarm.isGentleWake,
                crescendoDurationMinutes = alarm.crescendoDurationMinutes,
                mathDifficulty = alarm.mathDifficulty,
                mathProblemCount = alarm.mathProblemCount,
                mathGraduallyIncreaseDifficulty = alarm.mathGraduallyIncreaseDifficulty,
                snoozeDurationMinutes = alarm.snoozeDurationMinutes,
                buddyPhoneNumber = alarm.buddyPhoneNumber,
                buddyAlertDelayMinutes = alarm.buddyAlertDelayMinutes,
                buddyName = alarm.buddyName,
                userName = alarm.userName,
                buddyMessage = alarm.buddyMessage,
                smileToDismiss = alarm.smileToDismiss,
                smileFallbackMethod = alarm.smileFallbackMethod,
                isBriefingEnabled = alarm.isBriefingEnabled,
                isTtsEnabled = alarm.isTtsEnabled,
                isEvasiveSnooze = alarm.isEvasiveSnooze,
                evasiveSnoozesBeforeMoving = alarm.evasiveSnoozesBeforeMoving,
                isSmoothFadeOut = alarm.isSmoothFadeOut,
                isSoundEnabled = alarm.isSoundEnabled,
                isSnoozeEnabled = alarm.isSnoozeEnabled,
                isVibrateOnly = false, // Default or irrelevant now
                isSmartWakeupEnabled = alarm.isSmartWakeupEnabled,
                wakeupCheckDelayMinutes = alarm.wakeupCheckDelayMinutes,
                wakeupCheckTimeoutSeconds = alarm.wakeupCheckTimeoutSeconds,
                briefingTimeoutSeconds = alarm.briefingTimeoutSeconds
            )
        }
    }
}
