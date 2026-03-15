package com.elroi.lemurloop.ui.screen.alarm

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.VisibleForTesting
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import android.util.Log
import androidx.hilt.navigation.compose.hiltViewModel
import com.elroi.lemurloop.R
import com.elroi.lemurloop.domain.model.Alarm
import com.elroi.lemurloop.ui.components.BuddySelectionDialog
import com.elroi.lemurloop.ui.components.ImprovedDaySelector
import com.elroi.lemurloop.ui.components.VibrationPatternGallery
import com.elroi.lemurloop.ui.viewmodel.AlarmViewModel
import com.elroi.lemurloop.util.AlarmUtils
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch
import android.content.Intent
import android.net.Uri
@Immutable
data class Persona(val id: String, val title: String, val emoji: String)

val personas = listOf(
    Persona("COACH", "Drill Sergeant", "🪖"),
    Persona("COMEDIAN", "Sarcastic Friend", "🤡"),
    Persona("ZEN", "Zen Master", "🧘"),
    Persona("HYPEMAN", "Hype-Man", "🚀"),
    Persona("SURPRISE", "Surprise Me", "🎲")
)

private fun getPersonaLabelRes(id: String): Int = when (id) {
    "COACH" -> R.string.settings_persona_label_coach
    "COMEDIAN" -> R.string.settings_persona_label_comedian
    "ZEN" -> R.string.settings_persona_label_zen
    "HYPEMAN" -> R.string.settings_persona_label_hypeman
    "SURPRISE" -> R.string.settings_surprise_me
    else -> R.string.settings_persona_label_coach
}

@VisibleForTesting
internal fun previousWizardPage(currentPage: Int): Int =
    if (currentPage > 0) currentPage - 1 else 0

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmCreationWizard(
    onFinished: () -> Unit,
    onBack: () -> Unit,
    onSwitchToSimple: () -> Unit,
    viewModel: AlarmViewModel = hiltViewModel()
) {
    val coroutineScope = rememberCoroutineScope()
    val defaultSettings by viewModel.defaultAlarmSettings.collectAsState()
    val isCloudAiEnabled by viewModel.isCloudAiEnabled.collectAsState()
    var currentPage by remember { mutableIntStateOf(0) }
    val totalPages = 5

    // Initial state setup
    var selectedTime by remember { mutableStateOf<LocalTime>(LocalTime.now().withSecond(0).withNano(0)) }
    var selectedDays by remember { mutableStateOf<List<Int>>(listOf<Int>()) }
    var alarmLabel by remember { mutableStateOf<String>("") }
    
    var selectedPersona by remember { mutableStateOf<String>(defaultSettings.aiPersona) }
    var isBriefingEnabled by remember { mutableStateOf<Boolean>(defaultSettings.isBriefingEnabled) }
    var isTtsEnabled by remember { mutableStateOf<Boolean>(defaultSettings.isTtsEnabled) }
    var isSoundEnabled by remember { mutableStateOf<Boolean>(defaultSettings.isSoundEnabled) }
    var isVibrate by remember { mutableStateOf<Boolean>(defaultSettings.isVibrate) }
    var isGentleWake by remember { mutableStateOf<Boolean>(defaultSettings.isGentleWake) }
    var crescendoDurationMinutes by remember { mutableIntStateOf(defaultSettings.crescendoDurationMinutes) }
    var isSnoozeEnabled by remember { mutableStateOf<Boolean>(defaultSettings.isSnoozeEnabled) }
    var snoozeDurationMinutes by remember { mutableIntStateOf(defaultSettings.snoozeDurationMinutes) }
    var isSmoothFadeOut by remember { mutableStateOf<Boolean>(defaultSettings.isSmoothFadeOut) }
    var isEvasiveSnooze by remember { mutableStateOf<Boolean>(defaultSettings.isEvasiveSnooze) }
    var evasiveSnoozesBeforeMoving by remember { mutableIntStateOf(defaultSettings.evasiveSnoozesBeforeMoving) }
    var vibrationPattern by remember { mutableStateOf<String>(defaultSettings.vibrationPattern) }
    var vibrationCrescendoStartGapSeconds by remember { mutableIntStateOf(defaultSettings.vibrationCrescendoStartGapSeconds) }

    var selectedMathDifficulty by remember { mutableIntStateOf(defaultSettings.mathDifficulty) }
    var mathProblemCount by remember { mutableIntStateOf(defaultSettings.mathProblemCount) }
    var mathGraduallyIncreaseDifficulty by remember { mutableStateOf<Boolean>(defaultSettings.mathGraduallyIncreaseDifficulty) }
    var smileToDismiss by remember { mutableStateOf<Boolean>(defaultSettings.smileToDismiss) }
    var smileFallbackMethod by remember { mutableStateOf<String>(defaultSettings.smileFallbackMethod) }
    var isSmartWakeupEnabled by remember { mutableStateOf<Boolean>(defaultSettings.isSmartWakeupEnabled) }
    var wakeupCheckDelayMinutes by remember { mutableIntStateOf(defaultSettings.wakeupCheckDelayMinutes) }
    var wakeupCheckTimeoutSeconds by remember { mutableIntStateOf(defaultSettings.wakeupCheckTimeoutSeconds) }
    
    var buddyPhone by remember { mutableStateOf<String>("") }
    var buddyName by remember { mutableStateOf<String>("") }
    var buddyUserName by remember { mutableStateOf<String>(defaultSettings.briefingUserName) }
    var buddyMessage by remember { mutableStateOf<String>("") }
    var buddyAlertAfterMinutes by remember { mutableIntStateOf(5) }
    
    var showBuddyDialog by remember { mutableStateOf<Boolean>(false) }
    var showCloudAiSetupDialog by remember { mutableStateOf<Boolean>(false) }
    val globalBuddies by viewModel.globalBuddies.collectAsState()
    val confirmedNumbers by viewModel.confirmedBuddyNumbers.collectAsState()
    val pendingCodes by viewModel.pendingBuddyCodes.collectAsState()
    val geminiApiKey by viewModel.geminiApiKey.collectAsState()
    val defaultBuddyName = stringResource(R.string.wizard_buddy_default_name)

    // Sync persona and name with defaults when loaded
    LaunchedEffect(defaultSettings) {
        if (selectedPersona == "COACH" && defaultSettings.aiPersona != "COACH") {
            selectedPersona = defaultSettings.aiPersona
        }
        if (buddyUserName.isBlank() && defaultSettings.briefingUserName.isNotBlank()) {
            buddyUserName = defaultSettings.briefingUserName
        }
    }

    val timePickerState = rememberTimePickerState(
        initialHour = selectedTime.hour,
        initialMinute = selectedTime.minute,
        is24Hour = true
    )

    LaunchedEffect(timePickerState.hour, timePickerState.minute) {
        selectedTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
    }

    fun handleNext() {
        if (currentPage == 1) {
            viewModel.updateAlarmDefaults(defaultSettings.copy(aiPersona = selectedPersona))
        }

        if (currentPage < totalPages - 1) {
            currentPage++
        } else {
            if (buddyPhone.isNotBlank() && globalBuddies.none { it.endsWith("|$buddyPhone") }) {
                viewModel.addGlobalBuddy(buddyName.ifBlank { defaultBuddyName }, buddyPhone)
            }
            val newAlarm = Alarm(
                time = selectedTime,
                daysOfWeek = selectedDays,
                label = if (alarmLabel.isBlank()) null else alarmLabel,
                mathDifficulty = selectedMathDifficulty,
                mathProblemCount = mathProblemCount,
                mathGraduallyIncreaseDifficulty = mathGraduallyIncreaseDifficulty,
                buddyPhoneNumber = if (buddyPhone.isBlank()) null else buddyPhone,
                buddyName = if (buddyName.isBlank()) null else buddyName,
                buddyMessage = buddyMessage,
                buddyAlertDelayMinutes = buddyAlertAfterMinutes,
                smileToDismiss = smileToDismiss,
                smileFallbackMethod = smileFallbackMethod,
                isBriefingEnabled = isBriefingEnabled,
                isTtsEnabled = isTtsEnabled,
                isSoundEnabled = isSoundEnabled,
                isVibrate = isVibrate,
                isGentleWake = isGentleWake,
                crescendoDurationMinutes = crescendoDurationMinutes,
                isSnoozeEnabled = isSnoozeEnabled,
                snoozeDurationMinutes = snoozeDurationMinutes,
                isSmoothFadeOut = isSmoothFadeOut,
                isEvasiveSnooze = isEvasiveSnooze,
                evasiveSnoozesBeforeMoving = evasiveSnoozesBeforeMoving,
                isSmartWakeupEnabled = isSmartWakeupEnabled,
                wakeupCheckDelayMinutes = wakeupCheckDelayMinutes,
                wakeupCheckTimeoutSeconds = wakeupCheckTimeoutSeconds,
                userName = buddyUserName,
                vibrationPattern = vibrationPattern,
                vibrationCrescendoStartGapSeconds = vibrationCrescendoStartGapSeconds
            )
            try {
                viewModel.addAlarm(newAlarm)
                onFinished()
            } catch (e: Throwable) {
                Log.e("AlarmWizard", "Error finishing alarm creation (e.g. Hebrew/RTL)", e)
                onFinished()
            }
        }
    }

    fun handleBack() {
        if (currentPage > 0) currentPage-- else onBack()
    }

    BackHandler { handleBack() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                )
            )
            .pointerInput(Unit) {
                detectHorizontalDragGestures { _, dragAmount ->
                    if (dragAmount > 50) {
                        coroutineScope.launch {
                            currentPage = previousWizardPage(currentPage)
                        }
                    } else if (dragAmount < -50) {
                        coroutineScope.launch { handleNext() }
                    }
                }
            }
    ) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            // Header
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { handleBack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    repeat(totalPages) { index ->
                        val isSelected = index == currentPage
                        val width by animateDpAsState(targetValue = if (isSelected) 24.dp else 8.dp, label = "indicator")
                        Box(
                            modifier = Modifier
                                .size(height = 8.dp, width = width)
                                .clip(CircleShape)
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(
                    onClick = { 
                        viewModel.updateAlarmCreationStyle("SIMPLE")
                        onSwitchToSimple()
                    }
                ) {
                    Text(stringResource(R.string.wizard_quick_setup), style = MaterialTheme.typography.labelLarge)
                }
                TextButton(onClick = { onBack() }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }

            AnimatedContent(
                targetState = currentPage,
                transitionSpec = {
                    if (targetState > initialState) {
                        (fadeIn(tween(400)) + slideInHorizontally(tween(400)) { it / 2 }).togetherWith(fadeOut(tween(400)) + slideOutHorizontally(tween(400)) { -it / 2 })
                    } else {
                        (fadeIn(tween(400)) + slideInHorizontally(tween(400)) { -it / 2 }).togetherWith(fadeOut(tween(400)) + slideOutHorizontally(tween(400)) { it / 2 })
                    }
                },
                label = "wizard_content",
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) { targetPage ->
                Column(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp).verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val config = when (targetPage) {
                        0 -> Triple("⏰", stringResource(R.string.wizard_1_title), stringResource(R.string.wizard_1_body))
                        1 -> Triple("🌤️", stringResource(R.string.wizard_2_title), stringResource(R.string.wizard_2_body))
                        2 -> Triple("🧩", stringResource(R.string.wizard_3_title), stringResource(R.string.wizard_3_body))
                        3 -> Triple("🤝", stringResource(R.string.wizard_4_title), stringResource(R.string.wizard_4_body))
                        else -> Triple("✨", stringResource(R.string.wizard_5_title), stringResource(R.string.wizard_5_body))
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                    Box(modifier = Modifier.size(100.dp).clip(RoundedCornerShape(28.dp)).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                        Text(text = config.first, fontSize = 48.sp)
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(text = config.second, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = config.third, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 24.sp)
                    Spacer(modifier = Modifier.height(40.dp))

                    when (targetPage) {
                        0 -> TimeAndDayStep(timePickerState, selectedDays, { selectedDays = it }, defaultSettings.weekendDays)
                        1 -> WakeUpStyleStep(
                            selectedPersona, { selectedPersona = it },
                            isBriefingEnabled, { enabled -> 
                                if (enabled && isCloudAiEnabled && geminiApiKey.isBlank()) {
                                    showCloudAiSetupDialog = true
                                } else {
                                    isBriefingEnabled = enabled
                                }
                            },
                            isTtsEnabled, { isTtsEnabled = it },
                            isSoundEnabled, { isSoundEnabled = it },
                            isVibrate, { isVibrate = it },
                            isSnoozeEnabled, { isSnoozeEnabled = it },
                            isGentleWake, { isGentleWake = it },
                            crescendoDurationMinutes, { crescendoDurationMinutes = it },
                            snoozeDurationMinutes, { snoozeDurationMinutes = it },
                            isSmoothFadeOut, { isSmoothFadeOut = it },
                            isEvasiveSnooze, { isEvasiveSnooze = it },
                            evasiveSnoozesBeforeMoving, { evasiveSnoozesBeforeMoving = it },
                            vibrationPattern, { vibrationPattern = it },
                            vibrationCrescendoStartGapSeconds, { vibrationCrescendoStartGapSeconds = it },
                            isCloudAiEnabled, { enabled -> 
                                if (enabled && geminiApiKey.isBlank()) {
                                    showCloudAiSetupDialog = true
                                } else {
                                    viewModel.updateCloudAiEnabled(enabled)
                                }
                            }
                        )
                        2 -> WakeUpChallengeStep(
                            selectedMathDifficulty, { selectedMathDifficulty = it },
                            mathProblemCount, { mathProblemCount = it },
                            mathGraduallyIncreaseDifficulty, { mathGraduallyIncreaseDifficulty = it },
                            smileToDismiss, { smileToDismiss = it },
                            smileFallbackMethod, { smileFallbackMethod = it },
                            isSmartWakeupEnabled, { isSmartWakeupEnabled = it },
                            wakeupCheckDelayMinutes, { wakeupCheckDelayMinutes = it },
                            wakeupCheckTimeoutSeconds, { wakeupCheckTimeoutSeconds = it }
                        )
                        3 -> WakeUpBuddyStep(
                            buddyName, buddyPhone, buddyMessage, { buddyMessage = it },
                            buddyAlertAfterMinutes, { buddyAlertAfterMinutes = it },
                            buddyUserName, { 
                                buddyUserName = it
                                viewModel.updateUserName(it)
                            },
                            confirmedNumbers, pendingCodes, { showBuddyDialog = true },
                            onSendInvite = { 
                                viewModel.sendBuddyOptInRequest(buddyPhone, buddyName, buddyUserName)
                                if (globalBuddies.none { it.endsWith("|$buddyPhone") }) {
                                    viewModel.addGlobalBuddy(buddyName.ifBlank { defaultBuddyName }, buddyPhone)
                                }
                            }
                        )
                        4 -> FinalSummaryStep(
                            selectedTime, selectedDays, alarmLabel, { alarmLabel = it },
                            personas.find { it.id == selectedPersona } ?: personas[0],
                            selectedMathDifficulty, smileToDismiss, buddyName,
                            isGentleWake, buddyMessage, buddyAlertAfterMinutes, crescendoDurationMinutes,
                            isSnoozeEnabled, snoozeDurationMinutes, isSmoothFadeOut, isEvasiveSnooze, isSmartWakeupEnabled
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(48.dp))
                    Button(
                        onClick = { handleNext() },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(if (currentPage < totalPages - 1) stringResource(R.string.btn_next) else stringResource(R.string.wizard_btn_finish), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }

    if (showBuddyDialog) {
        BuddySelectionDialog(
            onDismiss = { showBuddyDialog = false },
            onBuddySelected = { name, phone ->
                buddyName = name
                buddyPhone = phone
                showBuddyDialog = false
            },
            globalBuddies = globalBuddies
        )
    }

    if (showCloudAiSetupDialog) {
        CloudAiSetupDialog(
            currentKey = geminiApiKey,
            onDismiss = { showCloudAiSetupDialog = false },
            onSave = { key ->
                viewModel.updateGeminiApiKey(key)
                viewModel.updateCloudAiEnabled(true)
                isBriefingEnabled = true
                showCloudAiSetupDialog = false
            }
        )
    }
}

@Composable
fun WizardAdvancedSection(
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
        TextButton(
            onClick = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(0.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    stringResource(R.string.wizard_advanced_options),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.weight(1f))
            }
        }
        
        AnimatedVisibility(visible = expanded) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    content()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeAndDayStep(
    timePickerState: TimePickerState,
    selectedDays: List<Int>,
    onDaysChange: (List<Int>) -> Unit,
    weekendDays: Set<Int>
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // We'll use a card-like container for the time picker to make it feel premium
        Surface(
            tonalElevation = 2.dp,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Box(modifier = Modifier.padding(24.dp)) {
                TimePicker(state = timePickerState)
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            stringResource(R.string.wizard_repeat_frequency),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        ImprovedDaySelector(
            selectedDays = selectedDays,
            weekendDays = weekendDays,
            onSelectionChanged = onDaysChange
        )
    }
}

@Composable
fun WakeUpStyleStep(
    selectedPersona: String,
    onPersonaChange: (String) -> Unit,
    isBriefingEnabled: Boolean,
    onBriefingEnabledChange: (Boolean) -> Unit,
    isTtsEnabled: Boolean,
    onTtsEnabledChange: (Boolean) -> Unit,
    isSoundEnabled: Boolean,
    onSoundEnabledChange: (Boolean) -> Unit,
    isVibrate: Boolean,
    onVibrateChange: (Boolean) -> Unit,
    isSnoozeEnabled: Boolean,
    onSnoozeEnabledChange: (Boolean) -> Unit,
    isGentleWake: Boolean,
    onGentleWakeChange: (Boolean) -> Unit,
    crescendoDurationMinutes: Int,
    onCrescendoDurationChange: (Int) -> Unit,
    snoozeDurationMinutes: Int,
    onSnoozeDurationChange: (Int) -> Unit,
    isSmoothFadeOut: Boolean,
    onSmoothFadeOutChange: (Boolean) -> Unit,
    isEvasiveSnooze: Boolean,
    onEvasiveSnoozeChange: (Boolean) -> Unit,
    evasiveSnoozesBefore: Int,
    onEvasiveSnoozesBeforeChange: (Int) -> Unit,
    vibrationPattern: String,
    onVibrationPatternChange: (String) -> Unit,
    vibrationStartGap: Int,
    onVibrationStartGapChange: (Int) -> Unit,
    isCloudAiEnabled: Boolean,
    onCloudAiEnabledChange: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Text(
            stringResource(R.string.wizard_ai_wakeup_persona),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        // Horizontal Row of Persona Cards
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            personas.forEach { persona ->
                PersonaCard(
                    persona = persona,
                    isSelected = selectedPersona == persona.id,
                    onClick = { onPersonaChange(persona.id) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            stringResource(R.string.wizard_wake_up_features),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Surface(
            tonalElevation = 1.dp,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                FeatureToggle(
                    title = stringResource(R.string.wizard_daily_briefing),
                    desc = stringResource(R.string.wizard_daily_briefing_desc),
                    checked = isBriefingEnabled,
                    onCheckedChange = onBriefingEnabledChange,
                    icon = "📻"
                )
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                
                FeatureToggle(
                    title = stringResource(R.string.wizard_voice_synthesis),
                    desc = stringResource(R.string.wizard_voice_synthesis_desc),
                    checked = isTtsEnabled,
                    onCheckedChange = onTtsEnabledChange,
                    icon = "🗣️",
                    enabled = isBriefingEnabled
                )
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                
                FeatureToggle(
                    title = stringResource(R.string.settings_alarm_sound),
                    checked = isSoundEnabled,
                    onCheckedChange = onSoundEnabledChange,
                    icon = "🔔"
                )
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                
                FeatureToggle(
                    title = stringResource(R.string.settings_vibrate),
                    checked = isVibrate,
                    onCheckedChange = onVibrateChange,
                    icon = "📳"
                )
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                
                FeatureToggle(
                    title = stringResource(R.string.alarm_detail_snooze_label),
                    checked = isSnoozeEnabled,
                    onCheckedChange = onSnoozeEnabledChange,
                    icon = "💤"
                )
            }
        }

        WizardAdvancedSection {
            // AI Engine Section
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(text = "🧠", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.wizard_2_ai_engine),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            FeatureToggle(
                title = stringResource(R.string.wizard_2_cloud_ai_title),
                desc = stringResource(R.string.wizard_2_cloud_ai_desc),
                checked = isCloudAiEnabled,
                onCheckedChange = onCloudAiEnabledChange,
                icon = "☁️",
                enabled = isBriefingEnabled
            )

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(modifier = Modifier.padding(bottom = 24.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Slick Audio Section
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(text = "🎵", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.wizard_2_slick_audio),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            FeatureToggle(
                title = stringResource(R.string.wizard_2_gentle_wake),
                desc = stringResource(R.string.wizard_2_gentle_wake_desc),
                checked = isGentleWake,
                onCheckedChange = onGentleWakeChange,
                icon = "🌅"
            )
            
            if (isGentleWake) {
                Spacer(modifier = Modifier.height(16.dp))
                Column(modifier = Modifier.padding(horizontal = 4.dp)) {
                    Text(
                        text = "${stringResource(R.string.wizard_2_gentle_wake_duration)}: ${stringResource(R.string.wizard_2_gentle_wake_unit_min, crescendoDurationMinutes)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = crescendoDurationMinutes.toFloat(),
                        onValueChange = { onCrescendoDurationChange(it.toInt()) },
                        valueRange = 1f..20f,
                        steps = 19
                    )
                }

                if (isVibrate) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        stringResource(R.string.vibration_advanced_title),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    VibrationPatternGallery(
                        selectedPattern = vibrationPattern,
                        onPatternSelected = onVibrationPatternChange
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            stringResource(R.string.vibration_initial_gap_label),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            stringResource(R.string.vibration_initial_gap_unit, vibrationStartGap),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = vibrationStartGap.toFloat(),
                        onValueChange = { onVibrationStartGapChange(it.toInt()) },
                        valueRange = 1f..60f,
                        steps = 59
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            AnimatedVisibility(visible = isSoundEnabled || isVibrate) {
                FeatureToggle(
                    title = stringResource(R.string.wizard_2_smooth_fade),
                    desc = stringResource(R.string.wizard_2_smooth_fade_desc),
                    checked = isSmoothFadeOut,
                    onCheckedChange = onSmoothFadeOutChange,
                    icon = "🔉"
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(modifier = Modifier.padding(bottom = 24.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            
            // Smart Snooze Section
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(text = "💤", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.wizard_2_smart_snooze),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (isSnoozeEnabled) {
                Column(modifier = Modifier.padding(horizontal = 4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            text = stringResource(R.string.wizard_2_snooze_duration),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${snoozeDurationMinutes}m",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = snoozeDurationMinutes.toFloat(),
                        onValueChange = { onSnoozeDurationChange(it.toInt()) },
                        valueRange = 1f..60f,
                        steps = 58
                    )
                }
            }
            
            if (snoozeDurationMinutes > 0) {
                Spacer(modifier = Modifier.height(16.dp))
                FeatureToggle(
                    title = stringResource(R.string.wizard_2_evasive_snooze),
                    desc = stringResource(R.string.wizard_2_evasive_snooze_desc),
                    checked = isEvasiveSnooze,
                    onCheckedChange = onEvasiveSnoozeChange,
                    icon = "🏃"
                )
                
                if (isEvasiveSnooze) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (evasiveSnoozesBefore == 0) stringResource(R.string.wizard_2_evasive_starts_after, 1) else stringResource(R.string.wizard_2_evasive_starts_after_plural, evasiveSnoozesBefore + 1),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    Slider(
                        value = evasiveSnoozesBefore.toFloat(),
                        onValueChange = { onEvasiveSnoozesBeforeChange(it.toInt()) },
                        valueRange = 0f..5f,
                        steps = 4
                    )
                }
            }
        }
    }
}

@Composable
fun PersonaCard(
    persona: Persona,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier.size(width = 110.dp, height = 130.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(persona.emoji, fontSize = 32.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                stringResource(getPersonaLabelRes(persona.id)),
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun FeatureToggle(
    title: String,
    desc: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: String,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.5f),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 24.sp, modifier = Modifier.padding(end = 16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            if (desc != null) {
                Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Composable
fun FinalSummaryStep(
    selectedTime: LocalTime,
    selectedDays: List<Int>,
    alarmLabel: String,
    onLabelChange: (String) -> Unit,
    persona: Persona,
    mathDifficulty: Int,
    smileToDismiss: Boolean,
    buddyName: String,
    isGentleWake: Boolean,
    buddyMessage: String,
    buddyAlertAfterMinutes: Int,
    crescendoDurationMinutes: Int,
    isSnoozeEnabled: Boolean,
    snoozeDurationMinutes: Int,
    isSmoothFadeOut: Boolean,
    isEvasiveSnooze: Boolean,
    isSmartWakeupEnabled: Boolean
) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Text(
            stringResource(R.string.wizard_review_label),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        // Summary Card
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⏰", fontSize = 32.sp)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            selectedTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (selectedDays.isEmpty()) stringResource(R.string.wizard_once) else stringResource(R.string.wizard_repeats_on_days, selectedDays.size),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        
                        // Time Until Calculation for Wizard Review
                        val nextOccurrence = remember(selectedTime, selectedDays) {
                            AlarmUtils.calculateNextOccurrence(
                                Alarm(time = selectedTime, daysOfWeek = selectedDays)
                            )
                        }
                        Text(
                            text = stringResource(R.string.wizard_starts_in, AlarmUtils.formatTimeUntil(context.resources, nextOccurrence)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                
                SummaryRow(stringResource(R.string.wizard_summary_persona), "${persona.emoji} ${stringResource(getPersonaLabelRes(persona.id))}")
                if (isGentleWake) SummaryRow(stringResource(R.string.wizard_summary_volume), "🌅 ${stringResource(R.string.wizard_summary_gentle_wake, crescendoDurationMinutes)}")
                if (isSmoothFadeOut) SummaryRow(stringResource(R.string.wizard_summary_fade_out), "🔉 ${stringResource(R.string.wizard_summary_smooth)}")
                SummaryRow(stringResource(R.string.wizard_summary_snooze), if (!isSnoozeEnabled) stringResource(R.string.wizard_summary_off) else "${snoozeDurationMinutes}m${if(isEvasiveSnooze) " 🏃" else ""}")
                if (smileToDismiss) SummaryRow(stringResource(R.string.wizard_summary_challenge), "😊 ${stringResource(R.string.wizard_summary_smile)}")
                if (mathDifficulty > 0) SummaryRow(stringResource(R.string.wizard_summary_challenge), "🧮 ${stringResource(R.string.wizard_summary_math, if (mathDifficulty == 1) stringResource(R.string.wizard_difficulty_easy) else if (mathDifficulty == 2) stringResource(R.string.wizard_math_med) else stringResource(R.string.wizard_difficulty_hard))}")
                if (isSmartWakeupEnabled) SummaryRow(stringResource(R.string.wizard_summary_smart_check), "🚨 ${stringResource(R.string.wizard_summary_enabled)}")
                if (buddyName.isNotBlank()) {
                    SummaryRow(stringResource(R.string.wizard_summary_guardian), "👤 $buddyName")
                    if (buddyMessage.isNotBlank()) SummaryRow(stringResource(R.string.wizard_summary_message), "✉️ \"$buddyMessage\"")
                    SummaryRow(stringResource(R.string.wizard_summary_delay), "⏱️ ${stringResource(R.string.wizard_summary_delay_min, buddyAlertAfterMinutes)}")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            stringResource(R.string.wizard_alarm_label),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = alarmLabel,
            onValueChange = onLabelChange,
            placeholder = { Text(stringResource(R.string.wizard_label_placeholder)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )
    }
}

@Composable
fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun WakeUpBuddyStep(
    buddyName: String,
    buddyPhone: String,
    buddyMessage: String,
    onBuddyMessageChange: (String) -> Unit,
    buddyAlertAfterMinutes: Int,
    onBuddyAlertAfterMinutesChange: (Int) -> Unit,
    buddyUserName: String,
    onBuddyUserNameChange: (String) -> Unit,
    confirmedNumbers: Set<String>,
    pendingCodes: Set<String>,
    onBuddyClick: () -> Unit,
    onSendInvite: () -> Unit
) {
    val context = LocalContext.current
    val smsPermission = Manifest.permission.SEND_SMS
    
    val isConfirmed = confirmedNumbers.contains(buddyPhone)
    val isPending = pendingCodes.any { it.endsWith(":$buddyPhone") }

    var cooldownRemaining by remember { mutableIntStateOf(0) }

    LaunchedEffect(cooldownRemaining) {
        if (cooldownRemaining > 0) {
            kotlinx.coroutines.delay(1000)
            cooldownRemaining--
        }
    }

    val smsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onSendInvite()
            cooldownRemaining = 30
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Surface(
            onClick = onBuddyClick,
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth(),
            border = if (isConfirmed) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isConfirmed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = if (isConfirmed) "✅" else if (buddyName.isNotBlank()) buddyName.take(1) else "👤", 
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (buddyName.isNotBlank()) buddyName else stringResource(R.string.wizard_add_buddy),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (isConfirmed) stringResource(R.string.wizard_4_status_confirmed)
                        else if (isPending) stringResource(R.string.wizard_4_status_pending)
                        else if (buddyPhone.isNotBlank()) buddyPhone 
                        else stringResource(R.string.wizard_buddy_alerted_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isConfirmed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Icon(if (buddyName.isBlank()) Icons.Default.Add else Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }

        if (buddyPhone.isNotBlank() && !isConfirmed) {
            Spacer(modifier = Modifier.height(8.dp))
            
            Button(
                onClick = {
                    if (cooldownRemaining == 0) {
                        if (ContextCompat.checkSelfPermission(context, smsPermission) == PackageManager.PERMISSION_GRANTED) {
                            onSendInvite()
                            cooldownRemaining = 30
                        } else {
                            smsLauncher.launch(smsPermission)
                        }
                    }
                },
                enabled = cooldownRemaining == 0,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPending) MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.secondary
                )
            ) {
                Icon(
                    imageVector = if (isPending) Icons.Default.Refresh else Icons.AutoMirrored.Filled.Send, 
                    contentDescription = null, 
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (cooldownRemaining > 0) stringResource(R.string.wizard_wait_seconds, cooldownRemaining)
                           else if (isPending) stringResource(R.string.wizard_4_btn_resend) 
                           else stringResource(R.string.wizard_4_btn_invite)
                )
            }
            
            Text(
                stringResource(R.string.wizard_buddy_sms_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )
        }

        WizardAdvancedSection {
            Text(
                stringResource(R.string.wizard_4_custom_message),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = buddyMessage,
                onValueChange = onBuddyMessageChange,
                placeholder = { Text(stringResource(R.string.wizard_4_custom_message_hint)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                "${stringResource(R.string.wizard_4_alert_after)}: $buddyAlertAfterMinutes min",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Slider(
                value = buddyAlertAfterMinutes.toFloat(),
                onValueChange = { onBuddyAlertAfterMinutesChange(it.toInt()) },
                valueRange = 1f..30f,
                steps = 29
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                stringResource(R.string.wizard_4_user_name),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = buddyUserName,
                onValueChange = { onBuddyUserNameChange(it) },
                label = { Text(stringResource(R.string.wizard_4_your_name_label)) },
                placeholder = { Text(stringResource(R.string.wizard_4_your_name_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }
}

@Composable
fun WakeUpChallengeStep(
    mathDifficulty: Int,
    onMathDifficultyChange: (Int) -> Unit,
    mathProblemCount: Int,
    onMathProblemCountChange: (Int) -> Unit,
    mathGraduallyIncreaseDifficulty: Boolean,
    onMathGraduallyIncreaseDifficultyChange: (Boolean) -> Unit,
    smileToDismiss: Boolean,
    onSmileToDismissChange: (Boolean) -> Unit,
    smileFallbackMethod: String,
    onSmileFallbackMethodChange: (String) -> Unit,
    isSmartWakeupEnabled: Boolean,
    onSmartWakeupEnabledChange: (Boolean) -> Unit,
    wakeupCheckDelayMinutes: Int,
    onWakeupCheckDelayChange: (Int) -> Unit,
    wakeupCheckTimeoutSeconds: Int,
    onWakeupCheckTimeoutChange: (Int) -> Unit
) {
    val context = LocalContext.current
    val cameraPermission = Manifest.permission.CAMERA
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, cameraPermission) == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (isGranted) {
            onSmileToDismissChange(true)
        } else {
            onSmileToDismissChange(false)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        // Face Challenge Card (Now on top)
        ChallengeCard(
            title = stringResource(R.string.wizard_3_face_title),
            desc = stringResource(R.string.wizard_3_face_desc),
            icon = "😊",
            checked = smileToDismiss,
            onCheckedChange = { checked ->
                if (checked) {
                    if (hasCameraPermission) {
                        onSmileToDismissChange(true)
                    } else {
                        cameraPermissionLauncher.launch(cameraPermission)
                    }
                } else {
                    onSmileToDismissChange(false)
                }
            }
        ) {
            Text(
                stringResource(R.string.wizard_3_face_help),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
            if (smileToDismiss && !hasCameraPermission) {
                Text(
                    stringResource(R.string.wizard_camera_permission_required),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Math Challenge Card (Now below)
        ChallengeCard(
            title = stringResource(R.string.wizard_3_math_title),
            desc = stringResource(R.string.wizard_3_math_desc),
            icon = "➕",
            checked = mathDifficulty > 0,
            onCheckedChange = { if (it) onMathDifficultyChange(1) else onMathDifficultyChange(0) }
        ) {
            Column(modifier = Modifier.padding(top = 16.dp)) {
                Text(stringResource(R.string.wizard_difficulty), style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = mathDifficulty.toFloat(),
                    onValueChange = { onMathDifficultyChange(it.toInt()) },
                    valueRange = 1f..3f,
                    steps = 1
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.wizard_difficulty_easy), style = MaterialTheme.typography.labelSmall)
                    Text(stringResource(R.string.wizard_difficulty_medium), style = MaterialTheme.typography.labelSmall)
                    Text(stringResource(R.string.wizard_difficulty_hard), style = MaterialTheme.typography.labelSmall)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(stringResource(R.string.wizard_math_problems_count, mathProblemCount), style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = mathProblemCount.toFloat(),
                    onValueChange = { onMathProblemCountChange(it.toInt()) },
                    valueRange = 1f..10f,
                    steps = 8
                )
            }
        }

        WizardAdvancedSection {
            // Challenge Dynamics Section
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(text = "🛡️", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.wizard_3_challenge_dynamics),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            if (mathDifficulty > 0 || smileToDismiss) {
                if (mathDifficulty > 0) {
                    FeatureToggle(
                        title = stringResource(R.string.wizard_3_math_gradual),
                        desc = stringResource(R.string.wizard_3_math_gradual_desc),
                        checked = mathGraduallyIncreaseDifficulty,
                        onCheckedChange = onMathGraduallyIncreaseDifficultyChange,
                        icon = "📈"
                    )
                }
                
                if (smileToDismiss) {
                    if (mathDifficulty > 0) Spacer(modifier = Modifier.height(16.dp))
                    
                    Column(modifier = Modifier.padding(horizontal = 4.dp)) {
                        Text(
                            stringResource(R.string.wizard_3_face_fallback),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = smileFallbackMethod == "NONE",
                                onClick = { onSmileFallbackMethodChange("NONE") },
                                label = { Text(stringResource(R.string.wizard_3_face_fallback_none)) }
                            )
                            FilterChip(
                                selected = smileFallbackMethod == "MATH",
                                onClick = { onSmileFallbackMethodChange("MATH") },
                                label = { Text(stringResource(R.string.wizard_3_face_fallback_math)) }
                            )
                        }
                    }
                }
            } else {
                Text(
                    stringResource(R.string.wizard_enable_challenge_for_dynamics),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(modifier = Modifier.padding(bottom = 24.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            
            // Safety Net Section
            Surface(
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(text = "🚨", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.wizard_3_safety_net),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            FeatureToggle(
                title = stringResource(R.string.wizard_3_smart_check),
                desc = stringResource(R.string.wizard_3_smart_check_desc),
                checked = isSmartWakeupEnabled,
                onCheckedChange = onSmartWakeupEnabledChange,
                icon = "🚨"
            )
            
            if (isSmartWakeupEnabled) {
                Spacer(modifier = Modifier.height(16.dp))
                Column(modifier = Modifier.padding(horizontal = 4.dp)) {
                    Text(
                        text = stringResource(R.string.wizard_3_smart_delay, wakeupCheckDelayMinutes),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = wakeupCheckDelayMinutes.toFloat(),
                        onValueChange = { onWakeupCheckDelayChange(it.toInt()) },
                        valueRange = 1f..10f,
                        steps = 8
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = stringResource(R.string.wizard_3_smart_timeout, wakeupCheckTimeoutSeconds),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = wakeupCheckTimeoutSeconds.toFloat(),
                        onValueChange = { onWakeupCheckTimeoutChange(it.toInt()) },
                        valueRange = 10f..120f,
                        steps = 10
                    )
                }
            }
        }
    }
}

@Composable
fun ChallengeCard(
    title: String,
    desc: String,
    icon: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        tonalElevation = if (checked) 2.dp else 1.dp,
        border = if (checked) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(icon, fontSize = 24.sp, modifier = Modifier.padding(end = 16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = checked, onCheckedChange = onCheckedChange)
            }
            
            AnimatedVisibility(visible = checked) {
                Column {
                    content()
                }
            }
        }
    }
}

@Composable
fun CloudAiSetupDialog(
    currentKey: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var keyInput by remember { mutableStateOf(currentKey) }
    val context = androidx.compose.ui.platform.LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.wizard_cloud_ai_setup_title), fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    stringResource(R.string.wizard_cloud_ai_setup_body),
                    style = MaterialTheme.typography.bodyMedium
                )
                
                OutlinedTextField(
                    value = keyInput,
                    onValueChange = { keyInput = it },
                    label = { Text(stringResource(R.string.wizard_cloud_ai_setup_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation()
                )
                
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/app/apikey"))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.filledTonalButtonColors()
                ) {
                    Text(stringResource(R.string.wizard_cloud_ai_setup_get_key))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(keyInput) },
                enabled = keyInput.isNotBlank()
            ) {
                Text(stringResource(R.string.wizard_cloud_ai_setup_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )
}
