package com.elroi.lemurloop.ui.screen.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.res.painterResource
import android.Manifest
import android.content.ComponentName
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
import com.elroi.lemurloop.R
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.shape.CircleShape
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import com.elroi.lemurloop.domain.manager.GeminiNanoStatus
import com.elroi.lemurloop.ui.screen.alarm.MathDifficultyChips
import com.elroi.lemurloop.ui.components.BuddySelectionDialog
import com.elroi.lemurloop.ui.components.SettingHelpIcon
import com.elroi.lemurloop.ui.components.VibrationPatternGallery
import com.elroi.lemurloop.util.debugLog
import com.elroi.lemurloop.ui.viewmodel.SettingsViewModel
import com.elroi.lemurloop.ui.viewmodel.SettingsUiEvent
import com.elroi.lemurloop.ui.viewmodel.ToastMessage
import com.elroi.lemurloop.domain.manager.SettingSearchItem

private fun getSearchItemTitleRes(id: String): Int = when (id) {
    "morning_personality" -> R.string.search_morning_personality_title
    "briefing_tts" -> R.string.search_briefing_tts_title
    "briefing_content" -> R.string.search_briefing_content_title
    "alarm_creation_style" -> R.string.search_alarm_creation_style_title
    "alarm_sound" -> R.string.search_alarm_sound_title
    "vibration" -> R.string.search_vibration_title
    "gentle_wake" -> R.string.search_gentle_wake_title
    "snooze_duration" -> R.string.search_snooze_duration_title
    "math_challenge" -> R.string.search_math_challenge_title
    "face_game" -> R.string.search_face_game_title
    "weekend_days" -> R.string.search_weekend_days_title
    "accountability_buddies" -> R.string.search_accountability_buddies_title
    "cloud_ai" -> R.string.search_cloud_ai_title
    "gemini_api_key" -> R.string.search_gemini_api_key_title
    "intelligence_health" -> R.string.search_intelligence_health_title
    "about" -> R.string.search_about_title
    "replay_tutorial" -> R.string.search_replay_tutorial_title
    "demo_alarms" -> R.string.search_demo_alarms_title
    "privacy_policy" -> R.string.search_privacy_policy_title
    "diagnostic_logs" -> R.string.search_diagnostic_logs_title
    "danger_zone" -> R.string.search_danger_zone_title
    else -> R.string.settings_screen_title
}

private fun getSearchItemDescRes(id: String): Int = when (id) {
    "morning_personality" -> R.string.search_morning_personality_desc
    "briefing_tts" -> R.string.search_briefing_tts_desc
    "briefing_content" -> R.string.search_briefing_content_desc
    "alarm_creation_style" -> R.string.search_alarm_creation_style_desc
    "alarm_sound" -> R.string.search_alarm_sound_desc
    "vibration" -> R.string.search_vibration_desc
    "gentle_wake" -> R.string.search_gentle_wake_desc
    "snooze_duration" -> R.string.search_snooze_duration_desc
    "math_challenge" -> R.string.search_math_challenge_desc
    "face_game" -> R.string.search_face_game_desc
    "weekend_days" -> R.string.search_weekend_days_desc
    "accountability_buddies" -> R.string.search_accountability_buddies_desc
    "cloud_ai" -> R.string.search_cloud_ai_desc
    "gemini_api_key" -> R.string.search_gemini_api_key_desc
    "intelligence_health" -> R.string.search_intelligence_health_desc
    "about" -> R.string.search_about_desc
    "replay_tutorial" -> R.string.search_replay_tutorial_desc
    "demo_alarms" -> R.string.search_demo_alarms_desc
    "privacy_policy" -> R.string.search_privacy_policy_desc
    "diagnostic_logs" -> R.string.search_diagnostic_logs_desc
    "danger_zone" -> R.string.search_danger_zone_desc
    else -> R.string.settings_search_hint
}

private fun getSearchSectionRes(section: String): Int = when (section) {
    "Morning Experience" -> R.string.settings_section_morning
    "Wake-Up Engine" -> R.string.settings_section_wakeup
    "Accountability" -> R.string.settings_section_accountability
    "Intelligence" -> R.string.settings_section_intelligence
    "Help & System" -> R.string.settings_section_help_system
    else -> R.string.settings_screen_title
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateUp: () -> Unit,
    onNavigateToHelp: () -> Unit = {},
    onNavigateToOnboarding: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onNavigateToLogs: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val location by viewModel.location.collectAsState()
    val isCelsius by viewModel.isCelsius.collectAsState()
    val isAutoLocation by viewModel.isAutoLocation.collectAsState()
    val alarmDefaults by viewModel.alarmDefaults.collectAsState()
    val hasChanges by viewModel.hasChanges.collectAsState()
    val expandedSections by viewModel.expandedSections.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()
    val isBriefingGenerating by viewModel.isBriefingGenerating.collectAsState()
    val generatingProgress by viewModel.generatingProgress.collectAsState()
    val previewBriefingScript by viewModel.previewBriefingScript.collectAsState()
    var showDiscardDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var defaultSoundName by remember { mutableStateOf(context.getString(R.string.settings_sound_default)) }
    var showAdvancedExperience by remember { mutableStateOf(false) }
    var showAdvancedMath by remember { mutableStateOf(false) }
    var showEditPrompts by remember { mutableStateOf(false) }

    val versionDisplay = remember(context) {
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val suffix = com.elroi.lemurloop.BuildConfig.VERSION_SUFFIX
            "${pInfo.versionName}$suffix"
        } catch (e: Exception) {
            context.getString(R.string.settings_sound_unknown)
        }
    }

    var pendingWeatherEnable by remember { mutableStateOf(false) }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                viewModel.updateIsAutoLocation(true, context)
                if (pendingWeatherEnable) {
                    viewModel.enableBriefingIncludeWeather()
                    pendingWeatherEnable = false
                }
            } else {
                viewModel.updateIsAutoLocation(false)
                if (pendingWeatherEnable) {
                    android.widget.Toast.makeText(context, context.getString(R.string.toast_location_weather), android.widget.Toast.LENGTH_SHORT).show()
                    pendingWeatherEnable = false
                } else {
                    android.widget.Toast.makeText(context, context.getString(R.string.toast_location_auto), android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    )
    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                viewModel.enableBriefingIncludeCalendar()
            } else {
                android.widget.Toast.makeText(context, context.getString(R.string.settings_toast_calendar_required), android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    )
    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri: Uri? = if (android.os.Build.VERSION.SDK_INT >= 33) {
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            viewModel.updateAlarmDefaults(alarmDefaults.copy(defaultSoundUri = uri?.toString()))
        }
    }

    LaunchedEffect(Unit) {
        viewModel.message.collect { msg ->
            val text = context.getString(msg.resId, *msg.formatArgs)
            android.widget.Toast.makeText(context, text, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    previewBriefingScript?.let { script ->
        PreviewBriefingDialog(
            script = script,
            onDismiss = { viewModel.clearBriefingPreview() }
        )
    }

    LaunchedEffect(alarmDefaults.defaultSoundUri) {
        if (alarmDefaults.defaultSoundUri != null) {
            try {
                val ringtone = RingtoneManager.getRingtone(context, Uri.parse(alarmDefaults.defaultSoundUri))
                defaultSoundName = ringtone?.getTitle(context) ?: context.getString(R.string.settings_sound_unknown)
            } catch (e: Exception) {
                defaultSoundName = context.getString(R.string.settings_sound_custom)
            }
        } else {
            defaultSoundName = context.getString(R.string.settings_sound_default)
        }
    }

    BackHandler(enabled = hasChanges) {
        showDiscardDialog = true
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text(stringResource(R.string.dialog_discard_title)) },
            text = { Text(stringResource(R.string.dialog_discard_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    onNavigateUp()
                }) {
                    Text(stringResource(R.string.btn_discard), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text(stringResource(R.string.btn_keep_editing))
                }
            }
        )
    }

    val onWipeBrainMemory = {
        viewModel.wipeBrainMemory()
        android.widget.Toast.makeText(context, context.getString(R.string.settings_toast_brain_wiped), android.widget.Toast.LENGTH_SHORT).show()
    }

    val onWipeAllData = {
        viewModel.wipeAllData()
        android.widget.Toast.makeText(context, context.getString(R.string.toast_all_data_wiped), android.widget.Toast.LENGTH_SHORT).show()
    }

    val coroutineScope = rememberCoroutineScope()

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var personaScrollOffset by remember { mutableStateOf(0) }
    var briefingScrollOffset by remember { mutableStateOf(0) }
    var snoozeScrollOffset by remember { mutableStateOf(0) }
    var mathScrollOffset by remember { mutableStateOf(0) }
    var faceGameScrollOffset by remember { mutableStateOf(0) }
    var scrollColumnTop by remember { mutableStateOf(0f) }
    var pendingScrollActionId by remember { mutableStateOf<String?>(null) }
    var sectionScrollOffsets by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var pendingScrollToSectionId by remember { mutableStateOf<String?>(null) }

    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_screen_title)) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (hasChanges) {
                            showDiscardDialog = true
                        } else {
                            onNavigateUp()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.btn_back))
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToHelp) {
                        Icon(Icons.Default.Info, contentDescription = stringResource(R.string.help_screen_title))
                    }
                    TextButton(onClick = {
                        viewModel.saveSettings()
                        onNavigateUp()
                    }) {
                        Text(stringResource(R.string.btn_save), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { padding ->
        val scrollState = rememberScrollState()
        val density = LocalDensity.current
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState)
                .onGloballyPositioned { scrollColumnTop = it.positionInRoot().y }
        ) {
            LaunchedEffect(Unit) {
                viewModel.events.collect { event ->
                    // Hide keyboard and clear focus whenever a search result action fires.
                    focusManager.clearFocus()
                    keyboardController?.hide()

                    when (event) {
                        is SettingsUiEvent.OpenSection -> {
                            pendingScrollToSectionId = event.sectionId
                        }
                        is SettingsUiEvent.OpenSubScreen -> {
                            when (event.route) {
                                "about" -> onNavigateToAbout()
                                "onboarding" -> onNavigateToOnboarding()
                                "logs" -> onNavigateToLogs()
                            }
                        }
                        is SettingsUiEvent.PerformInlineAction -> {
                            pendingScrollActionId = event.actionId
                        }
                    }
                }
            }
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                label = { Text(stringResource(R.string.settings_search_hint)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.clearSearch() }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.content_desc_clear_search))
                        }
                    }
                },
                singleLine = true
            )

            if (isSearching) {
                if (searchResults.isEmpty()) {
                    Text(
                        text = stringResource(R.string.settings_search_no_match, searchQuery),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        searchResults.forEach { item ->
                            Surface(
                                onClick = { viewModel.handleSearchResultClick(item) },
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(stringResource(getSearchItemTitleRes(item.id)), style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        stringResource(getSearchItemDescRes(item.id)),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        stringResource(getSearchSectionRes(item.section)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
            ExpandableSection(
                icon = Icons.Default.WbSunny,
                title = stringResource(R.string.settings_section_morning),
                isExpanded = expandedSections.contains("MORNING"),
                onToggle = { viewModel.toggleSection("MORNING") },
                sectionId = "MORNING",
                headerModifier = Modifier.onGloballyPositioned { coords ->
                    val offset = (coords.positionInRoot().y - scrollColumnTop + scrollState.value).toInt()
                    sectionScrollOffsets = sectionScrollOffsets + ("MORNING" to offset)
                    if (pendingScrollToSectionId == "MORNING") {
                        pendingScrollToSectionId = null
                        coroutineScope.launch {
                            val topPaddingPx = with(density) { 24.dp.toPx() }
                            scrollState.animateScrollTo((offset - topPaddingPx).toInt().coerceIn(0, scrollState.maxValue))
                        }
                    }
                }
            ) {
                // A. Companion Personality
                Text(
                    stringResource(R.string.settings_companion_personality),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .onGloballyPositioned { coords ->
                            personaScrollOffset = (coords.positionInRoot().y - scrollColumnTop + scrollState.value).toInt()
                            if (pendingScrollActionId == "scroll_morning_personality") {
                                pendingScrollActionId = null
                                coroutineScope.launch {
                                    val topPaddingPx = with(density) { 24.dp.toPx() }
                                    scrollState.animateScrollTo((personaScrollOffset - topPaddingPx).toInt().coerceIn(0, scrollState.maxValue))
                                }
                            }
                        }
                )
                val personas = listOf(
                    PersonaInfo("COACH", stringResource(R.string.persona_name_coach), stringResource(R.string.persona_desc_coach), Icons.Default.Info, MaterialTheme.colorScheme.error),
                    PersonaInfo("COMEDIAN", stringResource(R.string.persona_name_comedian), stringResource(R.string.persona_desc_comedian), Icons.Default.Face, MaterialTheme.colorScheme.secondary),
                    PersonaInfo("ZEN", stringResource(R.string.persona_name_zen), stringResource(R.string.persona_desc_zen), Icons.Default.Favorite, MaterialTheme.colorScheme.tertiary),
                    PersonaInfo("HYPEMAN", stringResource(R.string.persona_name_hypeman), stringResource(R.string.persona_desc_hypeman), Icons.Default.Notifications, MaterialTheme.colorScheme.primary),
                    PersonaInfo("SURPRISE", stringResource(R.string.persona_name_surprise), stringResource(R.string.persona_desc_surprise), Icons.Default.Refresh, MaterialTheme.colorScheme.outline)
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    personas.filter { it.id != "SURPRISE" }.forEach { persona ->
                        PersonaCard(
                            info = persona,
                            isSelected = alarmDefaults.aiPersona == persona.id && !alarmDefaults.aiPersonaSurprise,
                            onClick = {
                                viewModel.updateAlarmDefaults(alarmDefaults.copy(aiPersona = persona.id, aiPersonaSurprise = false))
                            },
                            onPreviewClick = {
                                viewModel.playPersonaPreview(persona.id)
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
                                Text(stringResource(R.string.settings_surprise_me), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(stringResource(R.string.settings_surprise_me_desc), style = MaterialTheme.typography.bodySmall)
                            }
                            Switch(checked = alarmDefaults.aiPersonaSurprise, onCheckedChange = { viewModel.updateAlarmDefaults(alarmDefaults.copy(aiPersonaSurprise = it)) })
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // B. Aura Report Content
                Text(
                    stringResource(R.string.settings_aura_report_content),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .onGloballyPositioned { coords ->
                            briefingScrollOffset = (coords.positionInRoot().y - scrollColumnTop + scrollState.value).toInt()
                            if (pendingScrollActionId == "scroll_briefing_content") {
                                pendingScrollActionId = null
                                coroutineScope.launch {
                                    val topPaddingPx = with(density) { 24.dp.toPx() }
                                    scrollState.animateScrollTo((briefingScrollOffset - topPaddingPx).toInt().coerceIn(0, scrollState.maxValue))
                                }
                            }
                        }
                )
                
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.settings_read_aloud_tts), style = MaterialTheme.typography.bodyLarge)
                            Switch(checked = alarmDefaults.isTtsEnabled, onCheckedChange = { viewModel.updateAlarmDefaults(alarmDefaults.copy(isTtsEnabled = it)) })
                        }
                        
                        Column(modifier = Modifier.padding(bottom = 8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(stringResource(R.string.settings_briefing_timeout), style = MaterialTheme.typography.bodySmall)
                                Text(stringResource(R.string.settings_seconds_value, alarmDefaults.briefingTimeoutSeconds), color = MaterialTheme.colorScheme.primary)
                            }
                            Slider(
                                value = alarmDefaults.briefingTimeoutSeconds.toFloat(),
                                onValueChange = { viewModel.updateAlarmDefaults(alarmDefaults.copy(briefingTimeoutSeconds = it.toInt())) },
                                valueRange = 10f..120f,
                                steps = 11 // (120-10)/10 = 11 steps for 10s increments
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.settings_weather_report), style = MaterialTheme.typography.bodyLarge)
                            Switch(
                                checked = alarmDefaults.briefingIncludeWeather,
                                onCheckedChange = { enabled ->
                                    if (enabled) {
                                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                            viewModel.updateAlarmDefaults(alarmDefaults.copy(briefingIncludeWeather = true))
                                        } else {
                                            pendingWeatherEnable = true
                                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                                        }
                                    } else {
                                        viewModel.updateAlarmDefaults(alarmDefaults.copy(briefingIncludeWeather = false))
                                    }
                                }
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.settings_calendar_events), style = MaterialTheme.typography.bodyLarge)
                            Switch(
                                checked = alarmDefaults.briefingIncludeCalendar,
                                onCheckedChange = { enabled ->
                                    if (enabled) {
                                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED) {
                                            viewModel.updateAlarmDefaults(alarmDefaults.copy(briefingIncludeCalendar = true))
                                        } else {
                                            calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
                                        }
                                    } else {
                                        viewModel.updateAlarmDefaults(alarmDefaults.copy(briefingIncludeCalendar = false))
                                    }
                                }
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.settings_fun_fact), style = MaterialTheme.typography.bodyLarge)
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
                    enabled = !isBriefingGenerating,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isBriefingGenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isBriefingGenerating) (if (generatingProgress == "Generating..." || generatingProgress.isBlank()) stringResource(R.string.settings_generating) else generatingProgress) else stringResource(R.string.settings_preview_briefing))
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
                        Text(stringResource(R.string.settings_advanced_customization), style = MaterialTheme.typography.titleMedium)
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
                            Text(stringResource(R.string.settings_customize_persona_prompts))
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
            }

            ExpandableSection(
                icon = Icons.Default.Settings,
                title = stringResource(R.string.settings_section_wakeup),
                isExpanded = expandedSections.contains("WAKEUP"),
                onToggle = { viewModel.toggleSection("WAKEUP") },
                sectionId = "WAKEUP",
                headerModifier = Modifier.onGloballyPositioned { coords ->
                    val offset = (coords.positionInRoot().y - scrollColumnTop + scrollState.value).toInt()
                    sectionScrollOffsets = sectionScrollOffsets + ("WAKEUP" to offset)
                    if (pendingScrollToSectionId == "WAKEUP") {
                        pendingScrollToSectionId = null
                        coroutineScope.launch {
                            val topPaddingPx = with(density) { 24.dp.toPx() }
                            scrollState.animateScrollTo((offset - topPaddingPx).toInt().coerceIn(0, scrollState.maxValue))
                        }
                    }
                }
            ) {
                // A. Alarm Creation
                Text(stringResource(R.string.settings_alarm_creation), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                val creationStyle by viewModel.alarmCreationStyle.collectAsState()
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    val styles = listOf("WIZARD", "SIMPLE")
                    styles.forEachIndexed { index, style ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = styles.size),
                            onClick = { viewModel.updateAlarmCreationStyle(style) },
                            selected = creationStyle == style,
                            label = { Text(if (style == "WIZARD") stringResource(R.string.settings_style_guided_wizard) else stringResource(R.string.settings_style_simple_setup), fontSize = 12.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // B. Default Alarm Behavior
                Text(stringResource(R.string.settings_default_alarm_behavior), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                
                // Audio & Haptics
                Text(stringResource(R.string.settings_audio_haptics), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.settings_alarm_sound), style = MaterialTheme.typography.bodyLarge)
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
                            Text(stringResource(R.string.settings_sound_source), fontWeight = FontWeight.Medium)
                            Text(defaultSoundName, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.settings_vibrate), style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = alarmDefaults.isVibrate, onCheckedChange = { viewModel.updateAlarmDefaults(alarmDefaults.copy(isVibrate = it)) })
                }
                AnimatedVisibility(visible = alarmDefaults.isVibrate) {
                    Column(modifier = Modifier.padding(bottom = 8.dp)) {
                        Text(
                            stringResource(R.string.vibration_advanced_title),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        VibrationPatternGallery(
                            selectedPattern = alarmDefaults.vibrationPattern,
                            onPatternSelected = { viewModel.updateAlarmDefaults(alarmDefaults.copy(vibrationPattern = it)) }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(R.string.vibration_initial_gap_label), style = MaterialTheme.typography.bodySmall)
                            Text(stringResource(R.string.vibration_initial_gap_unit, alarmDefaults.vibrationCrescendoStartGapSeconds), color = MaterialTheme.colorScheme.primary)
                        }
                        Slider(
                            value = alarmDefaults.vibrationCrescendoStartGapSeconds.toFloat(),
                            onValueChange = { viewModel.updateAlarmDefaults(alarmDefaults.copy(vibrationCrescendoStartGapSeconds = it.toInt())) },
                            valueRange = 1f..60f,
                            steps = 59
                        )
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    val isOutputEnabled = alarmDefaults.isSoundEnabled || alarmDefaults.isVibrate
                    val isGentleEnabled = alarmDefaults.isGentleWake && isOutputEnabled
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.settings_gentle_wake), style = MaterialTheme.typography.bodyLarge, color = if (isOutputEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f))
                        if (!isOutputEnabled) {
                            Text(stringResource(R.string.settings_requires_sound_or_vibrate), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
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
                            Text(stringResource(R.string.settings_crescendo_duration), style = MaterialTheme.typography.bodySmall)
                            Text(stringResource(R.string.settings_crescendo_unit_min, alarmDefaults.crescendoDurationMinutes), color = MaterialTheme.colorScheme.primary)
                        }
                        Slider(
                            value = alarmDefaults.crescendoDurationMinutes.toFloat(),
                            onValueChange = { viewModel.updateAlarmDefaults(alarmDefaults.copy(crescendoDurationMinutes = it.toInt())) },
                            valueRange = 0f..20f,
                            steps = 19
                        )
                    }
                }
                AnimatedVisibility(visible = alarmDefaults.isSoundEnabled || alarmDefaults.isVibrate) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.wizard_2_smooth_fade), style = MaterialTheme.typography.bodyLarge)
                        Switch(checked = alarmDefaults.isSmoothFadeOut, onCheckedChange = { viewModel.updateAlarmDefaults(alarmDefaults.copy(isSmoothFadeOut = it)) })
                    }
                }

                // Snooze & Dismissal
                Text(stringResource(R.string.settings_snooze_dismissal), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 12.dp))
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.settings_snooze_duration), style = MaterialTheme.typography.bodyLarge)
                        Text(if (alarmDefaults.snoozeDurationMinutes == 0) stringResource(R.string.settings_snooze_disabled) else stringResource(R.string.settings_snooze_min, alarmDefaults.snoozeDurationMinutes), color = if (alarmDefaults.snoozeDurationMinutes == 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                    }
                    Slider(
                        value = alarmDefaults.snoozeDurationMinutes.toFloat(),
                        onValueChange = { viewModel.updateAlarmDefaults(alarmDefaults.copy(snoozeDurationMinutes = it.toInt())) },
                        modifier = Modifier.onGloballyPositioned { coords ->
                            snoozeScrollOffset = (coords.positionInRoot().y - scrollColumnTop + scrollState.value).toInt()
                            if (pendingScrollActionId == "scroll_snooze") {
                                pendingScrollActionId = null
                                coroutineScope.launch {
                                    val topPaddingPx = with(density) { 24.dp.toPx() }
                                    scrollState.animateScrollTo((snoozeScrollOffset - topPaddingPx).toInt().coerceIn(0, scrollState.maxValue))
                                }
                            }
                        },
                        valueRange = 0f..60f,
                        steps = 59
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { coords ->
                            mathScrollOffset = (coords.positionInRoot().y - scrollColumnTop + scrollState.value).toInt()
                            if (pendingScrollActionId == "scroll_math") {
                                pendingScrollActionId = null
                                coroutineScope.launch {
                                    val topPaddingPx = with(density) { 24.dp.toPx() }
                                    scrollState.animateScrollTo((mathScrollOffset - topPaddingPx).toInt().coerceIn(0, scrollState.maxValue))
                                }
                            }
                        },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.settings_evasive_snooze), style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = alarmDefaults.isEvasiveSnooze, onCheckedChange = { viewModel.updateAlarmDefaults(alarmDefaults.copy(isEvasiveSnooze = it)) })
                }
                AnimatedVisibility(visible = alarmDefaults.isEvasiveSnooze) {
                    Column(modifier = Modifier.padding(bottom = 8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(R.string.settings_movement_threshold), style = MaterialTheme.typography.bodySmall)
                            Text(stringResource(R.string.settings_snoozes_count, alarmDefaults.evasiveSnoozesBeforeMoving + 1), color = MaterialTheme.colorScheme.primary)
                        }
                        Slider(
                            value = alarmDefaults.evasiveSnoozesBeforeMoving.toFloat(),
                            onValueChange = { viewModel.updateAlarmDefaults(alarmDefaults.copy(evasiveSnoozesBeforeMoving = it.toInt())) },
                            valueRange = 0f..5f,
                            steps = 4
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { coords ->
                            faceGameScrollOffset = (coords.positionInRoot().y - scrollColumnTop + scrollState.value).toInt()
                            if (pendingScrollActionId == "scroll_face_game") {
                                pendingScrollActionId = null
                                coroutineScope.launch {
                                    val topPaddingPx = with(density) { 24.dp.toPx() }
                                    scrollState.animateScrollTo((faceGameScrollOffset - topPaddingPx).toInt().coerceIn(0, scrollState.maxValue))
                                }
                            }
                        },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.settings_math_challenge), style = MaterialTheme.typography.bodyLarge)
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
                                Text(stringResource(R.string.settings_advanced_math_options), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                Icon(if (showAdvancedMath) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        AnimatedVisibility(visible = showAdvancedMath) {
                            Column {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(stringResource(R.string.settings_problem_count), style = MaterialTheme.typography.bodySmall)
                                    Text("${alarmDefaults.mathProblemCount}", color = MaterialTheme.colorScheme.primary)
                                }
                                Slider(
                                    value = alarmDefaults.mathProblemCount.toFloat(),
                                    onValueChange = { viewModel.updateAlarmDefaults(alarmDefaults.copy(mathProblemCount = it.toInt())) },
                                    valueRange = 1f..10f,
                                    steps = 8
                                )
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text(stringResource(R.string.settings_gradual_difficulty), style = MaterialTheme.typography.bodySmall)
                                    Switch(checked = alarmDefaults.mathGraduallyIncreaseDifficulty, onCheckedChange = { viewModel.updateAlarmDefaults(alarmDefaults.copy(mathGraduallyIncreaseDifficulty = it)) })
                                }
                            }
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.settings_face_game), style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = alarmDefaults.smileToDismiss, onCheckedChange = { viewModel.updateAlarmDefaults(alarmDefaults.copy(smileToDismiss = it)) })
                }
                AnimatedVisibility(visible = alarmDefaults.smileToDismiss) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(stringResource(R.string.settings_fallback_method), style = MaterialTheme.typography.bodySmall)
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
                Text(stringResource(R.string.settings_day_groups), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                val days = listOf(
                    7 to stringResource(R.string.day_sun),
                    1 to stringResource(R.string.day_mon),
                    2 to stringResource(R.string.day_tue),
                    3 to stringResource(R.string.day_wed),
                    4 to stringResource(R.string.day_thu),
                    5 to stringResource(R.string.day_fri),
                    6 to stringResource(R.string.day_sat)
                )
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.settings_select_weekend_days), style = MaterialTheme.typography.labelMedium)
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
            }

            ExpandableSection(
                icon = Icons.Default.Group,
                title = stringResource(R.string.settings_section_accountability),
                isExpanded = expandedSections.contains("ACCOUNTABILITY"),
                onToggle = { viewModel.toggleSection("ACCOUNTABILITY") },
                sectionId = "ACCOUNTABILITY",
                headerModifier = Modifier.onGloballyPositioned { coords ->
                    val offset = (coords.positionInRoot().y - scrollColumnTop + scrollState.value).toInt()
                    sectionScrollOffsets = sectionScrollOffsets + ("ACCOUNTABILITY" to offset)
                    if (pendingScrollToSectionId == "ACCOUNTABILITY") {
                        pendingScrollToSectionId = null
                        coroutineScope.launch {
                            val topPaddingPx = with(density) { 24.dp.toPx() }
                            scrollState.animateScrollTo((offset - topPaddingPx).toInt().coerceIn(0, scrollState.maxValue))
                        }
                    }
                }
            ) {
                BuddyManagementSection(viewModel)
            }

            ExpandableSection(
                icon = Icons.Default.Lightbulb,
                title = stringResource(R.string.settings_section_intelligence),
                isExpanded = expandedSections.contains("INTELLIGENCE"),
                onToggle = { viewModel.toggleSection("INTELLIGENCE") },
                sectionId = "INTELLIGENCE",
                headerModifier = Modifier.onGloballyPositioned { coords ->
                    val offset = (coords.positionInRoot().y - scrollColumnTop + scrollState.value).toInt()
                    sectionScrollOffsets = sectionScrollOffsets + ("INTELLIGENCE" to offset)
                    if (pendingScrollToSectionId == "INTELLIGENCE") {
                        pendingScrollToSectionId = null
                        coroutineScope.launch {
                            val topPaddingPx = with(density) { 24.dp.toPx() }
                            scrollState.animateScrollTo((offset - topPaddingPx).toInt().coerceIn(0, scrollState.maxValue))
                        }
                    }
                }
            ) {
                val isCloudAiEnabled by viewModel.isCloudAiEnabled.collectAsState()
                val isCloudTtsEnabled by viewModel.isCloudTtsEnabled.collectAsState()
                val cloudTtsKey by viewModel.cloudTtsApiKey.collectAsState()
                val brainTestResult by viewModel.briefingBrainTestResult.collectAsState()
                val brainTesting by viewModel.briefingBrainTesting.collectAsState()
                val isBriefingGenerating by viewModel.isBriefingGenerating.collectAsState()
                val generatingProgress by viewModel.generatingProgress.collectAsState()

                Text(
                    stringResource(R.string.intel_aura_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                )

                // Card A: Briefing Brain (Gemini)
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            stringResource(R.string.intel_briefing_brain_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            stringResource(R.string.intel_briefing_brain_desc),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.settings_use_gemini_briefings), style = MaterialTheme.typography.bodyLarge)
                            Switch(
                                checked = isCloudAiEnabled,
                                onCheckedChange = { viewModel.updateIsCloudAiEnabled(it) }
                            )
                        }
                        Button(
                            onClick = { viewModel.testGeminiQuick() },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !brainTesting,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (brainTesting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Icon(Icons.Default.Check, contentDescription = null)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (brainTesting) stringResource(R.string.intel_testing_gemini) else stringResource(R.string.intel_quick_gemini_test))
                        }
                        brainTestResult?.let { result ->
                            Text(
                                text = stringResource(R.string.intel_last_result, result),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            stringResource(R.string.intel_brain_off_fallback),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Card B: Persona Voice (Cloud TTS)
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(stringResource(R.string.settings_persona_voice_cloud_tts), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            stringResource(R.string.intel_cloud_tts_desc),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.settings_use_cloud_voices), style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    stringResource(R.string.intel_cloud_tts_network_note),
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    stringResource(R.string.intel_cloud_tts_manage_key),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            val hasKey = cloudTtsKey.isNotBlank()
                            Switch(
                                checked = isCloudTtsEnabled && hasKey,
                                onCheckedChange = { enabled ->
                                    if (hasKey) {
                                        viewModel.updateIsCloudTtsEnabled(enabled)
                                    } else {
                                        android.widget.Toast.makeText(
                                            context,
                                            context.getString(R.string.intel_add_cloud_tts_key_first),
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            )
                        }
                        val isPreviewingPersonaVoice by viewModel.isPreviewingPersonaVoice.collectAsState()
                        Button(
                            onClick = { viewModel.previewPersonaVoiceShort() },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isPreviewingPersonaVoice,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isPreviewingPersonaVoice) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isPreviewingPersonaVoice) stringResource(R.string.intel_playing) else stringResource(R.string.intel_preview_persona_voice))
                        }
                    }
                }

                IntelligenceHealthView(viewModel, onWipeBrainMemory)

                Spacer(modifier = Modifier.height(16.dp))

                // API Key
                Text(stringResource(R.string.settings_api_credentials), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                val apiKey by viewModel.geminiApiKey.collectAsState()
                val apiCloudTtsKey by viewModel.cloudTtsApiKey.collectAsState()
                val isKeyValidating by viewModel.isKeyValidating.collectAsState()
                val keyValidationResult by viewModel.keyValidationResult.collectAsState()
                val keyValidationError by viewModel.keyValidationError.collectAsState()
                val isCloudTtsKeyTesting by viewModel.isCloudTtsKeyTesting.collectAsState()
                val cloudTtsKeyTestResult by viewModel.cloudTtsKeyTestResult.collectAsState()

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { viewModel.updateGeminiApiKey(it) },
                        label = { Text(stringResource(R.string.settings_gemini_api_key)) },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        trailingIcon = {
                            when {
                                isKeyValidating -> CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                keyValidationResult == true -> Icon(Icons.Default.CheckCircle, "Valid", tint = MaterialTheme.colorScheme.primary)
                                keyValidationResult == false -> Icon(Icons.Default.Warning, "Invalid", tint = MaterialTheme.typography.bodySmall.color)
                            }
                        }
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { viewModel.validateApiKey() },
                            enabled = apiKey.isNotBlank() && !isKeyValidating
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.settings_test_api_key))
                        }
                        
                        TextButton(
                            onClick = { 
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/app/apikey"))
                                context.startActivity(intent)
                            }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.settings_get_key))
                        }
                    }

                    keyValidationError?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(stringResource(R.string.settings_google_cloud_tts_label), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = apiCloudTtsKey,
                        onValueChange = { viewModel.updateCloudTtsApiKey(it) },
                        label = { Text(stringResource(R.string.settings_google_cloud_tts_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { viewModel.testCloudTtsApiKey() },
                            enabled = apiCloudTtsKey.isNotBlank() && !isCloudTtsKeyTesting
                        ) {
                            if (isCloudTtsKeyTesting) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isCloudTtsKeyTesting) stringResource(R.string.settings_testing_cloud_tts) else stringResource(R.string.settings_test_api_key))
                        }

                        TextButton(
                            onClick = {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://console.cloud.google.com/flows/enableapi?apiid=texttospeech.googleapis.com")
                                )
                                context.startActivity(intent)
                            }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.settings_get_key))
                        }
                    }

                    cloudTtsKeyTestResult?.let { result ->
                        Text(
                            text = result,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }

                    Text(
                        text = stringResource(R.string.intel_keys_explanation),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            ExpandableSection(
                icon = Icons.Default.Info,
                title = stringResource(R.string.settings_section_help_system),
                isExpanded = expandedSections.contains("HELP"),
                onToggle = { viewModel.toggleSection("HELP") },
                sectionId = "HELP",
                headerModifier = Modifier.onGloballyPositioned { coords ->
                    val offset = (coords.positionInRoot().y - scrollColumnTop + scrollState.value).toInt()
                    sectionScrollOffsets = sectionScrollOffsets + ("HELP" to offset)
                    if (pendingScrollToSectionId == "HELP") {
                        pendingScrollToSectionId = null
                        coroutineScope.launch {
                            val topPaddingPx = with(density) { 24.dp.toPx() }
                            scrollState.animateScrollTo((offset - topPaddingPx).toInt().coerceIn(0, scrollState.maxValue))
                        }
                    }
                }
            ) {
                var showDangerZone by remember { mutableStateOf(false) }
                var showDemoDialog by remember { mutableStateOf(false) }
                var showLanguageDialog by remember { mutableStateOf(false) }
                val activity = (context as? android.app.Activity) ?: (context as? android.content.ContextWrapper)?.baseContext as? android.app.Activity

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        // App language
                        val languageSubtitle = when (appLanguage) {
                            "he" -> stringResource(R.string.settings_language_hebrew)
                            "en" -> stringResource(R.string.settings_language_english)
                            else -> stringResource(R.string.settings_language_system)
                        }
                        Row(
                            modifier = Modifier
                                .clickable { showLanguageDialog = true }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.settings_language), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(languageSubtitle, style = MaterialTheme.typography.bodySmall)
                            }
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                        }

                        if (showLanguageDialog) {
                            // #region agent log
                            debugLog(context, "E", "language dialog opened", mapOf("currentAppLanguage" to appLanguage))
                            // #endregion
                            AlertDialog(
                                onDismissRequest = { showLanguageDialog = false },
                                title = { Text(stringResource(R.string.settings_language)) },
                                text = {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        listOf("system" to R.string.settings_language_system, "en" to R.string.settings_language_english, "he" to R.string.settings_language_hebrew).forEach { (value, labelRes) ->
                                            val isSelected = appLanguage == value
                                            TextButton(
                                                onClick = {
                                                    showLanguageDialog = false
                                                    coroutineScope.launch {
                                                        debugLog(context, "Settings", "language_option_clicked", mapOf("value" to value, "activityNonNull" to (activity != null).toString()))
                                                        debugLog(context, "Settings", "language_before_setAppLanguageAndAwait", mapOf("value" to value))
                                                        viewModel.setAppLanguageAndAwait(value)
                                                        debugLog(context, "Settings", "language_after_setAppLanguageAndAwait", mapOf("value" to value))
                                                        withContext(Dispatchers.Main) {
                                                            val tag = when (value) {
                                                                "he" -> "he"
                                                                "en" -> "en"
                                                                else -> ""
                                                            }
                                                            debugLog(context, "Settings", "language_before_setApplicationLocales", mapOf("tag" to tag))
                                                            AppCompatDelegate.setApplicationLocales(
                                                                if (tag.isEmpty()) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag)
                                                            )
                                                            val act = activity
                                                            debugLog(context, "Settings", "language_before_restart_activity", mapOf("tag" to tag, "activityNonNull" to (act != null).toString()))
                                                            if (act != null) {
                                                                val component = ComponentName(act, com.elroi.lemurloop.MainActivity::class.java)
                                                                act.startActivity(Intent.makeRestartActivityTask(component))
                                                                act.finish()
                                                                debugLog(context, "Settings", "language_after_finish", mapOf("tag" to tag))
                                                                // Force process kill so the next launch is a single cold start; avoids double
                                                                // MainActivity creation and ensures the visible UI uses our localized resources.
                                                                android.os.Process.killProcess(android.os.Process.myPid())
                                                            }
                                                        }
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(stringResource(labelRes))
                                                if (isSelected) {
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                                                }
                                            }
                                        }
                                    }
                                },
                                confirmButton = { TextButton(onClick = { showLanguageDialog = false }) { Text(stringResource(R.string.btn_cancel)) } }
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        // About
                        Row(
                            modifier = Modifier
                                .clickable { onNavigateToAbout() }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.settings_about_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(stringResource(R.string.settings_about_version_credits, versionDisplay), style = MaterialTheme.typography.bodySmall)
                            }
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                        }
                        
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                                        
                        // Replay Tutorial
                        Row(
                            modifier = Modifier
                                .clickable { onNavigateToOnboarding() }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.settings_onboarding_replay), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(stringResource(R.string.settings_onboarding_replay_desc), style = MaterialTheme.typography.bodySmall)
                            }
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        // Create Demo Alarms
                        Row(
                            modifier = Modifier
                                .clickable { showDemoDialog = true }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Alarm, contentDescription = null, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.settings_demo_alarms_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    stringResource(R.string.settings_demo_alarms_desc),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        // Privacy Policy
                        val privacyPolicyContextInside = androidx.compose.ui.platform.LocalContext.current
                        Row(
                            modifier = Modifier
                                .clickable { 
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://elroi.github.io/LemurLoop/privacy-policy/"))
                                    privacyPolicyContextInside.startActivity(intent)
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.settings_privacy_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(stringResource(R.string.settings_privacy_desc), style = MaterialTheme.typography.bodySmall)
                            }
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        // Diagnostic Logs
                        Row(
                            modifier = Modifier
                                .clickable { onNavigateToLogs() }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.settings_diagnostic_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(stringResource(R.string.settings_diagnostic_desc), style = MaterialTheme.typography.bodySmall)
                            }
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Danger Zone
                OutlinedButton(
                    onClick = { showDangerZone = !showDangerZone },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                ) {
                    Text(if (showDangerZone) stringResource(R.string.settings_danger_zone_hide) else stringResource(R.string.settings_danger_zone_show))
                }

                AnimatedVisibility(visible = showDangerZone) {
                    Card(
                        modifier = Modifier.padding(top = 8.dp).fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(stringResource(R.string.settings_danger_zone), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.settings_danger_zone_desc), style = MaterialTheme.typography.bodySmall)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = onWipeAllData,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.settings_wipe_data))
                            }
                        }
                    }
                }

                if (showDemoDialog) {
                    AlertDialog(
                        onDismissRequest = { showDemoDialog = false },
                        title = { Text(stringResource(R.string.settings_demo_dialog_title)) },
                        text = {
                            Text(stringResource(R.string.settings_demo_dialog_text))
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                showDemoDialog = false
                                viewModel.seedDemoAlarms()
                            }) {
                                Text(stringResource(R.string.settings_demo_alarms_title))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDemoDialog = false }) {
                                Text(stringResource(R.string.btn_cancel))
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            }
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
    onClick: () -> Unit,
    onPreviewClick: (() -> Unit)? = null
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                            contentDescription = stringResource(R.string.content_desc_selected),
                            tint = info.color
                        )
                    }
                }

                if (onPreviewClick != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        androidx.compose.material3.IconButton(
                            onClick = { onPreviewClick() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = stringResource(R.string.content_desc_preview_persona),
                                tint = info.color
                            )
                        }
                        Text(
                            text = stringResource(R.string.settings_persona_preview_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun IntelligenceHealthView(viewModel: SettingsViewModel, onWipeBrainMemory: () -> Unit) {
    val status by viewModel.combinedHealthStatus.collectAsState()
    val error by viewModel.briefingError.collectAsState()
    val generatingProgress by viewModel.generatingProgress.collectAsState()
    val isGenerating by viewModel.isBriefingGenerating.collectAsState()
    val lastScript by viewModel.lastBriefingScript.collectAsState()
    var showFullError by remember { mutableStateOf(false) }
    var showFullScript by remember { mutableStateOf(false) }
    
    val statusParts = status.split("|").associate { 
        val kv = it.split(":")
        kv[0] to (kv.getOrNull(1) ?: "unknown")
    }
    
    val aiState = statusParts["ai"] ?: "pending"
    val containerColor = when(aiState) {
        "ok" -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        "draft" -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(16.dp),
        border = if (aiState == "draft") BorderStroke(1.dp, MaterialTheme.colorScheme.error) else null
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.settings_intelligence_health), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (isGenerating) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                }
            }
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HealthIndicator(stringResource(R.string.settings_health_weather), statusParts["weather"] ?: "pending", Modifier.weight(1f))
                HealthIndicator(stringResource(R.string.settings_health_calendar), statusParts["calendar"] ?: "pending", Modifier.weight(1f))
                HealthIndicator(stringResource(R.string.settings_health_ai_brain), aiState, Modifier.weight(1f))
                HealthIndicator(stringResource(R.string.settings_health_ai_voice), aiState, Modifier.weight(1f))
            }

            error?.takeIf { it.isNotBlank() }?.let { e ->
                Text(
                    text = stringResource(R.string.settings_issue_tap_details, e.take(80)),
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
                        title = { Text(stringResource(R.string.settings_diagnostic_log_title)) },
                        text = { 
                            val scroll = rememberScrollState()
                            Text(
                                text = fullError,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.verticalScroll(scroll)
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = { showFullError = false }) { Text(stringResource(R.string.btn_close)) }
                        }
                    )
                }
            }

            if (showFullScript) {
                lastScript?.let { script ->
                    AlertDialog(
                        onDismissRequest = { showFullScript = false },
                        title = { Text(stringResource(R.string.settings_briefing_preview_title)) },
                        text = { 
                            val scroll = rememberScrollState()
                            Text(
                                text = script,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.verticalScroll(scroll)
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = { showFullScript = false }) { Text(stringResource(R.string.btn_close)) }
                        }
                    )
                }
            }

            Button(
                onClick = { 
                    viewModel.saveSettings()
                    viewModel.testIntelligenceHealth() 
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isGenerating,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isGenerating) (if (generatingProgress == "Generating..." || generatingProgress.isBlank()) stringResource(R.string.settings_generating) else generatingProgress) else stringResource(R.string.settings_test_intelligence_health))
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
                Text(stringResource(R.string.settings_reset_brain))
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
    alarmDefaults: com.elroi.lemurloop.domain.manager.AlarmDefaults,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    var promptCoach by remember { mutableStateOf(alarmDefaults.promptCoach) }
    var promptComedian by remember { mutableStateOf(alarmDefaults.promptComedian) }
    var promptZen by remember { mutableStateOf(alarmDefaults.promptZen) }
    var promptHypeman by remember { mutableStateOf(alarmDefaults.promptHypeman) }

    val defaultPromptCoach = stringResource(R.string.persona_prompt_coach)
    val defaultPromptComedian = stringResource(R.string.persona_prompt_comedian)
    val defaultPromptZen = stringResource(R.string.persona_prompt_zen)
    val defaultPromptHypeman = stringResource(R.string.persona_prompt_hypeman)

    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(R.string.settings_edit_prompts),
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
                        promptCoach = defaultPromptCoach
                        promptComedian = defaultPromptComedian
                        promptZen = defaultPromptZen
                        promptHypeman = defaultPromptHypeman
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
@Composable
fun ExpandableSection(
    icon: ImageVector,
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    sectionId: String? = null,
    headerModifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpanded) 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) 
            else 
                MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(headerModifier)
                    .clickable { onToggle() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            icon, 
                            contentDescription = null, 
                            modifier = Modifier.size(18.dp),
                            tint = if (isExpanded) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isExpanded) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp)
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
fun PreviewBriefingDialog(
    script: String,
    onDismiss: () -> Unit
) {
    val paragraphs = script.split(Regex("\n+")).filter { it.isNotBlank() }
    val firstLine = paragraphs.firstOrNull() ?: ""
    val sourceLabel = when {
        firstLine.contains("☁️") -> "Source: Cloud AI"
        firstLine.contains("✈") -> "Source: Local AI (offline)"
        firstLine.contains("✅") -> "Source: Simple (no AI)"
        else -> null
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(stringResource(R.string.settings_briefing_preview))
                if (sourceLabel != null) {
                    Text(
                        text = sourceLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        text = {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                paragraphs.forEach { paragraph ->
                    Text(
                        text = paragraph.trim(),
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_close))
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}
