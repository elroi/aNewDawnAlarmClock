package com.elroi.alarmpal.ui.screen.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.res.painterResource
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.media.RingtoneManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.elroi.alarmpal.R
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.shape.CircleShape
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import com.elroi.alarmpal.domain.manager.GeminiNanoStatus
import com.elroi.alarmpal.ui.screen.alarm.MathDifficultyChips
import com.elroi.alarmpal.ui.components.BuddySelectionDialog
import com.elroi.alarmpal.ui.components.SettingHelpIcon
import com.elroi.alarmpal.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateUp: () -> Unit,
    onNavigateToHelp: () -> Unit = {},
    onNavigateToOnboarding: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val location by viewModel.location.collectAsState()
    val isCelsius by viewModel.isCelsius.collectAsState()
    val isAutoLocation by viewModel.isAutoLocation.collectAsState()
    val alarmDefaults by viewModel.alarmDefaults.collectAsState()
    val hasChanges by viewModel.hasChanges.collectAsState()
    var showDiscardDialog by remember { mutableStateOf(false) }
    var defaultSoundName by remember { mutableStateOf("Default") }
    val context = LocalContext.current
    var showAdvancedExperience by remember { mutableStateOf(false) }
    var showAdvancedMath by remember { mutableStateOf(false) }
    var showEditPrompts by remember { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                viewModel.updateIsAutoLocation(true, context)
            } else {
                viewModel.updateIsAutoLocation(false)
                android.widget.Toast.makeText(context, "Location permission is required for auto-detect", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    )
    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri: Uri? = result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            viewModel.updateAlarmDefaults(alarmDefaults.copy(defaultSoundUri = uri?.toString()))
        }
    }

    LaunchedEffect(alarmDefaults.defaultSoundUri) {
        if (alarmDefaults.defaultSoundUri != null) {
            try {
                val ringtone = RingtoneManager.getRingtone(context, Uri.parse(alarmDefaults.defaultSoundUri))
                defaultSoundName = ringtone?.getTitle(context) ?: "Unknown"
            } catch (e: Exception) {
                defaultSoundName = "Custom Sound"
            }
        } else {
            defaultSoundName = "Default"
        }
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

    val onWipeBrainMemory = {
        viewModel.wipeBrainMemory()
        android.widget.Toast.makeText(context, "Brain memory wiped", android.widget.Toast.LENGTH_SHORT).show()
    }

    val onWipeAllData = {
        viewModel.wipeAllData()
        android.widget.Toast.makeText(context, "All data wiped", android.widget.Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (hasChanges) {
                            showDiscardDialog = true
                        } else {
                            onNavigateUp()
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToHelp) {
                        Icon(Icons.Default.Info, contentDescription = "Help")
                    }
                    TextButton(onClick = {
                        viewModel.saveSettings()
                        onNavigateUp()
                    }) {
                        Text("SAVE", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // --- 1. MORNING EXPERIENCE 🌅 ---
            Text(
                text = "Morning Experience 🌅",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            // A. Companion Personality
            Text(
                "Companion Personality",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            val personas = listOf(
                PersonaInfo("COACH", "🪖 The Drill Sergeant", "No excuses. Just results. Move it!", Icons.Default.Info, MaterialTheme.colorScheme.error),
                PersonaInfo("COMEDIAN", "🤡 The Sarcastic Friend", "Oh, you're awake. Impressive.", Icons.Default.Face, MaterialTheme.colorScheme.secondary),
                PersonaInfo("ZEN", "🧘 The Zen Master", "Breathe. The day awaits, mindfully.", Icons.Default.Favorite, MaterialTheme.colorScheme.tertiary),
                PersonaInfo("HYPEMAN", "🚀 The Hype-Man", "Let's gooo! You got this!", Icons.Default.Notifications, MaterialTheme.colorScheme.primary),
                PersonaInfo("SURPRISE", "🎲 Surprise Me", "A random personality every morning.", Icons.Default.Refresh, MaterialTheme.colorScheme.outline)
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                personas.filter { it.id != "SURPRISE" }.forEach { persona ->
                    PersonaCard(
                        info = persona,
                        isSelected = alarmDefaults.aiPersona == persona.id && !alarmDefaults.aiPersonaSurprise,
                        onClick = {
                            viewModel.updateAlarmDefaults(alarmDefaults.copy(aiPersona = persona.id, aiPersonaSurprise = false))
                        }
                    )
                }
                
                // Surprise Mode Toggle (as a card)
                Surface(
                    onClick = { viewModel.updateAlarmDefaults(alarmDefaults.copy(aiPersonaSurprise = !alarmDefaults.aiPersonaSurprise)) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (alarmDefaults.aiPersonaSurprise) MaterialTheme.colorScheme.outline.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                    border = if (alarmDefaults.aiPersonaSurprise) BorderStroke(2.dp, MaterialTheme.colorScheme.outline) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("🎲 Surprise Me", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("A random personality every morning.", style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(checked = alarmDefaults.aiPersonaSurprise, onCheckedChange = { viewModel.updateAlarmDefaults(alarmDefaults.copy(aiPersonaSurprise = it)) })
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // B. Aura Report Content
            Text(
                "Aura Report Content",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Read Aloud (TTS)", style = MaterialTheme.typography.bodyLarge)
                        Switch(checked = alarmDefaults.isTtsEnabled, onCheckedChange = { viewModel.updateAlarmDefaults(alarmDefaults.copy(isTtsEnabled = it)) })
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Weather Report", style = MaterialTheme.typography.bodyLarge)
                        Switch(checked = alarmDefaults.briefingIncludeWeather, onCheckedChange = { viewModel.updateAlarmDefaults(alarmDefaults.copy(briefingIncludeWeather = it)) })
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Calendar Events", style = MaterialTheme.typography.bodyLarge)
                        Switch(checked = alarmDefaults.briefingIncludeCalendar, onCheckedChange = { viewModel.updateAlarmDefaults(alarmDefaults.copy(briefingIncludeCalendar = it)) })
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Fun Fact", style = MaterialTheme.typography.bodyLarge)
                        Switch(checked = alarmDefaults.briefingIncludeFact, onCheckedChange = { viewModel.updateAlarmDefaults(alarmDefaults.copy(briefingIncludeFact = it)) })
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { 
                    viewModel.saveSettings()
                    viewModel.launchTestBriefing() 
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Preview Briefing")
            }

            Spacer(modifier = Modifier.height(12.dp))

            // C. Persona Tuning (Collapsed)
            Surface(
                onClick = { showAdvancedExperience = !showAdvancedExperience },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Advanced Customization", style = MaterialTheme.typography.titleMedium)
                    Icon(if (showAdvancedExperience) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null)
                }
            }
            
            AnimatedVisibility(visible = showAdvancedExperience) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    val userName by viewModel.userName.collectAsState()
                    OutlinedTextField(
                        value = userName,
                        onValueChange = { viewModel.updateUserName(it) },
                        label = { Text(stringResource(R.string.onboarding_3_label_name)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { showEditPrompts = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Customize Persona Prompts")
                    }
                }
            }

            if (showEditPrompts) {
                EditPromptsDialog(
                    alarmDefaults = alarmDefaults,
                    onDismiss = { showEditPrompts = false },
                    onSave = { coach, comedian, zen, hypeman ->
                        viewModel.updateAlarmDefaults(alarmDefaults.copy(
                            promptCoach = coach,
                            promptComedian = comedian,
                            promptZen = zen,
                            promptHypeman = hypeman
                        ))
                        showEditPrompts = false
                    }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // --- 2. WAKE-UP ENGINE ⚙️ ---
            Text(
                text = "Wake-Up Engine ⚙️",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // A. Alarm Creation
            Text("Alarm Creation", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            val creationStyle by viewModel.alarmCreationStyle.collectAsState()
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                val styles = listOf("WIZARD", "SIMPLE")
                styles.forEachIndexed { index, style ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = styles.size),
                        onClick = { viewModel.updateAlarmCreationStyle(style) },
                        selected = creationStyle == style,
                        label = { Text(if (style == "WIZARD") "Guided Wizard" else "Simple Setup", fontSize = 12.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // B. Default Alarm Behavior
            Text("Default Alarm Behavior", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            
            // Audio & Haptics
            Text("Audio & Haptics", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Alarm Sound", style = MaterialTheme.typography.bodyLarge)
                Switch(checked = alarmDefaults.isSoundEnabled, onCheckedChange = { viewModel.updateAlarmDefaults(alarmDefaults.copy(isSoundEnabled = it)) })
            }
            AnimatedVisibility(visible = alarmDefaults.isSoundEnabled) {
                Surface(
                    onClick = {
                        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                            val existingUri = alarmDefaults.defaultSoundUri?.let { Uri.parse(it) } ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existingUri)
                        }
                        ringtonePickerLauncher.launch(intent)
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Sound Source", fontWeight = FontWeight.Medium)
                        Text(defaultSoundName, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Vibrate", style = MaterialTheme.typography.bodyLarge)
                Switch(checked = alarmDefaults.isVibrate, onCheckedChange = { viewModel.updateAlarmDefaults(alarmDefaults.copy(isVibrate = it)) })
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                val isOutputEnabled = alarmDefaults.isSoundEnabled || alarmDefaults.isVibrate
                val isGentleEnabled = alarmDefaults.isGentleWake && isOutputEnabled
                
                Column(modifier = Modifier.weight(1f)) {
                    Text("Gentle Wake", style = MaterialTheme.typography.bodyLarge, color = if (isOutputEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f))
                    if (!isOutputEnabled) {
                        Text("Requires Sound or Vibrate", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    }
                }
                Switch(
                    checked = isGentleEnabled,
                    enabled = isOutputEnabled,
                    onCheckedChange = { viewModel.updateAlarmDefaults(alarmDefaults.copy(isGentleWake = it)) }
                )
            }
            AnimatedVisibility(visible = alarmDefaults.isGentleWake && (alarmDefaults.isSoundEnabled || alarmDefaults.isVibrate)) {
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Crescendo Duration", style = MaterialTheme.typography.bodySmall)
                        Text("${alarmDefaults.crescendoDurationMinutes} min", color = MaterialTheme.colorScheme.primary)
                    }
                    Slider(
                        value = alarmDefaults.crescendoDurationMinutes.toFloat(),
                        onValueChange = { viewModel.updateAlarmDefaults(alarmDefaults.copy(crescendoDurationMinutes = it.toInt())) },
                        valueRange = 0f..20f,
                        steps = 19
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Smooth Fade-Out", style = MaterialTheme.typography.bodyLarge)
                Switch(checked = alarmDefaults.isSmoothFadeOut, onCheckedChange = { viewModel.updateAlarmDefaults(alarmDefaults.copy(isSmoothFadeOut = it)) })
            }

            // Snooze & Dismissal
            Text("Snooze & Dismissal", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 12.dp))
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Snooze Duration", style = MaterialTheme.typography.bodyLarge)
                    Text(if (alarmDefaults.snoozeDurationMinutes == 0) "Disabled" else "${alarmDefaults.snoozeDurationMinutes} min", color = if (alarmDefaults.snoozeDurationMinutes == 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                }
                Slider(
                    value = alarmDefaults.snoozeDurationMinutes.toFloat(),
                    onValueChange = { viewModel.updateAlarmDefaults(alarmDefaults.copy(snoozeDurationMinutes = it.toInt())) },
                    valueRange = 0f..60f,
                    steps = 59
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Evasive Snooze", style = MaterialTheme.typography.bodyLarge)
                Switch(checked = alarmDefaults.isEvasiveSnooze, onCheckedChange = { viewModel.updateAlarmDefaults(alarmDefaults.copy(isEvasiveSnooze = it)) })
            }
            AnimatedVisibility(visible = alarmDefaults.isEvasiveSnooze) {
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Movement Threshold", style = MaterialTheme.typography.bodySmall)
                        Text("${alarmDefaults.evasiveSnoozesBeforeMoving + 1} snoozes", color = MaterialTheme.colorScheme.primary)
                    }
                    Slider(
                        value = alarmDefaults.evasiveSnoozesBeforeMoving.toFloat(),
                        onValueChange = { viewModel.updateAlarmDefaults(alarmDefaults.copy(evasiveSnoozesBeforeMoving = it.toInt())) },
                        valueRange = 0f..5f,
                        steps = 4
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Math Challenge", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = alarmDefaults.mathDifficulty > 0,
                    onCheckedChange = { viewModel.updateAlarmDefaults(alarmDefaults.copy(mathDifficulty = if (it) 1 else 0)) }
                )
            }
            AnimatedVisibility(visible = alarmDefaults.mathDifficulty > 0) {
                Column {
                    MathDifficultyChips(
                        difficulty = alarmDefaults.mathDifficulty,
                        onSelected = { viewModel.updateAlarmDefaults(alarmDefaults.copy(mathDifficulty = it)) }
                    )
                    Surface(
                        onClick = { showAdvancedMath = !showAdvancedMath },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        color = Color.Transparent
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Advanced Math Options", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            Icon(if (showAdvancedMath) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    AnimatedVisibility(visible = showAdvancedMath) {
                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Problem Count", style = MaterialTheme.typography.bodySmall)
                                Text("${alarmDefaults.mathProblemCount}", color = MaterialTheme.colorScheme.primary)
                            }
                            Slider(
                                value = alarmDefaults.mathProblemCount.toFloat(),
                                onValueChange = { viewModel.updateAlarmDefaults(alarmDefaults.copy(mathProblemCount = it.toInt())) },
                                valueRange = 1f..10f,
                                steps = 8
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Gradual Difficulty", style = MaterialTheme.typography.bodySmall)
                                Switch(checked = alarmDefaults.mathGraduallyIncreaseDifficulty, onCheckedChange = { viewModel.updateAlarmDefaults(alarmDefaults.copy(mathGraduallyIncreaseDifficulty = it)) })
                            }
                        }
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Face Game", style = MaterialTheme.typography.bodyLarge)
                Switch(checked = alarmDefaults.smileToDismiss, onCheckedChange = { viewModel.updateAlarmDefaults(alarmDefaults.copy(smileToDismiss = it)) })
            }
            AnimatedVisibility(visible = alarmDefaults.smileToDismiss) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text("Fallback Method", style = MaterialTheme.typography.bodySmall)
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        val options = listOf("NONE", "MATH")
                        options.forEachIndexed { index, opt ->
                            SegmentedButton(
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                                onClick = { viewModel.updateAlarmDefaults(alarmDefaults.copy(smileFallbackMethod = opt)) },
                                selected = alarmDefaults.smileFallbackMethod == opt,
                                label = { Text(if (opt == "NONE") "None" else "Math", fontSize = 12.sp) }
                            )
                        }
                    }
                }
            }

            // C. Day Groups
            Spacer(modifier = Modifier.height(16.dp))
            Text("Day Groups", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            val days = listOf(7 to "Su", 1 to "Mo", 2 to "Tu", 3 to "We", 4 to "Th", 5 to "Fr", 6 to "Sa")
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Select your weekend days", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    days.forEach { (idx, label) ->
                        val isWeekend = alarmDefaults.weekendDays.contains(idx)
                        Surface(
                            onClick = {
                                val newWeekend = if (isWeekend) alarmDefaults.weekendDays - idx else alarmDefaults.weekendDays + idx
                                viewModel.updateAlarmDefaults(alarmDefaults.copy(weekendDays = newWeekend))
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isWeekend) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.weight(1f).height(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(label, fontSize = 11.sp, color = if (isWeekend) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // --- 3. ACCOUNTABILITY 🤝 ---
            Text(
                text = "Accountability 🤝",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            BuddyManagementSection(viewModel)

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // --- 4. INTELLIGENCE (Cloud First) 💡 ---
            Text(
                text = "Intelligence 💡",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "LemurLoop uses Gemini for creative briefings. All processing happens in the cloud for maximum intelligence.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            IntelligenceHealthView(viewModel, onWipeBrainMemory)

            Spacer(modifier = Modifier.height(16.dp))

            // API Key
            Text("API Credentials", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            val apiKey by viewModel.geminiApiKey.collectAsState()
            OutlinedTextField(
                value = apiKey,
                onValueChange = { viewModel.updateGeminiApiKey(it) },
                label = { Text("Gemini API Key") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // --- 5. HELP & SYSTEM ℹ️ ---
            Text(
                text = "Help & System ℹ️",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    // About
                    Row(
                        modifier = Modifier.clickable { onNavigateToAbout() }.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("About LemurLoop", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Version 1.2.0 • Credits • Legal", style = MaterialTheme.typography.bodySmall)
                        }
                        Icon(Icons.Default.ArrowForward, contentDescription = null)
                    }
                    
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Replay Tutorial
                    Row(
                        modifier = Modifier.clickable { onNavigateToOnboarding() }.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Replay Tutorial", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("See the setup wizard again", style = MaterialTheme.typography.bodySmall)
                        }
                        Icon(Icons.Default.ArrowForward, contentDescription = null)
                    }

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Privacy Policy
                    val privacyPolicyContextInside = androidx.compose.ui.platform.LocalContext.current
                    Row(
                        modifier = Modifier.clickable { 
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://elroi.github.io/aNewDawnAlarmClock/privacy-policy/"))
                            privacyPolicyContextInside.startActivity(intent)
                        }.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Privacy Policy", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Your data stays on device", style = MaterialTheme.typography.bodySmall)
                        }
                        Icon(Icons.Default.ArrowForward, contentDescription = null)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Danger Zone
            var showDangerZone by remember { mutableStateOf(false) }
            OutlinedButton(
                onClick = { showDangerZone = !showDangerZone },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
            ) {
                Text(if (showDangerZone) "Hide Danger Zone" else "Show Danger Zone")
            }

            AnimatedVisibility(visible = showDangerZone) {
                Card(
                    modifier = Modifier.padding(top = 8.dp).fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Danger Zone", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        Text("Deleting all data is permanent and cannot be undone.", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onWipeAllData,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Wipe All App Data")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}


data class PersonaInfo(
    val id: String,
    val name: String,
    val description: String,
    val icon: ImageVector,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonaCard(
    info: PersonaInfo,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) info.color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
        border = if (isSelected) BorderStroke(2.dp, info.color) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = info.color.copy(alpha = 0.2f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = info.icon,
                        contentDescription = null,
                        tint = info.color,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = info.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) info.color else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = info.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = info.color
                )
            }
        }
    }
}

@Composable
fun IntelligenceHealthView(viewModel: SettingsViewModel, onWipeBrainMemory: () -> Unit) {
    val status by viewModel.briefingStatus.collectAsState()
    val error by viewModel.briefingError.collectAsState()
    val lastScript by viewModel.lastBriefingScript.collectAsState()
    val isGenerating by viewModel.isBriefingGenerating.collectAsState()
    var showFullError by remember { mutableStateOf(false) }
    var showFullScript by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Intelligence Health 🩺", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (isGenerating) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                }
            }
            
            val statusParts = status.split("|").associate { 
                val kv = it.split(":")
                kv[0] to (kv.getOrNull(1) ?: "unknown")
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HealthIndicator("Weather", statusParts["weather"] ?: "pending", Modifier.weight(1f))
                HealthIndicator("Calendar", statusParts["calendar"] ?: "pending", Modifier.weight(1f))
                HealthIndicator("AI Brain", statusParts["ai"] ?: "pending", Modifier.weight(1f))
            }

            error?.takeIf { it.isNotBlank() }?.let { e ->
                Text(
                    text = "Issue: ${e.take(80)}... (Tap for details)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showFullError = true }
                        .padding(vertical = 4.dp)
                )
            }

            lastScript?.takeIf { it.isNotBlank() }?.let { script ->
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showFullScript = true }
                ) {
                    Text(
                        text = "\"${if (script.length > 100) script.take(100) + "..." else script}\"",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontStyle = FontStyle.Italic
                    )
                }
            }

            if (showFullError) {
                error?.let { fullError ->
                    AlertDialog(
                        onDismissRequest = { showFullError = false },
                        title = { Text("Diagnostic Log 🔍") },
                        text = { 
                            val scroll = rememberScrollState()
                            Text(
                                text = fullError,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.verticalScroll(scroll)
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = { showFullError = false }) { Text("Close") }
                        }
                    )
                }
            }

            if (showFullScript) {
                lastScript?.let { script ->
                    AlertDialog(
                        onDismissRequest = { showFullScript = false },
                        title = { Text("Last Generated Briefing 📝") },
                        text = { 
                            val scroll = rememberScrollState()
                            Text(
                                text = script,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.verticalScroll(scroll)
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = { showFullScript = false }) { Text("Close") }
                        }
                    )
                }
            }

            Button(
                onClick = { 
                    viewModel.saveSettings()
                    viewModel.launchTestBriefing() 
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isGenerating,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Generate Test Briefing")
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            OutlinedButton(
                onClick = onWipeBrainMemory,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reset Brain")
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun HealthIndicator(name: String, status: String, modifier: Modifier = Modifier) {
    val color = when(status) {
        "ok" -> MaterialTheme.colorScheme.primary
        "fail" -> MaterialTheme.colorScheme.error
        "cached" -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.outline
    }
    
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color = color, shape = CircleShape)
        )
        Text(name, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
        Text(status, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun EditPromptsDialog(
    alarmDefaults: com.elroi.alarmpal.domain.manager.AlarmDefaults,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    var promptCoach by remember { mutableStateOf(alarmDefaults.promptCoach) }
    var promptComedian by remember { mutableStateOf(alarmDefaults.promptComedian) }
    var promptZen by remember { mutableStateOf(alarmDefaults.promptZen) }
    var promptHypeman by remember { mutableStateOf(alarmDefaults.promptHypeman) }

    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Edit Prompts",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    stringResource(R.string.settings_persona_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                OutlinedTextField(
                    value = promptCoach,
                    onValueChange = { promptCoach = it },
                    label = { Text(stringResource(R.string.settings_persona_label_coach)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 5
                )
                
                OutlinedTextField(
                    value = promptComedian,
                    onValueChange = { promptComedian = it },
                    label = { Text(stringResource(R.string.settings_persona_label_comedian)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 5
                )

                OutlinedTextField(
                    value = promptZen,
                    onValueChange = { promptZen = it },
                    label = { Text(stringResource(R.string.settings_persona_label_zen)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 5
                )

                OutlinedTextField(
                    value = promptHypeman,
                    onValueChange = { promptHypeman = it },
                    label = { Text(stringResource(R.string.settings_persona_label_hypeman)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 5
                )
                
                OutlinedButton(
                    onClick = {
                        promptCoach = "The Drill Sergeant. You are loud, demanding, and use military terms. STRICT RULE: You are translating the text. Do NOT change facts, time, or weather. Do NOT add new information. DO NOT combine the final trivia sentence with the rest of the text."
                        promptComedian = "The Sarcastic Best Friend. You are witty, dry, and slightly ironic. STRICT RULE: You are translating the text. Do NOT change facts, time, or weather. Do NOT add new information. DO NOT combine the final trivia sentence with the rest of the text."
                        promptZen = "The Zen Master. You are calm, poetic, and mindful. STRICT RULE: You are translating the text. Do NOT change facts, time, or weather. Do NOT add new information. DO NOT combine the final trivia sentence with the rest of the text."
                        promptHypeman = "The Hype-Man. You are extremely energetic, use caps, and over-the-top excited. STRICT RULE: You are translating the text. Do NOT change facts, time, or weather. Do NOT add new information. DO NOT combine the final trivia sentence with the rest of the text."
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(stringResource(R.string.settings_persona_btn_reset), fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(promptCoach, promptComedian, promptZen, promptHypeman) }
            ) {
                Text(stringResource(R.string.btn_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )
}

@Composable
fun BuddyManagementSection(viewModel: SettingsViewModel) {
    val buddies by viewModel.globalBuddies.collectAsState()
    val confirmedNumbers by viewModel.confirmedBuddyNumbers.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.settings_buddy_title), style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.btn_add))
            }
        }

        if (buddies.isEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    stringResource(R.string.settings_buddy_empty_state),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            buddies.forEach { buddyStr ->
                val parts = buddyStr.split("|")
                val name = parts.getOrNull(0) ?: stringResource(R.string.buddy_dialog_unknown)
                val phone = parts.getOrNull(1) ?: ""
                val isConfirmed = confirmedNumbers.any { it.replace(Regex("[^\\d+]"), "").endsWith(phone.replace(Regex("[^\\d+]"), "")) || phone.replace(Regex("[^\\d+]"), "").endsWith(it.replace(Regex("[^\\d+]"), "")) }

                BuddyListItem(
                    name = name,
                    phone = phone,
                    isConfirmed = isConfirmed,
                    onDelete = { viewModel.removeGlobalBuddy(name, phone) }
                )
            }
        }
    }

    if (showAddDialog) {
        BuddySelectionDialog(
            onDismiss = { showAddDialog = false },
            onBuddySelected = { name, phone ->
                viewModel.addGlobalBuddy(name, phone)
                showAddDialog = false
            },
            globalBuddies = buddies,
            startInManualMode = true
        )
    }
}

@Composable
fun BuddyListItem(
    name: String,
    phone: String,
    isConfirmed: Boolean,
    onDelete: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = if (isConfirmed) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = name.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isConfirmed) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(phone, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isConfirmed) stringResource(R.string.settings_buddy_status_confirmed) else stringResource(R.string.settings_buddy_status_pending),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isConfirmed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.settings_buddy_btn_remove), tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
