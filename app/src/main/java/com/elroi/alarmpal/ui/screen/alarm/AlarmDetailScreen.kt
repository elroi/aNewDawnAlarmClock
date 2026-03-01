package com.elroi.alarmpal.ui.screen.alarm

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.elroi.alarmpal.ui.components.SettingHelpIcon
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.elroi.alarmpal.domain.model.Alarm
import com.elroi.alarmpal.ui.viewmodel.AlarmViewModel
import com.elroi.alarmpal.domain.manager.AlarmDefaults
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

// ─────────────────────────────────────────────────────────────────────────────
// Main screen
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmDetailScreen(
    alarmId: String?,
    onNavigateUp: () -> Unit,
    viewModel: AlarmViewModel = hiltViewModel()
) {
    val defaultSettings by viewModel.defaultAlarmSettings.collectAsState()
    var time           by remember { mutableStateOf(LocalTime.now()) }
    var label          by remember { mutableStateOf("") }
    var isGentleWake   by remember { mutableStateOf(defaultSettings.isGentleWake) }
    var buddyPhone     by remember { mutableStateOf("") }
    var buddyName      by remember { mutableStateOf("") }
    var buddyUserName  by remember { mutableStateOf("") }
    var buddyMessage   by remember { mutableStateOf("") }
    var buddyEnabled   by remember { mutableStateOf(false) }
    var buddyAlertDelay by remember { mutableStateOf(5) }
    var daysOfWeek     by remember { mutableStateOf(emptyList<Int>()) }
    var mathDifficulty by remember { mutableStateOf(defaultSettings.mathDifficulty) }
    var mathProblemCount by remember { mutableIntStateOf(defaultSettings.mathProblemCount) }
    var mathGraduallyIncreaseDifficulty by remember { mutableStateOf(defaultSettings.mathGraduallyIncreaseDifficulty) }
    var mathEnabled    by remember { mutableStateOf(defaultSettings.mathDifficulty > 0) }
    var smileToDismiss by remember { mutableStateOf(false) }
    var smileFallbackMethod by remember { mutableStateOf(defaultSettings.smileFallbackMethod) }
    var snoozeDuration by remember { mutableStateOf(defaultSettings.snoozeDurationMinutes) }
    var crescendoDuration by remember { mutableIntStateOf(defaultSettings.crescendoDurationMinutes) } // minutes, 0=instant
    var isBriefingEnabled by remember { mutableStateOf(defaultSettings.isBriefingEnabled) }
    var isTtsEnabled   by remember { mutableStateOf(defaultSettings.isTtsEnabled) }
    var isEvasiveSnooze by remember { mutableStateOf(defaultSettings.isEvasiveSnooze) }
    var evasiveSnoozesBeforeMoving by remember { mutableIntStateOf(defaultSettings.evasiveSnoozesBeforeMoving) }
    var isSmoothFadeOut by remember { mutableStateOf(defaultSettings.isSmoothFadeOut) }
    var isVibrate      by remember { mutableStateOf(defaultSettings.isVibrate) }
    var isSoundEnabled by remember { mutableStateOf(defaultSettings.isSoundEnabled) }
    var soundUri       by remember { mutableStateOf<String?>(null) }
    var soundName      by remember { mutableStateOf("Default") }
    var isSmartWakeupEnabled by remember { mutableStateOf(false) }
    var wakeupCheckDelayMinutes by remember { mutableIntStateOf(3) }
    var wakeupCheckTimeoutSeconds by remember { mutableIntStateOf(60) }
    val weekendDays    = defaultSettings.weekendDays
    var currentAlarm   by remember { mutableStateOf<Alarm?>(null) }
    var initialState by remember { mutableStateOf<AlarmStateSnapshot?>(null) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    val hasChanges = remember(
        initialState, time, label, isGentleWake, buddyPhone, buddyName,
        buddyUserName, buddyMessage, buddyEnabled, buddyAlertDelay,
        daysOfWeek, mathDifficulty, mathProblemCount, mathGraduallyIncreaseDifficulty, mathEnabled, smileToDismiss, smileFallbackMethod,
        snoozeDuration, crescendoDuration, isBriefingEnabled, isTtsEnabled, isEvasiveSnooze,
        evasiveSnoozesBeforeMoving, isSmoothFadeOut, isVibrate, isSoundEnabled, soundUri, isSmartWakeupEnabled,
        wakeupCheckDelayMinutes, wakeupCheckTimeoutSeconds
    ) {
        val current = AlarmStateSnapshot(
            time, label, isGentleWake, buddyPhone, buddyName,
            buddyUserName, buddyMessage, buddyEnabled, buddyAlertDelay,
            daysOfWeek, mathDifficulty, mathProblemCount, mathGraduallyIncreaseDifficulty, mathEnabled, smileToDismiss, smileFallbackMethod,
            snoozeDuration, crescendoDuration, isBriefingEnabled, isTtsEnabled, isEvasiveSnooze,
            evasiveSnoozesBeforeMoving, isSmoothFadeOut, isVibrate, isSoundEnabled, soundUri, isSmartWakeupEnabled,
            wakeupCheckDelayMinutes, wakeupCheckTimeoutSeconds
        )
        initialState != null && current != initialState
    }

    // Preview — launches AlarmActivity in preview mode + plays audio
    // When AlarmActivity is dismissed, the MediaPlayer stops automatically
    val context = LocalContext.current
    var isPreviewPlaying by remember { mutableStateOf(false) }
    var previewPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    
    val stopPreviewAudio = {
        previewPlayer?.stop()
        previewPlayer?.release()
        previewPlayer = null
        isPreviewPlaying = false
    }
    
    val previewLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { stopPreviewAudio() } 
    
    DisposableEffect(Unit) {
        onDispose { stopPreviewAudio() }
    }

    BackHandler(enabled = hasChanges) {
        showDiscardDialog = true
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard Unsaved Changes?") },
            text = { Text("You have unsaved changes. Are you sure you want to discard them?") },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    onNavigateUp()
                }) {
                    Text("Discard", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Keep Editing")
                }
            }
        )
    }
    
    val togglePreview = {
        if (isPreviewPlaying) {
            stopPreviewAudio()
        } else {
            // Start audio
                val uri = if (soundUri != null) {
                    try { Uri.parse(soundUri) } 
                    catch (e: Exception) { RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE) }
                } else {
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                }
                try {
                    val mp = MediaPlayer().apply {
                        setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                            .build()
                    )
                    setDataSource(context, uri)
                    isLooping = true  // loop until alarm screen is dismissed
                    prepare()
                    start()
                }
                previewPlayer = mp
                isPreviewPlaying = true
            } catch (_: Exception) { }

            // Launch AlarmActivity in preview mode
            val intent = Intent(context, com.elroi.alarmpal.ui.activity.AlarmActivity::class.java).apply {
                putExtra("IS_PREVIEW", true)
                putExtra("ALARM_LABEL", label.ifBlank { "Preview" })
                putExtra("ALARM_MATH_DIFFICULTY", if (mathEnabled) mathDifficulty else 0)
                putExtra("ALARM_MATH_PROBLEM_COUNT", mathProblemCount)
                putExtra("ALARM_MATH_GRADUAL_DIFFICULTY", mathGraduallyIncreaseDifficulty)
                putExtra("ALARM_SMILE_TO_DISMISS", smileToDismiss)
                putExtra("ALARM_SNOOZE_DURATION", snoozeDuration)
                putExtra("ALARM_IS_EVASIVE_SNOOZE", isEvasiveSnooze)
                putExtra("ALARM_EVASIVE_SNOOZES_BEFORE_MOVING", evasiveSnoozesBeforeMoving)
                putExtra("ALARM_SOUND_URI", soundUri)
                putExtra("ALARM_IS_SMOOTH_FADE_OUT", isSmoothFadeOut)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            previewLauncher.launch(intent)
        }
    }

    // Time picker dialog state — created fresh from current `time` each open
    // so editing an existing alarm shows its saved time, not "now"
    var showTimePicker by remember { mutableStateOf(false) }
    var timePickerState by remember { mutableStateOf<TimePickerState?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(alarmId, defaultSettings) {
        if (!alarmId.isNullOrBlank()) {
            if (currentAlarm == null) {
                currentAlarm = viewModel.getAlarm(alarmId)
                currentAlarm?.let {
                    time          = it.time
                    label         = it.label ?: ""
                    isGentleWake  = it.isGentleWake
                    buddyPhone    = it.buddyPhoneNumber ?: ""
                    buddyName     = it.buddyName ?: ""
                    buddyUserName = it.userName ?: ""
                    buddyMessage  = it.buddyMessage ?: ""
                    buddyEnabled  = !it.buddyPhoneNumber.isNullOrBlank()
                    buddyAlertDelay = it.buddyAlertDelayMinutes
                    daysOfWeek    = it.daysOfWeek
                    smileToDismiss = it.smileToDismiss
                    smileFallbackMethod = it.smileFallbackMethod
                    mathDifficulty = if (it.smileToDismiss && it.smileFallbackMethod == "NONE") 0 else it.mathDifficulty
                    mathProblemCount = it.mathProblemCount
                    mathGraduallyIncreaseDifficulty = it.mathGraduallyIncreaseDifficulty
                    mathEnabled    = it.mathDifficulty > 0 && !(it.smileToDismiss && it.smileFallbackMethod == "NONE")
                    snoozeDuration = it.snoozeDurationMinutes
                    crescendoDuration = it.crescendoDurationMinutes
                    isBriefingEnabled = it.isBriefingEnabled
                    isTtsEnabled   = it.isTtsEnabled
                    isEvasiveSnooze = it.isEvasiveSnooze
                    evasiveSnoozesBeforeMoving = it.evasiveSnoozesBeforeMoving
                    isSmoothFadeOut = it.isSmoothFadeOut
                    isVibrate      = it.isVibrate
                    isSoundEnabled = it.isSoundEnabled
                    isSmartWakeupEnabled = it.isSmartWakeupEnabled
                    wakeupCheckDelayMinutes = it.wakeupCheckDelayMinutes
                    wakeupCheckTimeoutSeconds = it.wakeupCheckTimeoutSeconds
                    soundUri       = it.soundUri
                    
                    if (initialState == null) {
                        initialState = AlarmStateSnapshot(
                            time, label, isGentleWake, buddyPhone, buddyName,
                            buddyUserName, buddyMessage, buddyEnabled, buddyAlertDelay,
                            daysOfWeek, mathDifficulty, mathProblemCount, mathGraduallyIncreaseDifficulty, mathEnabled, smileToDismiss, smileFallbackMethod,
                            snoozeDuration, crescendoDuration, isBriefingEnabled, isTtsEnabled, isEvasiveSnooze,
                            evasiveSnoozesBeforeMoving, isSmoothFadeOut, isVibrate, isSoundEnabled, soundUri, isSmartWakeupEnabled,
                            wakeupCheckDelayMinutes, wakeupCheckTimeoutSeconds
                        )
                    }
                }
            }
        } else {
            // Apply defaults for a new alarm once they load
            isGentleWake = defaultSettings.isGentleWake
            mathDifficulty = defaultSettings.mathDifficulty
            mathProblemCount = defaultSettings.mathProblemCount
            mathGraduallyIncreaseDifficulty = defaultSettings.mathGraduallyIncreaseDifficulty
            mathEnabled = mathDifficulty > 0
            smileFallbackMethod = defaultSettings.smileFallbackMethod
            snoozeDuration = defaultSettings.snoozeDurationMinutes
            crescendoDuration = defaultSettings.crescendoDurationMinutes
            isBriefingEnabled = defaultSettings.isBriefingEnabled
            isTtsEnabled = defaultSettings.isTtsEnabled
            isEvasiveSnooze = defaultSettings.isEvasiveSnooze
            evasiveSnoozesBeforeMoving = defaultSettings.evasiveSnoozesBeforeMoving
            isSmoothFadeOut = defaultSettings.isSmoothFadeOut
            isVibrate = defaultSettings.isVibrate
            isSoundEnabled = defaultSettings.isSoundEnabled
            soundUri = defaultSettings.defaultSoundUri
            
            if (initialState == null) {
                initialState = AlarmStateSnapshot(
                    time, label, isGentleWake, buddyPhone, buddyName,
                    buddyUserName, buddyMessage, buddyEnabled, buddyAlertDelay,
                    daysOfWeek, mathDifficulty, mathProblemCount, mathGraduallyIncreaseDifficulty, mathEnabled, smileToDismiss, smileFallbackMethod,
                    snoozeDuration, crescendoDuration, isBriefingEnabled, isTtsEnabled, isEvasiveSnooze,
                    evasiveSnoozesBeforeMoving, isSmoothFadeOut, isVibrate, isSoundEnabled, soundUri, isSmartWakeupEnabled,
                    wakeupCheckDelayMinutes, wakeupCheckTimeoutSeconds
                )
            }
        }
    }

    // Attempt to load the ringtone name if we have a URI
    LaunchedEffect(soundUri) {
        if (soundUri != null) {
            try {
                val ringtone = RingtoneManager.getRingtone(context, Uri.parse(soundUri))
                soundName = ringtone?.getTitle(context) ?: "Unknown"
            } catch (e: Exception) {
                soundName = "Custom Sound"
            }
        } else {
            soundName = "Default"
        }
    }

    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri: Uri? = result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            soundUri = uri?.toString()
        }
    }

    // "fires in" label
    val firesInLabel: String = remember(time, daysOfWeek) {
        val now = LocalDateTime.now()
        val candidate = now.with(time).let { if (it.isBefore(now) || it == now) it.plusDays(1) else it }
        val minutes = Duration.between(now, candidate).toMinutes()
        val h = minutes / 60
        val m = minutes % 60
        when {
            h == 0L -> "Fires in ${m}m"
            m == 0L -> "Fires in ${h}h"
            else    -> "Fires in ${h}h ${m}m"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (alarmId == null) "New Alarm" else "Edit Alarm") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (alarmId != null) {
                        IconButton(onClick = {
                            currentAlarm?.let { viewModel.deleteAlarm(it) }
                            onNavigateUp()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            androidx.compose.animation.AnimatedVisibility(
                visible = hasChanges || alarmId == null, // Always show for new alarms, or if changes exist for existing
                enter = androidx.compose.animation.scaleIn() + androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.scaleOut() + androidx.compose.animation.fadeOut()
            ) {
                FloatingActionButton(
                    onClick = {
                        val alarm = currentAlarm ?: Alarm(time = time)
                        val updatedAlarm = alarm.copy(
                            time = time,
                            label = label.takeIf { it.isNotBlank() },
                            isGentleWake = isGentleWake,
                            buddyPhoneNumber = if (buddyEnabled && buddyPhone.isNotBlank()) buddyPhone else null,
                            buddyName = if (buddyEnabled) buddyName else null,
                            userName = if (buddyEnabled) buddyUserName else null,
                            buddyMessage = if (buddyEnabled) buddyMessage else null,
                            buddyAlertDelayMinutes = buddyAlertDelay,
                            daysOfWeek = daysOfWeek,
                            mathDifficulty = if (smileToDismiss && smileFallbackMethod == "NONE") 0 else mathDifficulty,
                            mathProblemCount = mathProblemCount,
                            mathGraduallyIncreaseDifficulty = mathGraduallyIncreaseDifficulty,
                            smileToDismiss = smileToDismiss,
                            smileFallbackMethod = smileFallbackMethod,
                            snoozeDurationMinutes = snoozeDuration,
                            crescendoDurationMinutes = crescendoDuration,
                            isTtsEnabled = isTtsEnabled,
                            isEvasiveSnooze = isEvasiveSnooze,
                            evasiveSnoozesBeforeMoving = evasiveSnoozesBeforeMoving,
                            isSmoothFadeOut = isSmoothFadeOut,
                            isVibrate = isVibrate,
                            isSoundEnabled = isSoundEnabled,
                            soundUri = soundUri,
                            isSmartWakeupEnabled = isSmartWakeupEnabled,
                            wakeupCheckDelayMinutes = wakeupCheckDelayMinutes,
                            wakeupCheckTimeoutSeconds = wakeupCheckTimeoutSeconds
                        )
                        viewModel.addAlarm(updatedAlarm)
                        onNavigateUp()
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Save Alarm")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── ⏰ Time card ─────────────────────────────────────────────
            SectionCard(emoji = "⏰", title = "Time") {
                // Large time display — tap to open picker
                val is24Hour = android.text.format.DateFormat.is24HourFormat(context)
                val pattern = if (is24Hour) "HH:mm" else "hh:mm a"
                val timeText = time.format(DateTimeFormatter.ofPattern(pattern))
                OutlinedButton(
                    onClick = {
                        timePickerState = TimePickerState(
                            initialHour   = time.hour,
                            initialMinute = time.minute,
                            is24Hour      = is24Hour
                        )
                        showTimePicker = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text       = timeText,
                        fontSize   = 42.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text  = firesInLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    // Preview alarm sound button
                    OutlinedButton(
                        onClick = { togglePreview() },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            if (isPreviewPlaying) Icons.Default.Close else Icons.Default.PlayArrow,
                            contentDescription = if (isPreviewPlaying) "Stop preview" else "Preview alarm",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            if (isPreviewPlaying) "Stop" else "Preview",
                            fontSize = 13.sp
                        )
                    }
                }
                OutlinedTextField(
                    value         = label,
                    onValueChange = { label = it },
                    label         = { Text("Label (optional)") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )
            }

            // ── 📅 Repeat card ───────────────────────────────────────────
            SectionCard(emoji = "📅", title = "Repeat") {
                ImprovedDaySelector(
                    selectedDays    = daysOfWeek,
                    weekendDays     = weekendDays,
                    onSelectionChanged = { daysOfWeek = it }
                )
            }

            // ── 🔔 Wake-up card ──────────────────────────────────────────
            SectionCard(emoji = "🔔", title = "Wake-up") {
                
                // Sound Toggle
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Alarm Sound", fontWeight = FontWeight.Medium)
                        Text(if (isSoundEnabled) "Sound is on" else "Sound is off", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = isSoundEnabled, onCheckedChange = { isSoundEnabled = it })
                }

                // Vibration Toggle
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Vibrate", fontWeight = FontWeight.Medium)
                        Text(if (isVibrate) "Haptic feedback enabled" else "Silent wake", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = isVibrate, onCheckedChange = { isVibrate = it })
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Sound Selection (Only if sound enabled)
                AnimatedVisibility(visible = isSoundEnabled) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                                val existingUri = soundUri?.let { Uri.parse(it) } ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existingUri)
                            }
                            ringtonePickerLauncher.launch(intent)
                        }.padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Select Sound", fontWeight = FontWeight.Medium)
                            Text(soundName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                        Icon(Icons.Default.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                if (isSoundEnabled) Divider(modifier = Modifier.padding(bottom = 8.dp))

                // Unified Gentle Wake (Visible if either sound OR vibrate is enabled)
                AnimatedVisibility(visible = isSoundEnabled || isVibrate) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Gentle Wake", fontWeight = FontWeight.Medium)
                                Text("Slowly ramp up intensity", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(checked = isGentleWake, onCheckedChange = { isGentleWake = it })
                        }

                        AnimatedVisibility(visible = isGentleWake, enter = expandVertically(), exit = shrinkVertically()) {
                            Column(modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Crescendo duration", style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        if (crescendoDuration == 0) "Instant" else "${crescendoDuration} min",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Slider(
                                    value = crescendoDuration.toFloat(),
                                    onValueChange = { crescendoDuration = it.toInt() },
                                    valueRange = 0f..20f,
                                    steps = 19
                                )
                            }
                        }
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                    }
                }

                Divider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Smooth Fade-Out", fontWeight = FontWeight.Medium)
                        Text("Gradually fade sound on dismiss or snooze", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = isSmoothFadeOut, onCheckedChange = { isSmoothFadeOut = it })
                }

                Divider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Snooze duration", fontWeight = FontWeight.Medium)
                    Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.secondaryContainer) {
                        Text(
                            text     = if (snoozeDuration == 0) "Off" else "$snoozeDuration min",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style    = MaterialTheme.typography.labelLarge,
                            color    = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                Slider(
                    value = snoozeDuration.toFloat(),
                    onValueChange = { 
                        snoozeDuration = it.toInt()
                    },
                    valueRange = 1f..60f,
                    steps = 58
                )
                
                AnimatedVisibility(visible = snoozeDuration > 0, enter = expandVertically(), exit = shrinkVertically()) {
                    Column {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Evasive Snooze", fontWeight = FontWeight.Medium)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    SettingHelpIcon(title = "Evasive Snooze", content = "The Snooze button will randomly jump around the screen each time you try to press it, forcing you to pay attention.")
                                }
                                Text("Snooze button jumps away to wake you up", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(checked = isEvasiveSnooze, onCheckedChange = { isEvasiveSnooze = it })
                        }
                        
                        AnimatedVisibility(visible = isEvasiveSnooze, enter = expandVertically(), exit = shrinkVertically()) {
                            Column(modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Starts moving after", style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        if (evasiveSnoozesBeforeMoving == 0) "1st snooze" else "${evasiveSnoozesBeforeMoving + 1} snoozes",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Slider(
                                    value = evasiveSnoozesBeforeMoving.toFloat(),
                                    onValueChange = { evasiveSnoozesBeforeMoving = it.toInt() },
                                    valueRange = 0f..5f,
                                    steps = 4
                                )
                            }
                        }
                    }
                }
            }

            // ── 🧩 Dismissal challenge card ──────────────────────────────
            SectionCard(emoji = "🧩", title = "Dismissal challenge") {

                // Math challenge row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Math challenge", fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.width(8.dp))
                            SettingHelpIcon(title = "Math Challenge", content = "Requires you to solve math problems before you can dismiss the alarm. You can adjust the difficulty and the number of problems.")
                        }
                        Text("Solve a problem to dismiss", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = mathEnabled,
                        onCheckedChange = {
                            mathEnabled = it
                            if (!it) mathDifficulty = 0
                            else if (mathDifficulty == 0) mathDifficulty = 1
                        }
                    )
                }

                AnimatedVisibility(visible = mathEnabled && !smileToDismiss, enter = expandVertically(), exit = shrinkVertically()) {
                    Column {
                        MathDifficultyChips(
                            difficulty = mathDifficulty,
                            onSelected = { mathDifficulty = it }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Number of problems: $mathProblemCount", style = MaterialTheme.typography.bodyMedium)
                        }
                        Slider(
                            value = mathProblemCount.toFloat(),
                            onValueChange = { mathProblemCount = it.toInt() },
                            valueRange = 1f..10f,
                            steps = 8
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Gradually Increase Difficulty", fontWeight = FontWeight.Medium)
                                Text("Starts easy and gets harder up to your selected level", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = mathGraduallyIncreaseDifficulty,
                                onCheckedChange = { mathGraduallyIncreaseDifficulty = it }
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                Divider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Face game", fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.width(8.dp))
                            SettingHelpIcon(title = "Face Game", content = "Uses the front camera to detect your face and asks you to mimic 3 random facial expressions (like smiling or winking) to prove you're awake.")
                        }
                        Text("3 random face challenges to dismiss", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = smileToDismiss,
                        onCheckedChange = { 
                            smileToDismiss = it 
                            if (it && smileFallbackMethod == "MATH" && mathDifficulty == 0) {
                                mathDifficulty = 1
                            }
                        }
                    )
                }
                
                AnimatedVisibility(visible = smileToDismiss, enter = expandVertically(), exit = shrinkVertically()) {
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        Text(
                            text = "Fallback Method",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "If face detection fails, use this method:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            val fallbackOptions = listOf("NONE", "MATH")
                            fallbackOptions.forEachIndexed { index, option ->
                                SegmentedButton(
                                    shape = SegmentedButtonDefaults.itemShape(index = index, count = fallbackOptions.size),
                                    onClick = { 
                                        smileFallbackMethod = option 
                                        if (option == "MATH" && mathDifficulty == 0) {
                                            mathDifficulty = 1
                                        }
                                    },
                                    selected = smileFallbackMethod == option,
                                    label = { Text(if (option == "NONE") "None" else "Math") }
                                )
                            }
                        }
                        
                        AnimatedVisibility(visible = smileFallbackMethod == "MATH", enter = expandVertically(), exit = shrinkVertically()) {
                            Column(modifier = Modifier.padding(top = 8.dp)) {
                                Text("Fallback Math Difficulty", style = MaterialTheme.typography.bodyMedium)
                                MathDifficultyChips(
                                    difficulty = mathDifficulty,
                                    onSelected = { mathDifficulty = it }
                                )
                            }
                        }
                    }
                }

                Divider()

                // Smart Wakeup Check row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Smart Wakeup Check", fontWeight = FontWeight.Medium)
                        Text("Requires a response 3 mins after dismissal", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = isSmartWakeupEnabled, onCheckedChange = { isSmartWakeupEnabled = it })
                }

                AnimatedVisibility(visible = isSmartWakeupEnabled, enter = expandVertically(), exit = shrinkVertically()) {
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Delay before check", style = MaterialTheme.typography.bodyMedium)
                            Text("${wakeupCheckDelayMinutes} min", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        }
                        Slider(
                            value = wakeupCheckDelayMinutes.toFloat(),
                            onValueChange = { wakeupCheckDelayMinutes = it.toInt() },
                            valueRange = 1f..10f,
                            steps = 8
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Response timeout", style = MaterialTheme.typography.bodyMedium)
                            Text("${wakeupCheckTimeoutSeconds}s", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        }
                        Slider(
                            value = wakeupCheckTimeoutSeconds.toFloat(),
                            onValueChange = { wakeupCheckTimeoutSeconds = it.toInt() },
                            valueRange = 10f..120f,
                            steps = 10
                        )
                    }
                }
            }

            // ── 🧠 Alarm-Pal Intelligence card ──────────────────────────
            SectionCard(emoji = "🧠", title = "Alarm-Pal Intelligence") {
                // Wake-up Briefing (Gen AI element)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Wake-up Briefing", fontWeight = FontWeight.Medium)
                        Text("Personalized AI briefing when you wake up", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = isBriefingEnabled, onCheckedChange = { isBriefingEnabled = it })
                }
                
                AnimatedVisibility(visible = isBriefingEnabled, enter = expandVertically(), exit = shrinkVertically()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Read Aloud (TTS)", fontWeight = FontWeight.Medium)
                            Text("Speak the briefing out loud", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = isTtsEnabled, onCheckedChange = { isTtsEnabled = it })
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Accountability Buddy
                AccountabilityBuddyContent(
                    enabled               = buddyEnabled,
                    onEnabledChange       = { buddyEnabled = it },
                    phoneNumber           = buddyPhone,
                    onPhoneNumberChange   = { buddyPhone = it },
                    contactName           = buddyName,
                    onContactNameChange   = { buddyName = it },
                    userName              = buddyUserName,
                    onUserNameChange      = { buddyUserName = it },
                    customMessage         = buddyMessage,
                    onCustomMessageChange = { buddyMessage = it },
                    alertDelayMinutes     = buddyAlertDelay,
                    onAlertDelayChange    = { buddyAlertDelay = it },
                    alarmLabel            = label.ifBlank { "your alarm" }
                )
            }

            // ── Save button ──────────────────────────────────────────────
            val coroutineScope = rememberCoroutineScope()
            Button(
                onClick = {
                    val newAlarm = currentAlarm?.copy(
                        time                  = time,
                        label                 = label,
                        isGentleWake          = isGentleWake,
                        crescendoDurationMinutes = crescendoDuration,
                        buddyPhoneNumber      = if (buddyEnabled) buddyPhone else null,
                        buddyAlertDelayMinutes = buddyAlertDelay,
                        buddyName             = if (buddyEnabled) buddyName.ifBlank { null } else null,
                        userName              = buddyUserName.ifBlank { null },
                        buddyMessage          = buddyMessage.ifBlank { null },
                        daysOfWeek            = daysOfWeek,
                        mathDifficulty        = mathDifficulty,
                        smileToDismiss        = smileToDismiss,
                        snoozeDurationMinutes = snoozeDuration,
                        isBriefingEnabled     = isBriefingEnabled,
                        isTtsEnabled          = isTtsEnabled,
                        isEvasiveSnooze       = isEvasiveSnooze,
                        evasiveSnoozesBeforeMoving = evasiveSnoozesBeforeMoving,
                        soundUri              = soundUri,
                        isSmoothFadeOut       = isSmoothFadeOut,
                        isVibrate             = isVibrate,
                        isSoundEnabled        = isSoundEnabled,
                        isSmartWakeupEnabled  = isSmartWakeupEnabled,
                        wakeupCheckDelayMinutes = wakeupCheckDelayMinutes,
                        wakeupCheckTimeoutSeconds = wakeupCheckTimeoutSeconds
                    ) ?: Alarm(
                        time                  = time,
                        label                 = label,
                        isGentleWake          = isGentleWake,
                        crescendoDurationMinutes = crescendoDuration,
                        buddyPhoneNumber      = if (buddyEnabled) buddyPhone else null,
                        buddyAlertDelayMinutes = buddyAlertDelay,
                        buddyName             = if (buddyEnabled) buddyName.ifBlank { null } else null,
                        userName              = buddyUserName.ifBlank { null },
                        buddyMessage          = buddyMessage.ifBlank { null },
                        daysOfWeek            = daysOfWeek,
                        mathDifficulty        = mathDifficulty,
                        smileToDismiss        = smileToDismiss,
                        snoozeDurationMinutes = snoozeDuration,
                        isBriefingEnabled     = isBriefingEnabled,
                        isTtsEnabled          = isTtsEnabled,
                        isEvasiveSnooze       = isEvasiveSnooze,
                        evasiveSnoozesBeforeMoving = evasiveSnoozesBeforeMoving,
                        soundUri              = soundUri,
                        isVibrate             = isVibrate,
                        isSoundEnabled        = isSoundEnabled,
                        isSmartWakeupEnabled  = isSmartWakeupEnabled,
                        wakeupCheckDelayMinutes = wakeupCheckDelayMinutes,
                        wakeupCheckTimeoutSeconds = wakeupCheckTimeoutSeconds
                    )
                    viewModel.addAlarm(newAlarm)
                    onNavigateUp()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text("Save alarm", fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    // Time picker dialog — state is seeded from current `time` at open-time, so
    // editing an existing alarm always shows its saved time rather than "now"
    val tps = timePickerState
    if (showTimePicker && tps != null) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Set time") },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = tps)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    time = LocalTime.of(tps.hour, tps.minute)
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Section card wrapper
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionCard(
    emoji: String,
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(emoji, fontSize = 18.sp)
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            content()
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Day selector — Sunday-first, 2-letter abbreviations, quick-select row
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImprovedDaySelector(
    selectedDays: List<Int>,
    weekendDays: Set<Int>,
    onSelectionChanged: (List<Int>) -> Unit
) {
    // ISO 8601 indexing to match AndroidAlarmScheduler: Mon=1 … Sat=6, Sun=7
    // We display Sunday first (US convention) but internally store Sun as 7
    data class Day(val isoIndex: Int, val label: String)
    val days = listOf(
        Day(7, "Su"), Day(1, "Mo"), Day(2, "Tu"), Day(3, "We"),
        Day(4, "Th"), Day(5, "Fr"), Day(6, "Sa")
    )
    val everyday = listOf(1, 2, 3, 4, 5, 6, 7)
    val weekend  = everyday.filter { weekendDays.contains(it) }.sorted()
    val weekdays = everyday.filter { !weekendDays.contains(it) }.sorted()

    // Quick-select row
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        listOf(
            "Weekdays"  to weekdays,
            "Weekend"   to weekend,
            "Every day" to everyday
        ).forEach { (label, target) ->
            val isActive = selectedDays.sorted() == target.sorted()
            Surface(
                selected = isActive,
                onClick = { onSelectionChanged(if (isActive) emptyList() else target) },
                shape = 
                    RoundedCornerShape(8.dp),
                color = if (isActive) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                border = if (isActive) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.weight(1f).height(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isActive) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // Day chips — Sunday first with fixed height to prevent text wrapping
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        days.forEach { day ->
            val isSelected = selectedDays.contains(day.isoIndex)
            Surface(
                selected = isSelected,
                onClick = {
                    val new = if (isSelected) selectedDays - day.isoIndex else selectedDays + day.isoIndex
                    onSelectionChanged(new.sorted())
                },
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.weight(1f).height(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = day.label,
                        fontSize = 11.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Clip,
                        color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Snooze duration chips
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnoozeChips(selectedMinutes: Int, onSelected: (Int) -> Unit) {
    // This function is still here for backwards compatibility, but not used in the UI anymore
}

// ─────────────────────────────────────────────────────────────────────────────
// Math difficulty chips
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MathDifficultyChips(difficulty: Int, onSelected: (Int) -> Unit) {
    val options = listOf(1 to "Easy", 2 to "Medium", 3 to "Hard")
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
    ) {
        options.forEach { (level, label) ->
            Surface(
                selected = difficulty == level,
                onClick = { onSelected(level) },
                shape = RoundedCornerShape(8.dp),
                color = if (difficulty == level) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                border = if (difficulty == level) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.weight(1f).height(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (difficulty == level) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Accountability Buddy card (unchanged logic, kept here)
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountabilityBuddyContent(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    phoneNumber: String,
    onPhoneNumberChange: (String) -> Unit,
    contactName: String,
    onContactNameChange: (String) -> Unit,
    userName: String,
    onUserNameChange: (String) -> Unit,
    customMessage: String,
    onCustomMessageChange: (String) -> Unit,
    alertDelayMinutes: Int,
    onAlertDelayChange: (Int) -> Unit,
    alarmLabel: String
) {
    val context = LocalContext.current
    var hasSmsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
        )
    }
    // SMS rationale dialog: show this BEFORE the system permission dialog
    var showSmsRationale by remember { mutableStateOf(false) }
    // Opt-in request state
    var optInSent by remember { mutableStateOf(false) }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasSmsPermission = granted
        if (granted) onEnabledChange(true)
    }

    // SMS rationale dialog
    if (showSmsRationale) {
        AlertDialog(
            onDismissRequest = { showSmsRationale = false },
            title = { Text("📱 SMS Permission Required") },
            text = {
                Text(
                    "AlarmPal needs to send SMS messages on your behalf to:\n\n" +
                    "• Invite your buddy to opt in as your accountability partner\n" +
                    "• Alert them if you miss an alarm\n\n" +
                    "You control when messages are sent. No spam, ever."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showSmsRationale = false
                    smsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
                }) { Text("Allow SMS") }
            },
            dismissButton = {
                TextButton(onClick = { showSmsRationale = false }) { Text("Not Now") }
            }
        )
    }

    val contactPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickContact()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        context.contentResolver.query(uri, arrayOf(ContactsContract.Contacts.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) onContactNameChange(cursor.getString(0) ?: "")
        }
        context.contentResolver.query(uri, arrayOf(ContactsContract.Contacts._ID), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val contactId = cursor.getString(0)
                context.contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                    "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                    arrayOf(contactId), null
                )?.use { phoneCursor ->
                    if (phoneCursor.moveToFirst()) onPhoneNumberChange(phoneCursor.getString(0) ?: "")
                }
            }
        }
    }

    val alarmDesc      = if (alarmLabel.isNotBlank()) "\"$alarmLabel\"" else "an alarm"
    val whoText        = if (userName.isNotBlank()) userName else "Someone"
    val defaultMessage = "⏰ $whoText missed $alarmDesc! Please check in — they might need a wake-up call."
    val smsPreview     = if (customMessage.isNotBlank())
        customMessage.replace("{name}", userName.ifBlank { "they" })
    else defaultMessage
    val hasContact = contactName.isNotBlank()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Accountability Buddy", fontWeight = FontWeight.Medium)
                Text(
                    if (hasContact && enabled) contactName else "Text someone if you miss this alarm",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (hasContact && enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = { shouldEnable ->
                    if (shouldEnable) {
                        if (hasSmsPermission) {
                            onEnabledChange(true)
                        } else {
                            // Show rationale BEFORE the system dialog
                            showSmsRationale = true
                        }
                    } else {
                        onEnabledChange(false)
                    }
                }
            )
        }

        AnimatedVisibility(visible = enabled, enter = expandVertically(), exit = shrinkVertically()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Divider(modifier = Modifier.padding(vertical = 4.dp))

                if (hasContact) {
                    InputChip(
                        selected = true,
                        onClick  = {},
                        label    = { Text(contactName) },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null,
                                modifier = Modifier.size(InputChipDefaults.AvatarSize))
                        },
                        trailingIcon = {
                            IconButton(
                                onClick  = { onContactNameChange(""); onPhoneNumberChange("") },
                                modifier = Modifier.size(InputChipDefaults.AvatarSize)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remove contact",
                                    modifier = Modifier.size(16.dp))
                            }
                        }
                    )
                } else {
                    OutlinedButton(
                        onClick  = { contactPickerLauncher.launch(null) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Pick from Contacts")
                    }
                }

                OutlinedTextField(
                    value         = phoneNumber,
                    onValueChange = onPhoneNumberChange,
                    label         = { Text(if (hasContact) "Phone Number" else "Or type a phone number") },
                    placeholder   = { Text("+1 234 567 8901") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value         = userName,
                    onValueChange = onUserNameChange,
                    label         = { Text("Your name") },
                    placeholder   = { Text("Alex") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value         = customMessage.ifBlank { defaultMessage },
                    onValueChange = { typed ->
                        onCustomMessageChange(if (typed == defaultMessage) "" else typed)
                    },
                    label         = { Text("Message") },
                    supportingText = { Text("Use {name} as a placeholder for your name",
                        style = MaterialTheme.typography.labelSmall) },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Alert after", style = MaterialTheme.typography.bodyMedium)
                    Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.secondaryContainer) {
                        Text(
                            text     = "$alertDelayMinutes min",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style    = MaterialTheme.typography.labelLarge,
                            color    = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                Slider(
                    value         = alertDelayMinutes.toFloat(),
                    onValueChange = { onAlertDelayChange(it.toInt()) },
                    valueRange    = 1f..30f,
                    steps         = 29
                )

                Surface(
                    shape    = MaterialTheme.shapes.medium,
                    color    = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Message preview", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(smsPreview, style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic)
                    }
                }

                if (!hasSmsPermission) {
                    Text(
                        "⚠️ SMS permission required to send messages",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                // Buddy opt-in section — shown when phone number is entered
                if (phoneNumber.isNotBlank() && hasSmsPermission) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (optInSent) "⏳ Opt-in requested" else "📲 Notify buddy",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium,
                                color = if (optInSent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (optInSent)
                                    "Opt-in request sent. They're on the team!"
                                else
                                    "Send them an invite SMS so they know what to expect.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (!optInSent) {
                            OutlinedButton(
                                onClick = {
                                    // Send opt-in SMS directly via SmsManager
                                    val smsManager: android.telephony.SmsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                                        val subId = android.telephony.SubscriptionManager.getDefaultSmsSubscriptionId()
                                        context.getSystemService(android.telephony.SmsManager::class.java)
                                            .createForSubscriptionId(subId)
                                    } else {
                                        @Suppress("DEPRECATION")
                                        android.telephony.SmsManager.getDefault()
                                    }
                                    val buddyDisplayName = contactName.takeIf { it.isNotBlank() } ?: "there"
                                    val userDisplayName = userName.takeIf { it.isNotBlank() } ?: "Someone"
                                    val optInMsg = "⏰ Hey $buddyDisplayName, $userDisplayName just added you as their AlarmPal " +
                                        "accountability buddy! If they miss an alarm, you'll get a text to check in. " +
                                        "Text $userDisplayName directly to opt out. — AlarmPal"
                                    try {
                                        val parts = smsManager.divideMessage(optInMsg)
                                        if (parts.size == 1) {
                                            smsManager.sendTextMessage(phoneNumber, null, optInMsg, null, null)
                                        } else {
                                            smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
                                        }
                                        optInSent = true
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(context, "SMS failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.padding(start = 12.dp)
                            ) {
                                Text("Send")
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Legacy DaySelector kept for backward compatibility (not used in new screen)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun DaySelector(selectedDays: List<Int>, onSelectionChanged: (List<Int>) -> Unit) {
    ImprovedDaySelector(selectedDays = selectedDays, weekendDays = setOf(6, 7), onSelectionChanged = onSelectionChanged)
}

/** Helper data class to capture the state of an alarm for change detection */
private data class AlarmStateSnapshot(
    val time: LocalTime,
    val label: String,
    val isGentleWake: Boolean,
    val buddyPhone: String,
    val buddyName: String,
    val buddyUserName: String,
    val buddyMessage: String,
    val buddyEnabled: Boolean,
    val buddyAlertDelay: Int,
    val daysOfWeek: List<Int>,
    val mathDifficulty: Int,
    val mathProblemCount: Int,
    val mathGraduallyIncreaseDifficulty: Boolean,
    val mathEnabled: Boolean,
    val smileToDismiss: Boolean,
    val smileFallbackMethod: String,
    val snoozeDuration: Int,
    val crescendoDuration: Int,
    val isBriefingEnabled: Boolean,
    val isTtsEnabled: Boolean,
    val isEvasiveSnooze: Boolean,
    val evasiveSnoozesBeforeMoving: Int,
    val isSmoothFadeOut: Boolean,
    val isVibrate: Boolean,
    val isSoundEnabled: Boolean,
    val soundUri: String?,
    val isSmartWakeupEnabled: Boolean,
    val wakeupCheckDelayMinutes: Int,
    val wakeupCheckTimeoutSeconds: Int
)
