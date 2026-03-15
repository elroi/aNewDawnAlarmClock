package com.elroi.lemurloop.ui.activity

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.elroi.lemurloop.R
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.elroi.lemurloop.service.AlarmIntentExtras
import com.elroi.lemurloop.service.AlarmService
import com.elroi.lemurloop.ui.theme.LemurLoopTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@AndroidEntryPoint
class AlarmActivity : ComponentActivity() {

    @javax.inject.Inject
    lateinit var mathProblemGenerator: com.elroi.lemurloop.domain.dismissal.MathProblemGenerator
    
    @javax.inject.Inject
    lateinit var alarmRepository: com.elroi.lemurloop.domain.repository.AlarmRepository

    @javax.inject.Inject
    lateinit var lightSensorManager: com.elroi.lemurloop.domain.manager.LightSensorManager

    @javax.inject.Inject
    lateinit var diagnosticLogger: com.elroi.lemurloop.domain.manager.DiagnosticLogger

    // Tracks whether CAMERA permission was granted after requesting it
    private var cameraPermissionGranted = false
    private val requestCameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        cameraPermissionGranted = granted
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        diagnosticLogger.debug("AlarmActivity", "lifecycle: onCreate")
        turnScreenOnAndKeyguardOff()
        handleIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        diagnosticLogger.debug("AlarmActivity", "lifecycle: onStart")
        turnScreenOnAndKeyguardOff()
    }

    override fun onResume() {
        super.onResume()
        diagnosticLogger.debug("AlarmActivity", "lifecycle: onResume")
    }

    override fun onPause() {
        super.onPause()
        diagnosticLogger.debug("AlarmActivity", "lifecycle: onPause")
    }

    override fun onStop() {
        super.onStop()
        diagnosticLogger.debug("AlarmActivity", "lifecycle: onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        diagnosticLogger.debug("AlarmActivity", "lifecycle: onDestroy")
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        diagnosticLogger.debug("AlarmActivity", "lifecycle: onNewIntent")
        setIntent(intent)
        turnScreenOnAndKeyguardOff()
        handleIntent(intent)
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // Called when user presses Home or Recents to leave the activity
        diagnosticLogger.warn("AlarmActivity", "User left the alarm screen - focus lost")
    }

    private fun handleIntent(intent: Intent) {
        diagnosticLogger.debug("AlarmActivity", "handleIntent: action=${intent.action}, flags=${intent.flags}")
        val alarmId = intent.getStringExtra(AlarmIntentExtras.EXTRA_ALARM_ID)
        val alarmLabel = intent.getStringExtra(AlarmIntentExtras.EXTRA_ALARM_LABEL)
        val mathDifficulty = intent.getIntExtra(AlarmIntentExtras.EXTRA_MATH_DIFFICULTY, 0)
        val mathProblemCount = intent.getIntExtra(AlarmIntentExtras.EXTRA_MATH_PROBLEM_COUNT, 1)
        val mathGradualDifficulty =
            intent.getBooleanExtra(AlarmIntentExtras.EXTRA_MATH_GRADUAL_DIFFICULTY, false)
        val snoozeDuration = intent.getIntExtra(AlarmIntentExtras.EXTRA_SNOOZE_DURATION, 5)
        val snoozeCount = intent.getIntExtra(AlarmIntentExtras.EXTRA_SNOOZE_COUNT, 0)
        val smileToDismiss = intent.getBooleanExtra(AlarmIntentExtras.EXTRA_SMILE_TO_DISMISS, false)
        val smileFallbackMethod =
            intent.getStringExtra(AlarmIntentExtras.EXTRA_SMILE_FALLBACK_METHOD) ?: "MATH"
        val isPreview      = intent.getBooleanExtra("IS_PREVIEW", false)
        val isEvasiveSnooze =
            intent.getBooleanExtra(AlarmIntentExtras.EXTRA_IS_EVASIVE_SNOOZE, false)
        val evasiveSnoozesBeforeMoving =
            intent.getIntExtra(AlarmIntentExtras.EXTRA_EVASIVE_SNOOZES_BEFORE_MOVING, 0)
        val isSnoozeEnabled =
            intent.getBooleanExtra(AlarmIntentExtras.EXTRA_IS_SNOOZE_ENABLED, true)
        val isBriefingEnabled =
            intent.getBooleanExtra(AlarmIntentExtras.EXTRA_BRIEFING_ENABLED, true)
        val isTtsEnabled = intent.getBooleanExtra(AlarmIntentExtras.EXTRA_TTS_ENABLED, true)
        val briefingTimeout =
            intent.getIntExtra(AlarmIntentExtras.EXTRA_BRIEFING_TIMEOUT, 30)
        
        // Pre-request CAMERA permission so SmileDismissScreen can use it immediately
        cameraPermissionGranted = ContextCompat.checkSelfPermission(
            this, android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (smileToDismiss && !cameraPermissionGranted) {
            requestCameraPermission.launch(android.Manifest.permission.CAMERA)
        }
        
        val initialSystemBrightness = try {
            android.provider.Settings.System.getInt(contentResolver, android.provider.Settings.System.SCREEN_BRIGHTNESS) / 255f
        } catch (e: Exception) {
            -1f // -1 means use system default
        }
        
        setContent {
            LemurLoopTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var showMathDialog by remember { mutableStateOf(false) }
                    var showSmileScreen by remember { mutableStateOf(false) }
                    var successMessage by remember { mutableStateOf<String?>(null) }
                    var snoozedUntil by remember { mutableStateOf<LocalTime?>(null) }
                    var isSmileFlashlightActive by remember { mutableStateOf(false) }
                    
                    val briefingState by com.elroi.lemurloop.domain.manager.BriefingStateManager.briefingState.collectAsState()
                    var showBriefingScreen by remember { mutableStateOf(false) }
                    
                    val briefingText = (briefingState as? com.elroi.lemurloop.domain.manager.BriefingState.Ready)?.text
                        ?: (briefingState as? com.elroi.lemurloop.domain.manager.BriefingState.Completed)?.let { "" } // Just fallback if needed, shouldn't happen while showing
                    val generatingMessage = (briefingState as? com.elroi.lemurloop.domain.manager.BriefingState.Generating)?.message
                    val hasBriefingStarted = briefingState is com.elroi.lemurloop.domain.manager.BriefingState.Ready || briefingState is com.elroi.lemurloop.domain.manager.BriefingState.Completed
                    
                    var wasBriefingReady by remember { mutableStateOf(false) }
                    if (hasBriefingStarted) wasBriefingReady = true
                    var isBriefingPaused by remember { mutableStateOf(false) }
                    var hasPausedOnce by remember { mutableStateOf(false) }

                    LaunchedEffect(isBriefingPaused) {
                        if (isBriefingPaused) {
                            hasPausedOnce = true
                            startService(Intent(this@AlarmActivity, com.elroi.lemurloop.service.AlarmService::class.java).apply {
                                action = com.elroi.lemurloop.service.AlarmService.ACTION_PAUSE_TTS
                            })
                        } else if (hasPausedOnce) {
                            startService(Intent(this@AlarmActivity, com.elroi.lemurloop.service.AlarmService::class.java).apply {
                                action = com.elroi.lemurloop.service.AlarmService.ACTION_RESUME_TTS
                            })
                        }
                    }

                    LaunchedEffect(briefingState, isBriefingPaused) {
                        if (briefingState is com.elroi.lemurloop.domain.manager.BriefingState.Ready) {
                            if (briefingText.isNullOrBlank()) {
                                // If empty, TTS is disabled or failed, just kill activity
                                finish()
                            }
                        } else if (briefingState is com.elroi.lemurloop.domain.manager.BriefingState.Completed && !isBriefingPaused) {
                            // TTS has finished speaking or timeout reached
                            // Wait 2 seconds before closing
                            kotlinx.coroutines.delay(2000L)
                            startService(Intent(this@AlarmActivity, com.elroi.lemurloop.service.AlarmService::class.java).apply {
                                action = com.elroi.lemurloop.service.AlarmService.ACTION_STOP_TTS
                            })
                            finish()
                        }
 else if (briefingState is com.elroi.lemurloop.domain.manager.BriefingState.Idle && wasBriefingReady && !isBriefingPaused) {
                            // The associated service was stopped abruptly 
                            finish()
                        }
                    }
                    var isGentleWake by remember { mutableStateOf(false) }
                    var crescendoMins by remember { mutableStateOf(0) }

                    val lux by lightSensorManager.lux.collectAsState()

                    LaunchedEffect(Unit) {
                        lightSensorManager.startListening()
                    }

                    DisposableEffect(Unit) {
                        onDispose {
                            lightSensorManager.stopListening()
                        }
                    }


                    LaunchedEffect(alarmId) {
                        if (alarmId != null) {
                            val alarm = alarmRepository.getAlarmById(alarmId)
                            if (alarm != null) {
                                isGentleWake = alarm.isGentleWake
                                crescendoMins = alarm.crescendoDurationMinutes
                            }
                        }
                    }

                    // Decide which challenge to show first on dismiss
                    val onDismissPressed: () -> Unit = {
                        if (!isPreview) {
                            startService(android.content.Intent(this@AlarmActivity, com.elroi.lemurloop.service.AlarmService::class.java).apply {
                                action = com.elroi.lemurloop.service.AlarmService.ACTION_MUTE_RINGTONE
                            })
                        }
                        
                        if (smileToDismiss && cameraPermissionGranted) {
                            showSmileScreen = true
                        } else if (mathDifficulty > 0) {
                            showMathDialog = true
                        } else {
                            triggerDismissLogic(isPreview, isBriefingEnabled, { showBriefingScreen = true })
                        }
                    }
                    
                    val allowSnooze = snoozeDuration > 0 && isSnoozeEnabled && !isPreview

                    val isDismissing = showSmileScreen || showMathDialog || successMessage != null || snoozedUntil != null
                    
                    BackHandler(enabled = !isDismissing && allowSnooze) {
                        snoozeAlarm(alarmId, snoozeDuration) 
                        snoozedUntil = LocalTime.now().plusMinutes(snoozeDuration.toLong())
                    }

                    val initialBrightness = remember { initialSystemBrightness }

                    val isChallengeActive = showSmileScreen || showMathDialog || successMessage != null

                    // --- ADAPTIVE BRIGHTNESS logic (Refined with smoothing) ---
                    val targetBrightness by remember(isGentleWake, crescendoMins, isChallengeActive, snoozedUntil, showBriefingScreen, lux, isSmileFlashlightActive) {
                        derivedStateOf {
                            when {
                                // 0. Smile flashlight takes priority
                                isSmileFlashlightActive -> 1.0f
                                // 1. If we are showing the briefing, ensure a minimum readable brightness
                                showBriefingScreen -> {
                                    if (lux > 50f) 1.0f else maxOf(0.6f, (lux / 50f))
                                }
                                // 2. If we are in the middle of a challenge, keep it bright
                                isChallengeActive -> {
                                    if (lux > 50f) 1.0f else 0.8f
                                }
                                // 3. Snoozed state (revert to system default)
                                snoozedUntil != null -> -1f
                                // 4. Gentle Wake crescendo
                                isGentleWake && crescendoMins > 0 -> {
                                    -2f // Special marker for crescendo loop
                                }
                                // 5. Default firing state (High brightness, adapted to room)
                                else -> {
                                    if (lux > 50f) 1.0f else maxOf(0.8f, initialBrightness)
                                }
                            }
                        }
                    }

                    // Smooth the fluctuations from the light sensor
                    // We use a relatively slow animation (1500ms) to ensure it's not "pulsating"
                    val animationTarget = remember(targetBrightness, initialBrightness) {
                        if (targetBrightness == -2f) -2f // Placeholder for crescendo
                        else if (targetBrightness == -1f) (if (initialBrightness > 0) initialBrightness else 0.5f)
                        else targetBrightness
                    }

                    var manualBrightness by remember { mutableFloatStateOf(-1f) }
                    val animatedBrightness by androidx.compose.animation.core.animateFloatAsState(
                        targetValue = if (animationTarget == -2f) manualBrightness else animationTarget,
                        animationSpec = androidx.compose.animation.core.tween(durationMillis = 1500),
                        label = "brightnessAnimation"
                    )

                    // Actual window update effect
                    LaunchedEffect(animatedBrightness, targetBrightness) {
                        // When targeting system default (-1f), we apply it as soon as the animation is close
                        val toApply = if (targetBrightness == -1f && Math.abs(animatedBrightness - animationTarget) < 0.05f) {
                            -1f 
                        } else {
                            animatedBrightness
                        }

                        if (toApply > 0f || toApply == -1f) {
                            val activity = this@AlarmActivity
                            activity.window?.attributes = activity.window.attributes?.apply {
                                screenBrightness = toApply
                            }
                        }
                    }

                    // Handle the crescendo loop separately to update 'manualBrightness'
                    LaunchedEffect(isGentleWake, crescendoMins, isChallengeActive, showBriefingScreen, snoozedUntil) {
                        if (isGentleWake && crescendoMins > 0 && !isChallengeActive && !showBriefingScreen && snoozedUntil == null) {
                            val durationMs = crescendoMins * 60_000L
                            val startTime = System.currentTimeMillis()
                            while (true) {
                                val elapsed = System.currentTimeMillis() - startTime
                                val fraction = if (elapsed >= durationMs) 1.0f else elapsed.toFloat() / durationMs.toFloat()
                                val startFloor = if (initialBrightness > 0) initialBrightness else 0.01f
                                val crescendoBase = startFloor + ((1.0f - startFloor) * fraction)
                                
                                // Factor in lux even during crescendo (don't go too dim in bright room)
                                manualBrightness = if (lux > 50f) 1.0f else maxOf(crescendoBase, (lux / 100f))
                                
                                if (elapsed >= durationMs) break
                                kotlinx.coroutines.delay(1000)
                            }
                        }
                    }
                    
                    val funnySnoozeLabels = remember {
                        listOf(
                            "5 more mins?", "Catch me!", "Too slow!", "Nope!", 
                            "Sleep is better", "Try again", "Zzz...", "Not today!",
                            "You wish!", "Run, button, run!"
                        )
                    }
                    val currentSnoozeLabel = remember(snoozeCount) {
                        if (isEvasiveSnooze && snoozeCount > evasiveSnoozesBeforeMoving) {
                            "💤 ${funnySnoozeLabels.random()}"
                        } else {
                            "💤 Snooze"
                        }
                    }

                    val successMessages = remember {
                        listOf(
                            "Great job! Now solve this.",
                            "Victory! One more hurdle.",
                            "You're doing great! Keep going.",
                            "Success! Wakey wakey!",
                            "Awesome! Almost there."
                        )
                    }
                    val finalSuccessMessages = remember {
                        listOf(
                            "You're awake! Have a great day.",
                            "Challenge completed! Time to shine.",
                            "You did it! Morning is yours.",
                            "Bravo! Full awake mode engaged.",
                            "Success! No more sleeping today!"
                        )
                    }
                    
                    if (showBriefingScreen) {
                        BriefingScreen(
                            briefingText = briefingText,
                            generatingMessage = generatingMessage,
                            hasStarted = hasBriefingStarted,
                            isCompleted = briefingState is com.elroi.lemurloop.domain.manager.BriefingState.Completed,
                            isTtsEnabled = isTtsEnabled,
                            isPaused = isBriefingPaused,
                            timeoutSeconds = briefingTimeout,
                            onPauseChange = { isBriefingPaused = it },
                            onStopTts = {
                                startService(Intent(this@AlarmActivity, com.elroi.lemurloop.service.AlarmService::class.java).apply {
                                    action = com.elroi.lemurloop.service.AlarmService.ACTION_STOP_TTS
                                })
                                finish()
                            }
                        )
                    } else {
                        AlarmFiringScreen(
                            label = alarmLabel,
                            isGentleWake = isGentleWake,
                            crescendoMins = crescendoMins,
                            isDismissing = isDismissing,
                            snoozeCount = snoozeCount,
                            snoozeDuration = snoozeDuration,
                            isEvasiveSnooze = isEvasiveSnooze,
                            evasiveSnoozesBeforeMoving = evasiveSnoozesBeforeMoving,
                            snoozeLabel = currentSnoozeLabel,
                            allowSnooze = allowSnooze,
                            onDismiss = onDismissPressed,
                            onSnooze = { 
                                if (isPreview) {
                                    finish() 
                                } else {
                                    snoozeAlarm(alarmId, snoozeDuration) 
                                    snoozedUntil = LocalTime.now().plusMinutes(snoozeDuration.toLong())
                                }
                            }
                        )
                    }

                    if (snoozedUntil != null) {
                        SnoozedScreen(
                            snoozedUntil = snoozedUntil!!,
                            onDismiss = { finish() }
                        )
                    }
                    
                    if (showSmileScreen) {
                        SmileDismissScreen(
                            lux = lux,
                            fallbackMethod = smileFallbackMethod,
                            onFlashlightStateChanged = { isSmileFlashlightActive = it },
                            onFallbackTriggered = {
                                showSmileScreen = false
                                if (smileFallbackMethod == "MATH") {
                                    showMathDialog = true
                                } else {
                                    triggerDismissLogic(isPreview, isBriefingEnabled, { showBriefingScreen = true })
                                }
                            },
                            onDismissed = { 
                                showSmileScreen = false
                                // Chain to Math if enabled, otherwise dismiss
                                if (mathDifficulty > 0) {
                                    successMessage = successMessages.random()
                                } else {
                                    successMessage = finalSuccessMessages.random()
                                }
                            }
                        )
                    }

                    if (showMathDialog) {
                        MathChallengeDialog(
                            difficulty = mathDifficulty,
                            problemCount = mathProblemCount,
                            gradualIncrease = mathGradualDifficulty,
                            generator = mathProblemGenerator,
                            onSuccess = {
                                showMathDialog = false
                                successMessage = finalSuccessMessages.random()
                            },
                            onFailure = { /* keep dialog open */ }
                        )
                    }

                    successMessage?.let { msg ->
                        ChallengeSuccessScreen(
                            message = msg,
                            onContinue = {
                                val wasFinalMessage = finalSuccessMessages.contains(msg)
                                successMessage = null // Clear the message state FIRST
                                
                                if (mathDifficulty > 0 && !wasFinalMessage) {
                                    // If math is enabled and we haven't shown it yet 
                                    // (indicated by a non-final message from Smile screen)
                                    showMathDialog = true
                                } else {
                                    // Proceed to dismissal/briefing
                                    triggerDismissLogic(isPreview, isBriefingEnabled, { showBriefingScreen = true })
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    private fun triggerDismissLogic(isPreview: Boolean, isBriefingEnabled: Boolean, showBriefingScreen: () -> Unit) {
        if (!isPreview) {
            startService(Intent(this, com.elroi.lemurloop.service.AlarmService::class.java).apply {
                action = com.elroi.lemurloop.service.AlarmService.ACTION_DISMISS
            })
            // Switch UI immediately to Briefing spinner ONLY if enabled
            if (isBriefingEnabled) {
                showBriefingScreen()
            } else {
                finish()
            }
        } else {
            finish()
        }
    }

    
    private fun snoozeAlarm(originalAlarmId: String?, durationMinutes: Int) {
        startService(Intent(this, AlarmService::class.java).apply {
            action = AlarmService.ACTION_SNOOZE
            putExtra(AlarmIntentExtras.EXTRA_ALARM_ID, originalAlarmId)
            putExtra(AlarmIntentExtras.EXTRA_SNOOZE_DURATION, durationMinutes)
        })
        // Activity stays open to show SnoozedScreen
    }

    private fun turnScreenOnAndKeyguardOff() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            (getSystemService(Context.KEYGUARD_SERVICE) as? android.app.KeyguardManager)?.requestDismissKeyguard(this, null)
        }
        // Apply legacy flags as a fallback for all versions, as some OEMs ignore the newer APIs
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )
    }
}

@Composable
fun MathChallengeDialog(
    difficulty: Int,
    problemCount: Int,
    gradualIncrease: Boolean,
    generator: com.elroi.lemurloop.domain.dismissal.MathProblemGenerator,
    onSuccess: () -> Unit,
    onFailure: () -> Unit
) {
    var solvedCount by remember { mutableIntStateOf(0) }
    
    val currentDifficulty = remember(solvedCount, difficulty, problemCount, gradualIncrease) {
        if (!gradualIncrease || problemCount <= 1 || difficulty <= 1) {
            difficulty
        } else {
            val step = (difficulty - 1).toFloat() / (problemCount - 1).toFloat()
            val computedDifficulty = 1 + (step * solvedCount).toInt()
            computedDifficulty.coerceIn(1, difficulty)
        }
    }

    var problem by remember(currentDifficulty, solvedCount) { mutableStateOf(generator.generateProblem(currentDifficulty.coerceAtLeast(1))) }
    var answer by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    val offsetX = remember { androidx.compose.animation.core.Animatable(0f) }

    AlertDialog(
        onDismissRequest = { /* Prevent dismiss */ },
        title = { 
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.alarm_activity_math_challenge), fontWeight = FontWeight.Bold)
                if (problemCount > 1) {
                    Text("${solvedCount + 1} / $problemCount", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().offset(x = offsetX.value.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = problem.question, 
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
                OutlinedTextField(
                    value = answer,
                    onValueChange = { answer = it; isError = false },
                    isError = isError,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    ),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 24.sp, 
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.fillMaxWidth(0.8f)
                )
                if (isError) {
                    Text(
                        stringResource(R.string.alarm_activity_incorrect_try_again), 
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (answer.toIntOrNull() == problem.answer) {
                        isError = false
                        solvedCount++
                        answer = ""
                        if (solvedCount >= problemCount) {
                            scope.launch {
                                kotlinx.coroutines.delay(100)
                                onSuccess()
                            }
                        }
                    } else {
                        isError = true
                        onFailure()
                        answer = ""
                        scope.launch {
                            for (i in 0..5) {
                                offsetX.animateTo(
                                    targetValue = if (i % 2 == 0) 10f else -10f,
                                    animationSpec = androidx.compose.animation.core.tween(50)
                                )
                            }
                            offsetX.animateTo(0f, androidx.compose.animation.core.tween(50))
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.alarm_activity_submit_answer), modifier = Modifier.padding(vertical = 4.dp), fontSize = 16.sp)
            }
        },
        dismissButton = null
    )
}

@Composable
fun AlarmFiringScreen(
    label: String?,
    isGentleWake: Boolean,
    crescendoMins: Int,
    isDismissing: Boolean,
    snoozeCount: Int,
    snoozeDuration: Int,
    isEvasiveSnooze: Boolean,
    evasiveSnoozesBeforeMoving: Int,
    snoozeLabel: String,
    allowSnooze: Boolean,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? android.app.Activity
    
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val buttonWidthDp = 120
    val buttonHeightDp = 60
    
    val isEvasiveActive = isEvasiveSnooze && !isDismissing && snoozeCount > evasiveSnoozesBeforeMoving && allowSnooze
    
    val density = androidx.compose.ui.platform.LocalDensity.current
    var posX by remember { mutableFloatStateOf(-1f) }
    var posY by remember { mutableFloatStateOf(-1f) }
    var dirX by remember { mutableFloatStateOf(1f) }
    var dirY by remember { mutableFloatStateOf(1f) }

    val baseSpeedDp = 200f
    val addedSpeedDp = maxOf(0, snoozeCount - evasiveSnoozesBeforeMoving) * 50f
    val speedPxPerSec = remember(snoozeCount, density) { with(density) { (baseSpeedDp + addedSpeedDp).dp.toPx() } }
    
    val buttonWidthPx = with(density) { buttonWidthDp.dp.toPx() }
    val buttonHeightPx = with(density) { buttonHeightDp.dp.toPx() }
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    
    val maxX = maxOf(0f, screenWidthPx - buttonWidthPx)
    val maxY = maxOf(0f, screenHeightPx - buttonHeightPx)

    LaunchedEffect(isEvasiveActive, maxX, maxY) {
        if (isEvasiveActive && posX < 0f && maxX > 0f && maxY > 0f) {
            posX = kotlin.random.Random.nextFloat() * maxX
            posY = kotlin.random.Random.nextFloat() * maxY
            val angle = kotlin.random.Random.nextFloat() * 2 * Math.PI.toFloat()
            dirX = kotlin.math.cos(angle)
            dirY = kotlin.math.sin(angle)
        }
    }

    LaunchedEffect(isEvasiveActive, speedPxPerSec, maxX, maxY) {
        if (!isEvasiveActive || maxX <= 0f || maxY <= 0f) return@LaunchedEffect
        var lastTime = androidx.compose.runtime.withFrameNanos { it }
        while (isActive) {
            val currentTime = androidx.compose.runtime.withFrameNanos { it }
            val dt = (currentTime - lastTime) / 1_000_000_000f
            lastTime = currentTime
            
            var newX = posX + dirX * speedPxPerSec * dt
            var newY = posY + dirY * speedPxPerSec * dt
            
            if (newX < 0f) {
                newX = 0f
                dirX = kotlin.math.abs(dirX)
            } else if (newX > maxX) {
                newX = maxX
                dirX = -kotlin.math.abs(dirX)
            }
            
            if (newY < 0f) {
                newY = 0f
                dirY = kotlin.math.abs(dirY)
            } else if (newY > maxY) {
                newY = maxY
                dirY = -kotlin.math.abs(dirY)
            }
            
            posX = newX
            posY = newY
        }
    }

    val snoozeOffsetX = if (posX < 0f) 0.dp else with(density) { posX.toDp() }
    val snoozeOffsetY = if (posY < 0f) 0.dp else with(density) { posY.toDp() }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // BUG-12 FIX: update clock every second instead of using a static snapshot
            var currentTime by remember { mutableStateOf(LocalTime.now()) }
            LaunchedEffect(Unit) {
                while (true) {
                    currentTime = LocalTime.now()
                    kotlinx.coroutines.delay(1000L)
                }
            }

            Row(verticalAlignment = Alignment.Bottom) {
                val context = androidx.compose.ui.platform.LocalContext.current
                val is24Hour = android.text.format.DateFormat.is24HourFormat(context)
                if (is24Hour) {
                    Text(
                        text = currentTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = currentTime.format(DateTimeFormatter.ofPattern("hh:mm:ss")),
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = currentTime.format(DateTimeFormatter.ofPattern(" a")),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = if (label.isNullOrBlank()) "Alarm" else label,
                fontSize = 32.sp
            )

            Spacer(modifier = Modifier.height(64.dp))

            if (!isEvasiveActive && allowSnooze) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = onSnooze,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    ) {
                        Text(
                            snoozeLabel, 
                            fontSize = 22.sp, 
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                    
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.weight(1f).padding(start = 8.dp)
                    ) {
                        Text(
                            "☀️ Dismiss", 
                            fontSize = 22.sp, 
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                }
            } else {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                ) {
                    Text(
                        "☀️ Dismiss", 
                        fontSize = 22.sp, 
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            }
        }

        if (isEvasiveActive && allowSnooze) {
            Button(
                onClick = onSnooze,
                modifier = Modifier
                    .offset(x = snoozeOffsetX, y = snoozeOffsetY)
                    .padding(8.dp)
            ) {
                Text(
                    snoozeLabel, 
                    fontSize = 20.sp, 
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp)
                )
            }
        }
    }
}

@Composable
fun ChallengeSuccessScreen(
    message: String,
    onContinue: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var timeLeft by remember { mutableIntStateOf(10) }

    LaunchedEffect(Unit) {
        while (timeLeft > 0) {
            kotlinx.coroutines.delay(1000)
            timeLeft--
        }
        onContinue()
    }
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Filled.CheckCircle,
                contentDescription = stringResource(R.string.content_desc_success),
                modifier = Modifier.size(120.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = message,
                fontSize = 28.sp,
                fontWeight = FontWeight.Medium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(64.dp))

            Button(
                onClick = {
                    scope.launch {
                        kotlinx.coroutines.delay(100)
                        onContinue()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "Continue (${timeLeft}s)", 
                    fontSize = 24.sp, 
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun BriefingScreen(
    briefingText: String?,
    generatingMessage: String? = null,
    hasStarted: Boolean,
    isCompleted: Boolean,
    isTtsEnabled: Boolean,
    isPaused: Boolean,
    timeoutSeconds: Int,
    onPauseChange: (Boolean) -> Unit,
    onStopTts: () -> Unit
) {
    var closingSeconds by remember { mutableIntStateOf(-1) }

    LaunchedEffect(isCompleted, isPaused, hasStarted, isTtsEnabled) {
        if (isCompleted && !isPaused) {
            closingSeconds = 2
            while (closingSeconds > 0) {
                kotlinx.coroutines.delay(1000L)
                if (!isPaused) {
                    closingSeconds--
                }
            }
        } else if (hasStarted && !isTtsEnabled && !isCompleted && !isPaused) {
            // Show the full countdown if TTS is disabled
            closingSeconds = timeoutSeconds
            while (closingSeconds > 0 && !isCompleted) {
                kotlinx.coroutines.delay(1000L)
                if (!isPaused) {
                    closingSeconds--
                }
            }
        } else if (!isCompleted) {
            closingSeconds = -1
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(top = 48.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        if (!hasStarted) {
            val displayMessage = generatingMessage ?: "Initializing brain..."
            
            androidx.compose.material3.CircularProgressIndicator(
                modifier = Modifier.size(64.dp),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = displayMessage,
                style = MaterialTheme.typography.titleLarge,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                fontWeight = FontWeight.Light,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )
            
            // Add a mini "sub-status" to make it look even more active
            var subStatusIdx by remember { mutableStateOf(0) }
            val subStatuses = listOf(
                "Loading personality modules...", 
                "Squeezing pixels...", 
                "Calibrating wit...", 
                "Brewing digital coffee..."
            )
            LaunchedEffect(Unit) {
                while(true) {
                    kotlinx.coroutines.delay(2000L)
                    subStatusIdx = (subStatusIdx + 1) % subStatuses.size
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subStatuses[subStatusIdx],
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
        } else {
            val listState = androidx.compose.foundation.lazy.rememberLazyListState()
            
            // Elegant scrolling text area for the briefing
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .weight(1f, fill = true)
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                androidx.compose.foundation.lazy.LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp, top = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val paragraphs = briefingText?.split(Regex("\n+"))?.filter { it.isNotBlank() } ?: emptyList()
                    items(paragraphs.size) { index ->
                        Text(
                            text = paragraphs[index].trim(),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 20.sp,
                                lineHeight = 30.sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Start,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }

                // Vertical scroll indicator (Refined for better visibility)
                androidx.compose.animation.AnimatedVisibility(
                    visible = listState.canScrollForward || listState.canScrollBackward,
                    enter = androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.fadeOut(),
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 4.dp)
                ) {
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .width(8.dp)
                            .height(100.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                    )
                }
            }

            // Bottom controls: Countdown and Skip
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Surface(
                        onClick = { onPauseChange(!isPaused) },
                        color = if (isPaused) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        shape = androidx.compose.foundation.shape.CircleShape,
                        tonalElevation = 4.dp
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = if (isPaused) androidx.compose.material.icons.Icons.Filled.PlayArrow else androidx.compose.material.icons.Icons.Filled.Pause,
                                contentDescription = if (isPaused) "Resume" else "Pause",
                                modifier = Modifier.size(20.dp),
                                tint = if (isPaused) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            val statusText = when {
                                isPaused -> "Paused"
                                closingSeconds >= 0 -> "Closing in ${closingSeconds}s"
                                isTtsEnabled -> "Reading aloud..."
                                else -> "Reading..."
                            }
                            
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isPaused) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    Button(
                        onClick = onStopTts,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        shape = androidx.compose.foundation.shape.CircleShape
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Filled.Close,
                            contentDescription = stringResource(R.string.alarm_activity_dismiss),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.alarm_activity_dismiss))
                    }
                }
            }
        }
    }
}

@Composable
fun SnoozedScreen(
    snoozedUntil: LocalTime,
    onDismiss: () -> Unit
) {
    var timeLeft by remember { mutableStateOf(10) }
    
    LaunchedEffect(Unit) {
        while (timeLeft > 0) {
            kotlinx.coroutines.delay(1000)
            timeLeft--
        }
        onDismiss()
    }
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Filled.CheckCircle,
                contentDescription = stringResource(R.string.content_desc_snoozed),
                modifier = Modifier.size(120.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = stringResource(R.string.alarm_snoozed_title),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = stringResource(
                    R.string.alarm_snoozed_next_at,
                    snoozedUntil.format(DateTimeFormatter.ofPattern(if (android.text.format.DateFormat.is24HourFormat(androidx.compose.ui.platform.LocalContext.current)) "HH:mm" else "h:mm a"))
                ),
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(64.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
            ) {
                Text(stringResource(R.string.alarm_activity_dismiss_time, timeLeft.toString()), fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
