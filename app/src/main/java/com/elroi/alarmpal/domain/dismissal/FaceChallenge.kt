package com.elroi.alarmpal.domain.dismissal

import kotlin.math.abs

/**
 * A single face-expression challenge the user must hold for [HOLD_SECONDS] seconds.
 *
 * [emoji]       – large emoji shown on screen
 * [title]       – bold instruction line
 * [hint]        – smaller helper text
 * [ringColor]   – ARGB colour for this challenge's progress ring
 * [detect]      – returns true when the face is performing this expression
 */
sealed class FaceChallenge(
    val emoji: String,
    val title: String,
    val hint: String,
    val ringColor: Long,          // ARGB
    val detect: (FaceResult) -> Boolean
) {
    object Smile : FaceChallenge(
        emoji = "😊",
        title = "Flash a smile!",
        hint = "Open your mouth for better tracking",
        ringColor = 0xFFFFC107,   // Amber
        detect = { it.smilingProbability >= 0.75f }
    )

    object BigSmile : FaceChallenge(
        emoji = "😁",
        title = "Biggest smile!",
        hint = "Show those teeth! 😄",
        ringColor = 0xFFFF5722,   // Deep Orange
        detect = { it.smilingProbability >= 0.90f }
    )

    object WinkLeft : FaceChallenge(
        emoji = "😉",
        title = "Wink your left eye!",
        hint = "Close just the left one",
        ringColor = 0xFF03A9F4,   // Light Blue
        // ML Kit leftEye = user's RIGHT eye (front camera mirrors). Swap to match physical eye.
        detect = { it.rightEyeOpenProbability < 0.30f && it.leftEyeOpenProbability > 0.6f }
    )

    object WinkRight : FaceChallenge(
        emoji = "🤪",
        title = "Wink your right eye!",
        hint = "Close just the right one",
        ringColor = 0xFF9C27B0,   // Purple
        // ML Kit rightEye = user's LEFT eye (front camera mirrors). Swap to match physical eye.
        detect = { it.leftEyeOpenProbability < 0.30f && it.rightEyeOpenProbability > 0.6f }
    )

    object TiltHead : FaceChallenge(
        emoji = "🙃",
        title = "Tilt your head!",
        hint = "Roll it past 20°",
        ringColor = 0xFF4CAF50,   // Green
        detect = { abs(it.headEulerAngleZ) >= 20f }
    )

    companion object {
        private val ALL = listOf(Smile, BigSmile, WinkLeft, WinkRight, TiltHead)

        /** Returns [count] distinct challenges in a random order. */
        fun randomSequence(count: Int = 3): List<FaceChallenge> =
            ALL.shuffled().take(count)

        /** How many seconds each challenge must be held. */
        const val HOLD_SECONDS = 2
    }
}
