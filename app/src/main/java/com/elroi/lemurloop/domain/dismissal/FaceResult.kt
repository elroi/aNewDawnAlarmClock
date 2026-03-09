package com.elroi.lemurloop.domain.dismissal

/**
 * All face-classification data returned by ML Kit for a single frame.
 * Probabilities are in the range [0.0, 1.0]; Euler angles are in degrees.
 */
data class FaceResult(
    val smilingProbability: Float,
    val leftEyeOpenProbability: Float,
    val rightEyeOpenProbability: Float,
    /** Head roll: positive = tilted right, negative = tilted left */
    val headEulerAngleZ: Float
)
