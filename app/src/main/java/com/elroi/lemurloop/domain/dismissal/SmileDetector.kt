package com.elroi.lemurloop.domain.dismissal

import android.annotation.SuppressLint
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Thin wrapper around ML Kit FaceDetector.
 * Analyses a single camera frame and returns a [FaceResult] for the first detected face,
 * or null if no face was detected.
 *
 * CLASSIFICATION_MODE_ALL gives us smilingProbability + eye-open probabilities.
 * Head Euler angles are always available without extra config.
 */
class SmileDetector {

    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setMinFaceSize(0.20f)
            .build()
    )

    /**
     * Processes one [ImageProxy] frame from CameraX and returns a [FaceResult],
     * or null if no face was detected. Closes the ImageProxy when done.
     */
    @SuppressLint("UnsafeOptInUsageError")
    suspend fun getFaceResult(imageProxy: ImageProxy): FaceResult? =
        suspendCancellableCoroutine { continuation ->
            val mediaImage = imageProxy.image
            if (mediaImage == null) {
                imageProxy.close()
                continuation.resume(null)
                return@suspendCancellableCoroutine
            }

            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            detector.process(image)
                .addOnSuccessListener { faces ->
                    val face = faces.firstOrNull()
                    val result = face?.let {
                        FaceResult(
                            smilingProbability       = it.smilingProbability       ?: 0f,
                            leftEyeOpenProbability   = it.leftEyeOpenProbability   ?: 1f,
                            rightEyeOpenProbability  = it.rightEyeOpenProbability  ?: 1f,
                            headEulerAngleZ          = it.headEulerAngleZ
                        )
                    }
                    continuation.resume(result)
                }
                .addOnFailureListener {
                    continuation.resume(null)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        }

    fun close() {
        detector.close()
    }
}
