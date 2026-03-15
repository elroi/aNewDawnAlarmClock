package com.elroi.lemurloop.ui.activity

import android.app.Activity
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.elroi.lemurloop.R
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.elroi.lemurloop.domain.dismissal.FaceChallenge
import com.elroi.lemurloop.domain.dismissal.FaceResult
import com.elroi.lemurloop.domain.dismissal.SmileDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import kotlin.math.abs

/**
 * Full-screen camera preview that guides the user through a randomised sequence of
 * [FaceChallenge.HOLD_SECONDS]-second face challenges. Calls [onDismissed] when all are done.
 */
@Composable
fun SmileDismissScreen(
    lux: Float,
    fallbackMethod: String,
    onFlashlightStateChanged: (Boolean) -> Unit,
    onFallbackTriggered: () -> Unit,
    onDismissed: () -> Unit
) {
    val context      = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val detector     = remember { SmileDetector() }

    // Build a random 3-challenge sequence once per composition
    val challenges   = remember { FaceChallenge.randomSequence(3) }

    var currentIndex by remember { mutableIntStateOf(0) }
    var holdSeconds  by remember { mutableIntStateOf(0) }
    var isHolding    by remember { mutableStateOf(false) }

    // ── Phone-upright detection via accelerometer ─────────────────────────────
    // The user must be physically sitting/standing (phone held upright, portrait).
    // We check that the gravity Y component is dominant — if the phone is lying flat
    // (Y ≈ 0, Z ≈ 9.8) we show "Sit up!" and pause detection.
    var isPhoneUpright by remember { mutableStateOf(true) }
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                // Y-axis = vertical when phone is portrait + upright
                // Threshold: Y ≥ 7.5 m/s² means phone is not lying flat
                val yGravity = abs(event.values[1])
                isPhoneUpright = yGravity >= 7.5f
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        sensor?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI)
        }
        onDispose { sensorManager.unregisterListener(listener) }
    }

    // ── Soft Flashlight (Ambient Light Sensor) ────────────────────────────────
    // Uses the hardware light sensor to detect if the room is too dark (< 10 lux).
    // If so, we activate the top/bottom white screen panels and set max brightness.
    // It stays on consistently until the face challenges are completed.
    var isRoomDark by remember { mutableStateOf(false) }
    LaunchedEffect(lux) {
        if (lux < 10f) {
            isRoomDark = true
        }
    }

    // Smoothly fade the flashlight on or off based on the room's ambient light
    val flashlightAlpha by animateFloatAsState(
        targetValue = if (isRoomDark) 1f else 0f,
        animationSpec = tween(durationMillis = 1000), 
        label = "flashlightAlpha"
    )

    // Notify activity about flashlight state
    LaunchedEffect(flashlightAlpha) {
        onFlashlightStateChanged(flashlightAlpha > 0.01f)
    }

    // Auto-fallback timer
    var activeSeconds by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (activeSeconds < 30) {
            delay(1000L)
            activeSeconds++
        }
        if (fallbackMethod != "NONE") {
            onFallbackTriggered()
        } else {
            // If no fallback is allowed, just let them keep trying
            activeSeconds = 0
        }
    }

    // Derived
    val challenge    = challenges.getOrNull(currentIndex)
    val progress     = holdSeconds.toFloat() / FaceChallenge.HOLD_SECONDS.toFloat()
    val ringColor    = challenge?.let { Color(it.ringColor) } ?: Color.White

    // Keep a state-backed ref so the camera callback always sees the current challenge
    // (the factory lambda captures it once; rememberUpdatedState avoids stale closure)
    val currentChallenge by rememberUpdatedState(challenge)

    var showStepSuccess by remember { mutableStateOf(false) }

    // Count up while holding; advance or dismiss when complete
    // Also restarts when isPhoneUpright changes — so lying flat pauses progress
    LaunchedEffect(isHolding, currentIndex, isPhoneUpright) {
        if (isHolding && isPhoneUpright && !showStepSuccess) {
            while (holdSeconds < FaceChallenge.HOLD_SECONDS) {
                delay(1_000L)
                holdSeconds++
            }
            // Challenge done
            if (currentIndex < challenges.lastIndex) {
                showStepSuccess = true
            } else {
                onDismissed()
            }
        } else {
            if (!showStepSuccess) holdSeconds = 0
        }
    }

    // Handle the success overlay purely based on its own state to avoid cancellation
    // if the user stops holding the face expression while the success message is showing.
    LaunchedEffect(showStepSuccess) {
        if (showStepSuccess) {
            delay(1500L) // Show "Success!" for 1.5s
            showStepSuccess = false
            currentIndex++
            holdSeconds = 0
        }
    }

    DisposableEffect(Unit) {
        onDispose { detector.close() }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // ── Camera preview ──────────────────────────────────────────────────
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                bindFaceCamera(ctx, previewView, lifecycleOwner, detector) { result ->
                    // Set isHolding if face matches AND is upright. 
                    // Any valid face result (even wrong expression) could technically reset the flashlight,
                    // but we only care about progress here: if they aren't progressing, turn on the light.
                    isHolding = isPhoneUpright &&
                        result != null &&
                        (currentChallenge?.detect?.invoke(result) == true)
                }
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // ── Soft Flashlight Light Panels ────────────────────────────────────
        // We draw solid white blocks over the top quarter and bottom quarter of the screen
        // instead of a full-screen overlay, so the centre UI remains perfectly visible
        // while maximizing illumination.
        if (flashlightAlpha > 0f) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val blockHeight = size.height / 4f
                // Top quarter
                drawRect(
                    color = Color.White.copy(alpha = flashlightAlpha),
                    topLeft = Offset.Zero,
                    size = Size(size.width, blockHeight)
                )
                // Bottom quarter
                drawRect(
                    color = Color.White.copy(alpha = flashlightAlpha),
                    topLeft = Offset(0f, size.height - blockHeight),
                    size = Size(size.width, blockHeight)
                )
            }
        }

        // ── Dark scrim for the centre section so text is legible ────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f)) // Base dark scrim overlay
        )

        // ── UI overlay ──────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            // Step indicator (top)
            StepIndicator(
                total     = challenges.size,
                current   = currentIndex,
                ringColor = ringColor
            )

            // Centre block: animated emoji + ring + copy
            if (challenge != null) {
                ChallengeCard(
                    challenge    = challenge,
                    progress     = progress,
                    ringColor    = ringColor,
                    holdSeconds  = holdSeconds
                )
            }

            // Bottom hint
            if (!isPhoneUpright) {
                // "Sit up!" banner — phone is lying flat
                Text(
                    text      = stringResource(R.string.face_sit_up),
                    color     = Color(0xFFFF9800),
                    fontSize  = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text  = if (isHolding) stringResource(R.string.face_hold_it) else stringResource(R.string.face_position_face),
                    color = if (isHolding) ringColor else Color.White.copy(alpha = 0.7f),
                    fontSize = 22.sp,
                    fontWeight = if (isHolding) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center
                )
            }
            
            // Manual Fallback Button
            if (fallbackMethod == "MATH") {
                androidx.compose.material3.TextButton(
                    onClick = onFallbackTriggered,
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text(stringResource(R.string.smile_fallback_math), color = Color.White.copy(alpha = 0.8f))
                }
            } else {
                Spacer(modifier = Modifier.height(48.dp)) // Maintain spacing
            }
        }

        // ── Success Overlay ─────────────────────────────────────────────────
        androidx.compose.animation.AnimatedVisibility(
            visible = showStepSuccess,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = stringResource(R.string.content_desc_success),
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(120.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.smile_step_complete),
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Sub-composables
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StepIndicator(total: Int, current: Int, ringColor: Color) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(total) { i ->
            val done   = i < current
            val active = i == current
            Box(
                modifier = Modifier
                    .size(if (active) 14.dp else 10.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            done   -> Color(0xFF4CAF50)                    // green checkmark dot
                            active -> ringColor
                            else   -> Color.White.copy(alpha = 0.35f)
                        }
                    )
            )
        }
    }
}

@Composable
private fun ChallengeCard(
    challenge:   FaceChallenge,
    progress:    Float,
    ringColor:   Color,
    holdSeconds: Int
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {

        // Animated emoji transitions when challenge changes
        AnimatedContent(
            targetState = challenge.emoji,
            transitionSpec = {
                (fadeIn() + scaleIn(initialScale = 0.6f))
                    .togetherWith(fadeOut() + scaleOut(targetScale = 0.6f))
            },
            label = "emoji"
        ) { emoji ->
            Text(text = emoji, fontSize = 110.sp, textAlign = TextAlign.Center)
        }

        Spacer(Modifier.height(16.dp))

        // Progress ring
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(200.dp)) {
                val strokeW = 14.dp.toPx()
                val inset   = strokeW / 2
                val topLeft = Offset(inset, inset)
                val arcSize = Size(size.width - strokeW, size.height - strokeW)
                val stroke  = Stroke(width = strokeW, cap = StrokeCap.Round)

                // Track
                drawArc(
                    color      = Color.White.copy(alpha = 0.18f),
                    startAngle = -90f, sweepAngle = 360f,
                    useCenter  = false, topLeft = topLeft, size = arcSize, style = stroke
                )
                // Fill
                if (progress > 0f) {
                    drawArc(
                        color      = ringColor,
                        startAngle = -90f, sweepAngle = 360f * progress.coerceIn(0f, 1f),
                        useCenter  = false, topLeft = topLeft, size = arcSize, style = stroke
                    )
                }
            }

            // Countdown inside ring
            Text(
                text       = if (progress > 0f)
                    "${FaceChallenge.HOLD_SECONDS - holdSeconds}"
                else "?",
                color      = if (progress > 0f) ringColor else Color.White.copy(alpha = 0.4f),
                fontSize   = 64.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(20.dp))

        // Title + hint
        AnimatedContent(
            targetState = challenge,
            transitionSpec = {
                (fadeIn()).togetherWith(fadeOut())
            },
            label = "challengeText"
        ) { ch ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text       = when (ch) {
                        FaceChallenge.Smile -> stringResource(R.string.face_prompt_smile)
                        FaceChallenge.BigSmile -> stringResource(R.string.face_prompt_biggest_smile)
                        FaceChallenge.WinkLeft -> stringResource(R.string.face_prompt_wink_left)
                        FaceChallenge.WinkRight -> stringResource(R.string.face_prompt_wink_right)
                        FaceChallenge.TiltHead -> stringResource(R.string.face_prompt_tilt)
                    },
                    color      = Color.White,
                    fontSize   = 32.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign  = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text      = when (ch) {
                        FaceChallenge.Smile -> stringResource(R.string.face_hint_smile)
                        FaceChallenge.BigSmile -> stringResource(R.string.face_hint_biggest_smile)
                        FaceChallenge.WinkLeft -> stringResource(R.string.face_hint_wink_left)
                        FaceChallenge.WinkRight -> stringResource(R.string.face_hint_wink_right)
                        FaceChallenge.TiltHead -> stringResource(R.string.face_hint_tilt)
                    },
                    color     = Color.White.copy(alpha = 0.75f),
                    fontSize  = 15.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Camera binding
// ─────────────────────────────────────────────────────────────────────────────

private val cameraExecutor = Executors.newSingleThreadExecutor()

private fun bindFaceCamera(
    context:        Context,
    previewView:    PreviewView,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    detector:       SmileDetector,
    onFaceResult:   (FaceResult?) -> Unit
) {
    val future = ProcessCameraProvider.getInstance(context)
    future.addListener({
        val provider = future.get()

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
            .build()

        analysis.setAnalyzer(cameraExecutor) { imageProxy ->
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val result = detector.getFaceResult(imageProxy)
                    Log.d("FaceChallenge", "smile=${result?.smilingProbability} " +
                          "leftEye=${result?.leftEyeOpenProbability} " +
                          "rightEye=${result?.rightEyeOpenProbability} " +
                          "tilt=${result?.headEulerAngleZ}")
                    withContext(Dispatchers.Main) { onFaceResult(result) }
                } catch (e: Exception) {
                    Log.e("FaceChallenge", "Detection error", e)
                    withContext(Dispatchers.Main) { onFaceResult(null) }
                }
            }
        }

        try {
            provider.unbindAll()
            provider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_FRONT_CAMERA,
                preview,
                analysis
            )
        } catch (e: Exception) {
            Log.e("FaceChallenge", "Camera bind failed", e)
        }
    }, ContextCompat.getMainExecutor(context))
}
