package com.elroi.alarmpal.ui.screen.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.res.painterResource
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.media.RingtoneManager
import android.net.Uri
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
import com.elroi.alarmpal.ui.components.SettingHelpIcon
import com.elroi.alarmpal.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateUp: () -> Unit,
    onNavigateToHelp: () -> Unit = {},
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Wake-Up Briefing", style = MaterialTheme.typography.titleLarge)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Auto-detect Location", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Use device location for weather",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isAutoLocation,
                    onCheckedChange = { checked -> 
                        if (checked) {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context,
                                android.Manifest.permission.ACCESS_COARSE_LOCATION
                            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                            
                            if (hasPermission) {
                                viewModel.updateIsAutoLocation(true, context)
                            } else {
                                locationPermissionLauncher.launch(android.Manifest.permission.ACCESS_COARSE_LOCATION)
                            }
                        } else {
                            viewModel.updateIsAutoLocation(false)
                        }
                    }
                )
            }

            OutlinedTextField(
                value = location,
                onValueChange = { viewModel.updateLocation(it) },
                label = { Text(if (isAutoLocation) "Auto-detected city" else "Weather Location (e.g., New York, NY)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isAutoLocation
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            
            Text("AI Features ✨", style = MaterialTheme.typography.titleLarge)
            
            val isCloudAiEnabled by viewModel.isCloudAiEnabled.collectAsState()
            val geminiApiKey by viewModel.geminiApiKey.collectAsState()
            val isKeyValidating by viewModel.isKeyValidating.collectAsState()
            val keyValidationResult by viewModel.keyValidationResult.collectAsState()
            val keyValidationError by viewModel.keyValidationError.collectAsState()
            val detectedKey by viewModel.detectedClipboardKey.collectAsState()

            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Cloud AI Enhancement", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Use Gemini for complex briefings (Internet & API Key required)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        Switch(
                            checked = isCloudAiEnabled,
                            onCheckedChange = { viewModel.updateIsCloudAiEnabled(it) }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    val preferredTier by viewModel.preferredAiTier.collectAsState()
                    val isAdvancedSupported by viewModel.isAdvancedAiSupported.collectAsState()
                    
                    if (isAdvancedSupported == GeminiNanoStatus.SUPPORTED) {
                        Text(
                            "Local AI Intelligence Tier",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            val tiers = listOf("STANDARD", "ADVANCED")
                            tiers.forEachIndexed { index, tier ->
                                SegmentedButton(
                                    shape = SegmentedButtonDefaults.itemShape(index = index, count = tiers.size),
                                    onClick = { viewModel.updatePreferredAiTier(tier) },
                                    selected = preferredTier == tier,
                                    label = {
                                        Text(if (tier == "STANDARD") "Standard" else "Advanced")
                                    }
                                )
                            }
                        }
                        
                        Text(
                            if (preferredTier == "ADVANCED") 
                                "Advanced: Real on-device LLM (Gemma 2B) for creative briefings."
                            else 
                                "Standard: Fast & consistent on-device briefings.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        // Fallback Order Selector (Only if Cloud is also enabled and Nano is supported)
                        if (isCloudAiEnabled) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Primary Intelligence Source",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            val fallbackOrder by viewModel.aiFallbackOrder.collectAsState()
                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                val orders = listOf("CLOUD_THEN_LOCAL", "LOCAL_THEN_CLOUD")
                                orders.forEachIndexed { index, order ->
                                    SegmentedButton(
                                        shape = SegmentedButtonDefaults.itemShape(index = index, count = orders.size),
                                        onClick = { viewModel.updateAiFallbackOrder(order) },
                                        selected = fallbackOrder == order,
                                        label = {
                                            Text(if (order == "CLOUD_THEN_LOCAL") "Cloud First" else "Local First")
                                        }
                                    )
                                }
                            }
                            Text(
                                if (fallbackOrder == "CLOUD_THEN_LOCAL") 
                                    "Prioritize Cloud (Gemini) for best quality, fallback to Local (Nano) if offline."
                                else 
                                    "Prioritize Local (Nano) for best privacy/speed, fallback to Cloud if needed.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    } else if (isAdvancedSupported == GeminiNanoStatus.CHECKING) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.1f))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                            Text(
                                "Checking device capabilities...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }

                    var showNanoHelpDialogLocal by remember { mutableStateOf(false) }

                    if (isAdvancedSupported == GeminiNanoStatus.DOWNLOAD_REQUIRED) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.1f))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Info, contentDescription = "Info", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                            Text(
                                "Advanced On-Device AI (Gemma 2B) requires a ~1.5GB model download from official Hugging Face servers. Using Standard local briefings until downloaded.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }

                        if (showNanoHelpDialogLocal) {
                            AlertDialog(
                                onDismissRequest = { showNanoHelpDialogLocal = false },
                                title = { Text("On-Device AI (Gemma 2B)") },
                                text = {
                                    Column {
                                        Text("This application uses Google's Gemma 2B model for local, privacy-first AI generation.")
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("Because the model file is large (~1.5GB), it must be downloaded separately.")
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("You must have Wi-Fi enabled to download the model.")
                                    }
                                },
                                confirmButton = {
                                    TextButton(onClick = { showNanoHelpDialogLocal = false }) {
                                        Text("Got it")
                                    }
                                }
                            )
                        }
                        OutlinedButton(
                            onClick = { showNanoHelpDialogLocal = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Learn More")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.triggerLocalModelDownload() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Download Model")
                        }
                    }
                    if (isAdvancedSupported == GeminiNanoStatus.DOWNLOADING) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.1f))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                            Text(
                                "Downloading Gemma 2B model... please wait.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                    if (isAdvancedSupported == GeminiNanoStatus.NOT_SUPPORTED) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.1f))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Info, contentDescription = "Info", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Advanced On-Device AI (Gemma 2B) is not supported on this device. Using Standard local briefings.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    "Tap here to learn how to enable it.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                                    modifier = Modifier.clickable { showNanoHelpDialogLocal = true }.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(visible = isCloudAiEnabled) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LaunchedEffect(Unit) {
                        viewModel.checkClipboardForKey(context)
                    }

                    if (detectedKey != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    "Gemini API Key detected in clipboard!",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { viewModel.useDetectedKey() },
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                    ) {
                                        Text("Use Key")
                                    }
                                    OutlinedButton(
                                        onClick = { viewModel.dismissDetectedKey() },
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                    ) {
                                        Text("Dismiss")
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = geminiApiKey,
                            onValueChange = { viewModel.updateGeminiApiKey(it) },
                            label = { Text("Gemini API Key") },
                            placeholder = { Text("AIza...") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            trailingIcon = {
                                if (keyValidationResult != null) {
                                    Icon(
                                        imageVector = if (keyValidationResult == true) Icons.Default.Check else Icons.Default.Close,
                                        contentDescription = null,
                                        tint = if (keyValidationResult == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        )
                        
                        Button(
                            onClick = { viewModel.validateApiKey() },
                            enabled = !isKeyValidating && geminiApiKey.isNotBlank(),
                            modifier = Modifier.height(56.dp)
                        ) {
                            if (isKeyValidating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Test")
                            }
                        }
                    }

                    if (keyValidationResult == true) {
                        Text(
                            "✅ API Key is valid and working!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else if (keyValidationResult == false) {
                        Text(
                            "❌ ${keyValidationError ?: "Invalid API Key. Please check and try again."}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/app/apikey"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.filledTonalButtonColors()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Step 1: Get API Key from Google")
                    }

                    Text(
                        "Click to open Google AI Studio, create a key, and then return here. We'll automatically detect it!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            var showWipeConfirmationLocal by remember { mutableStateOf(false) }
            if (showWipeConfirmationLocal) {
                AlertDialog(
                    onDismissRequest = { showWipeConfirmationLocal = false },
                    title = { Text("Reset Brain") },
                    text = { Text("This will delete all learned context, preferences, and custom settings. The brain will restart fresh. Are you sure?") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showWipeConfirmationLocal = false
                                viewModel.wipeBrainMemory()
                            }
                        ) {
                            Text("Reset", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showWipeConfirmationLocal = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
            IntelligenceHealthView(viewModel, onWipeBrainMemory = { showWipeConfirmationLocal = true })

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text("Alarm-Pal Personality 🎭", style = MaterialTheme.typography.titleLarge)
            Text(
                "Choose how your AI companion should wake you up.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val personas = listOf(
                PersonaInfo("COACH", "🪖 The Drill Sergeant", "No excuses. Just results. Move it!", Icons.Default.Info, MaterialTheme.colorScheme.error),
                PersonaInfo("COMEDIAN", "🤡 The Sarcastic Friend", "Oh, you're awake. Impressive.", Icons.Default.Face, MaterialTheme.colorScheme.secondary),
                PersonaInfo("ZEN", "🧘 The Zen Master", "Breathe. The day awaits, mindfully.", Icons.Default.Favorite, MaterialTheme.colorScheme.tertiary),
                PersonaInfo("HYPEMAN", "🚀 The Hype-Man", "Let's gooo! You got this!", Icons.Default.Notifications, MaterialTheme.colorScheme.primary),
                PersonaInfo("SURPRISE", "🎲 Surprise Me", "A random personality every morning.", Icons.Default.Refresh, MaterialTheme.colorScheme.outline)
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                personas.forEach { persona ->
                    PersonaCard(
                        info = persona,
                        isSelected = alarmDefaults.aiPersona == persona.id,
                        onClick = {
                            viewModel.updateAlarmDefaults(alarmDefaults.copy(aiPersona = persona.id))
                        }
                    )
                }
            }
            var showEditPromptsDialogLocal by remember { mutableStateOf(false) }
            OutlinedButton(
                onClick = { showEditPromptsDialogLocal = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Edit Personality Prompts")
            }
            
            if (showEditPromptsDialogLocal) {
                EditPromptsDialog(
                    alarmDefaults = alarmDefaults,
                    onDismiss = { showEditPromptsDialogLocal = false },
                    onSave = { coach, comedian, zen, hypeman ->
                        viewModel.updateAlarmDefaults(
                            alarmDefaults.copy(
                                promptCoach = coach,
                                promptComedian = comedian,
                                promptZen = zen,
                                promptHypeman = hypeman
                            )
                        )
                        showEditPromptsDialogLocal = false
                    }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Temperature Unit", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        if (isCelsius) "Celsius (°C)" else "Fahrenheit (°F)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isCelsius,
                    onCheckedChange = { viewModel.updateIsCelsius(it) }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text("Weekdays & Weekend", style = MaterialTheme.typography.titleLarge)

            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Select your weekend days", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Unselected days will be considered weekdays",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                val days = listOf(
                    7 to "Su", 1 to "Mo", 2 to "Tu", 3 to "We",
                    4 to "Th", 5 to "Fr", 6 to "Sa"
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    days.forEach { (isoIndex, label) ->
                        val isWeekend = alarmDefaults.weekendDays.contains(isoIndex)
                        Surface(
                            selected = isWeekend,
                            onClick = {
                                val newWeekend = if (isWeekend) alarmDefaults.weekendDays - isoIndex else alarmDefaults.weekendDays + isoIndex
                                viewModel.updateAlarmDefaults(alarmDefaults.copy(weekendDays = newWeekend))
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isWeekend) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                            border = if (isWeekend) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.weight(1f).height(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    color = if (isWeekend) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text("New Alarm Defaults", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))

            // --- AUDIO & VIBRATION ---
            Text("Audio & Vibration", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 8.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Default Alarm Sound", fontWeight = FontWeight.Medium)
                    Text("New alarms will have sound enabled by default", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = alarmDefaults.isSoundEnabled, onCheckedChange = { 
                    viewModel.updateAlarmDefaults(alarmDefaults.copy(isSoundEnabled = it)) 
                })
            }

            AnimatedVisibility(visible = alarmDefaults.isSoundEnabled, enter = expandVertically(), exit = shrinkVertically()) {
                Surface(
                    onClick = {
                        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
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
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Sound Source", fontWeight = FontWeight.Medium)
                            Text(defaultSoundName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Vibrate", fontWeight = FontWeight.Medium)
                }
                Switch(checked = alarmDefaults.isVibrate, onCheckedChange = { viewModel.updateAlarmDefaults(alarmDefaults.copy(isVibrate = it)) })
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Smooth Fade-Out", fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.width(8.dp))
                        SettingHelpIcon(title = "Smooth Fade-Out", content = "Gradually reduces the alarm volume over 1 second when you snooze or dismiss, instead of an abrupt stop.")
                    }
                    Text("Gradually fade sound on dismiss or snooze", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = alarmDefaults.isSmoothFadeOut, onCheckedChange = { viewModel.updateAlarmDefaults(alarmDefaults.copy(isSmoothFadeOut = it)) })
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Gentle Wake", fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.width(8.dp))
                        SettingHelpIcon(title = "Gentle Wake", content = "Starts the alarm volume at 0% and gradually increases to your maximum settings over the specified duration. Great for a peaceful start to the day.")
                    }
                    Text("Volume crescendo", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = alarmDefaults.isGentleWake, onCheckedChange = { viewModel.updateAlarmDefaults(alarmDefaults.copy(isGentleWake = it)) })
            }
            
            AnimatedVisibility(visible = alarmDefaults.isGentleWake, enter = expandVertically(), exit = shrinkVertically()) {
                Column(modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Fade-in duration", style = MaterialTheme.typography.bodyMedium)
                        Text(if (alarmDefaults.crescendoDurationMinutes == 0) "Instant" else "${alarmDefaults.crescendoDurationMinutes} min", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    }
                    Slider(
                        value = alarmDefaults.crescendoDurationMinutes.toFloat(),
                        onValueChange = { viewModel.updateAlarmDefaults(alarmDefaults.copy(crescendoDurationMinutes = it.toInt())) },
                        valueRange = 0f..20f,
                        steps = 19
                    )
                }
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // --- SNOOZE & DISMISSAL ---
            Text("Snooze & Dismissal", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Snooze duration", fontWeight = FontWeight.Medium)
                Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.secondaryContainer) {
                    Text(
                        text = if (alarmDefaults.snoozeDurationMinutes == 0) "Off" else "${alarmDefaults.snoozeDurationMinutes} min",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            Slider(
                value = alarmDefaults.snoozeDurationMinutes.toFloat(),
                onValueChange = { 
                    val newSnooze = it.toInt()
                    viewModel.updateAlarmDefaults(alarmDefaults.copy(
                        snoozeDurationMinutes = newSnooze,
                        isEvasiveSnooze = alarmDefaults.isEvasiveSnooze 
                    ))
                },
                valueRange = 1f..60f,
                steps = 58
            )

            AnimatedVisibility(visible = alarmDefaults.snoozeDurationMinutes > 0, enter = expandVertically(), exit = shrinkVertically()) {
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Evasive Snooze", fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.width(8.dp))
                                SettingHelpIcon(title = "Evasive Snooze", content = "The Snooze button will randomly jump around the screen each time you try to press it, forcing you to pay attention.")
                            }
                        }
                        Switch(checked = alarmDefaults.isEvasiveSnooze, onCheckedChange = { viewModel.updateAlarmDefaults(alarmDefaults.copy(isEvasiveSnooze = it)) })
                    }
                    
                    AnimatedVisibility(visible = alarmDefaults.isEvasiveSnooze, enter = expandVertically(), exit = shrinkVertically()) {
                        Column(modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Starts moving after", style = MaterialTheme.typography.bodyMedium)
                                Text(if (alarmDefaults.evasiveSnoozesBeforeMoving == 0) "1st snooze" else "${alarmDefaults.evasiveSnoozesBeforeMoving + 1} snoozes", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            }
                            Slider(
                                value = alarmDefaults.evasiveSnoozesBeforeMoving.toFloat(),
                                onValueChange = { viewModel.updateAlarmDefaults(alarmDefaults.copy(evasiveSnoozesBeforeMoving = it.toInt())) },
                                valueRange = 0f..5f,
                                steps = 4
                            )
                        }
                    }
                }
            }
            
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
                    Text("Solve a problem to dismiss", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = alarmDefaults.mathDifficulty > 0,
                    onCheckedChange = {
                        val newDiff = if (!it) 0 else if (alarmDefaults.mathDifficulty == 0) 1 else alarmDefaults.mathDifficulty
                        viewModel.updateAlarmDefaults(alarmDefaults.copy(mathDifficulty = newDiff))
                    }
                )
            }

            AnimatedVisibility(visible = alarmDefaults.mathDifficulty > 0 && !alarmDefaults.smileToDismiss, enter = expandVertically(), exit = shrinkVertically()) {
                Column {
                    MathDifficultyChips(
                        difficulty = alarmDefaults.mathDifficulty,
                        onSelected = { viewModel.updateAlarmDefaults(alarmDefaults.copy(mathDifficulty = it)) }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Number of problems: ${alarmDefaults.mathProblemCount}", style = MaterialTheme.typography.bodyMedium)
                    }
                    Slider(
                        value = alarmDefaults.mathProblemCount.toFloat(),
                        onValueChange = { viewModel.updateAlarmDefaults(alarmDefaults.copy(mathProblemCount = it.toInt())) },
                        valueRange = 1f..10f,
                        steps = 8
                    )
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Gradually Increase Difficulty", fontWeight = FontWeight.Medium)
                            Text("Starts easy and gets harder up to your selected level", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = alarmDefaults.mathGraduallyIncreaseDifficulty,
                            onCheckedChange = { viewModel.updateAlarmDefaults(alarmDefaults.copy(mathGraduallyIncreaseDifficulty = it)) }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            
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
                    Text("3 random face challenges to dismiss", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = alarmDefaults.smileToDismiss,
                    onCheckedChange = { 
                        val newMath = if (it && alarmDefaults.smileFallbackMethod == "MATH" && alarmDefaults.mathDifficulty == 0) 1 else alarmDefaults.mathDifficulty
                        viewModel.updateAlarmDefaults(alarmDefaults.copy(smileToDismiss = it, mathDifficulty = newMath))
                    }
                )
            }
            
            AnimatedVisibility(visible = alarmDefaults.smileToDismiss, enter = expandVertically(), exit = shrinkVertically()) {
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
                                onClick = { viewModel.updateAlarmDefaults(alarmDefaults.copy(smileFallbackMethod = option)) },
                                selected = alarmDefaults.smileFallbackMethod == option,
                                label = { Text(if (option == "NONE") "No Fallback" else "Math Challenge", fontSize = 12.sp) }
                            )
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // --- SMART FEATURES ---
            Text("Smart Features", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Wake-Up Briefing", fontWeight = FontWeight.Medium)
                    Text("Personalized AI briefing when you wake up", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = alarmDefaults.isBriefingEnabled, onCheckedChange = { viewModel.updateAlarmDefaults(alarmDefaults.copy(isBriefingEnabled = it)) })
            }

            AnimatedVisibility(visible = alarmDefaults.isBriefingEnabled, enter = expandVertically(), exit = shrinkVertically()) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Read Aloud (TTS)", fontWeight = FontWeight.Medium)
                            Text("Speak the briefing out loud", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = alarmDefaults.isTtsEnabled, onCheckedChange = { viewModel.updateAlarmDefaults(alarmDefaults.copy(isTtsEnabled = it)) })
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Briefing Content", fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 16.dp))
                    
                    OutlinedTextField(
                        value = alarmDefaults.briefingUserName,
                        onValueChange = { viewModel.updateAlarmDefaults(alarmDefaults.copy(briefingUserName = it.take(20))) },
                        label = { Text("Your Name (Optional)") },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        singleLine = true
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Include Weather", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = alarmDefaults.briefingIncludeWeather, onCheckedChange = { viewModel.updateAlarmDefaults(alarmDefaults.copy(briefingIncludeWeather = it)) })
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Include Calendar Events", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = alarmDefaults.briefingIncludeCalendar, onCheckedChange = { viewModel.updateAlarmDefaults(alarmDefaults.copy(briefingIncludeCalendar = it)) })
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Include Daily Fun Fact", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = alarmDefaults.briefingIncludeFact, onCheckedChange = { viewModel.updateAlarmDefaults(alarmDefaults.copy(briefingIncludeFact = it)) })
                    }
                    Spacer(modifier = Modifier.height(8.dp))
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

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // Privacy Policy link
            val privacyPolicyContext = androidx.compose.ui.platform.LocalContext.current
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://elroiluria.github.io/alarmpal-privacy-policy/")
                        )
                        privacyPolicyContext.startActivity(intent)
                    }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Privacy Policy",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "How AlarmPal handles your data",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
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
                    "You can customize the exact system instructions given to the AI for each personality type.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                OutlinedTextField(
                    value = promptCoach,
                    onValueChange = { promptCoach = it },
                    label = { Text("The Drill Sergeant") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 5
                )
                
                OutlinedTextField(
                    value = promptComedian,
                    onValueChange = { promptComedian = it },
                    label = { Text("The Sarcastic Friend") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 5
                )

                OutlinedTextField(
                    value = promptZen,
                    onValueChange = { promptZen = it },
                    label = { Text("The Zen Master") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 5
                )

                OutlinedTextField(
                    value = promptHypeman,
                    onValueChange = { promptHypeman = it },
                    label = { Text("The Hype-Man") },
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
                    Text("Reset to Defaults", fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(promptCoach, promptComedian, promptZen, promptHypeman) }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
