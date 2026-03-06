package com.elroi.alarmpal.domain.model

import java.time.LocalTime
import java.util.UUID

data class Alarm(
    val id: String = UUID.randomUUID().toString(),
    val time: LocalTime,
    val label: String? = null,
    val isEnabled: Boolean = true,
    val daysOfWeek: List<Int> = emptyList(), // 1 = Monday, 7 = Sunday
    val isVibrate: Boolean = true,
    val soundUri: String? = null,
    val isGentleWake: Boolean = false,
    val crescendoDurationMinutes: Int = 1, // 0 = instant full volume, 1-20 min fade-in
    val mathDifficulty: Int = 0, // 0 = None, 1 = Easy, 2 = Medium, 3 = Hard
    val mathProblemCount: Int = 1, // 1-10 max problems
    val mathGraduallyIncreaseDifficulty: Boolean = false,
    val snoozeDurationMinutes: Int = 5,
    val buddyPhoneNumber: String? = null,
    val buddyAlertDelayMinutes: Int = 5,
    val buddyName: String? = null,
    val userName: String? = null,
    val buddyMessage: String? = null,
    val smileToDismiss: Boolean = false,
    val smileFallbackMethod: String = "MATH", // "NONE" or "MATH"
    val isBriefingEnabled: Boolean = true,
    val isTtsEnabled: Boolean = true,
    val isEvasiveSnooze: Boolean = false,
    val evasiveSnoozesBeforeMoving: Int = 0,
    val isSmoothFadeOut: Boolean = true,
    val isSoundEnabled: Boolean = true,
    val isSnoozeEnabled: Boolean = true,
    val isSmartWakeupEnabled: Boolean = false,
    val wakeupCheckDelayMinutes: Int = 3,
    val wakeupCheckTimeoutSeconds: Int = 60,
    val aiPersona: String? = null
)
