package com.elroi.alarmpal.ui.screen.alarm

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.elroi.alarmpal.R
import com.elroi.alarmpal.domain.model.Alarm
import com.elroi.alarmpal.ui.components.BuddySelectionDialog
import com.elroi.alarmpal.ui.components.ImprovedDaySelector
import com.elroi.alarmpal.ui.viewmodel.AlarmViewModel
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

@Immutable
data class Persona(val id: String, val title: String, val emoji: String)

val personas = listOf(
    Persona("COACH", "Drill Sergeant", "🪖"),
    Persona("COMEDIAN", "Sarcastic Friend", "🤡"),
    Persona("ZEN", "Zen Master", "🧘"),
    Persona("HYPEMAN", "Hype-Man", "🚀"),
    Persona("SURPRISE", "Surprise Me", "🎲")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmCreationWizard(
    onFinished: () -> Unit,
    onBack: () -> Unit,
    viewModel: AlarmViewModel = hiltViewModel()
) {
    val coroutineScope = rememberCoroutineScope()
    val defaultSettings by viewModel.defaultAlarmSettings.collectAsState()
    var currentPage by remember { mutableIntStateOf(0) }
    val totalPages = 4

    // Initial state setup
    var selectedTime by remember { mutableStateOf(LocalTime.now().withSecond(0).withNano(0)) }
    var selectedDays by remember { mutableStateOf(listOf<Int>()) }
    var alarmLabel by remember { mutableStateOf("") }
    
    var selectedPersona by remember { mutableStateOf(defaultSettings.aiPersona) }
    var isBriefingEnabled by remember { mutableStateOf(defaultSettings.isBriefingEnabled) }
    var isTtsEnabled by remember { mutableStateOf(defaultSettings.isTtsEnabled) }
    var isSoundEnabled by remember { mutableStateOf(defaultSettings.isSoundEnabled) }
    var isVibrate by remember { mutableStateOf(defaultSettings.isVibrate) }
    var isGentleWake by remember { mutableStateOf(defaultSettings.isGentleWake) }

    var selectedMathDifficulty by remember { mutableIntStateOf(defaultSettings.mathDifficulty) }
    var mathProblemCount by remember { mutableIntStateOf(defaultSettings.mathProblemCount) }
    var mathGraduallyIncreaseDifficulty by remember { mutableStateOf(defaultSettings.mathGraduallyIncreaseDifficulty) }
    var smileToDismiss by remember { mutableStateOf(defaultSettings.smileToDismiss) }
    var smileFallbackMethod by remember { mutableStateOf(defaultSettings.smileFallbackMethod) }
    
    var buddyPhone by remember { mutableStateOf("") }
    var buddyName by remember { mutableStateOf("") }
    var showBuddyDialog by remember { mutableStateOf(false) }
    val globalBuddies by viewModel.globalBuddies.collectAsState()

    // Sync persona with defaults when loaded
    LaunchedEffect(defaultSettings) {
        if (selectedPersona == "COACH" && defaultSettings.aiPersona != "COACH") {
            selectedPersona = defaultSettings.aiPersona
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
            val newAlarm = Alarm(
                time = selectedTime,
                daysOfWeek = selectedDays,
                label = if (alarmLabel.isBlank()) null else alarmLabel,
                mathDifficulty = selectedMathDifficulty,
                mathProblemCount = mathProblemCount,
                mathGraduallyIncreaseDifficulty = mathGraduallyIncreaseDifficulty,
                buddyPhoneNumber = if (buddyPhone.isBlank()) null else buddyPhone,
                buddyName = if (buddyName.isBlank()) null else buddyName,
                smileToDismiss = smileToDismiss,
                smileFallbackMethod = smileFallbackMethod,
                isBriefingEnabled = isBriefingEnabled,
                isTtsEnabled = isTtsEnabled,
                isSoundEnabled = isSoundEnabled,
                isVibrate = isVibrate,
                isGentleWake = isGentleWake
            )
            viewModel.addAlarm(newAlarm)
            onFinished()
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
                    if (dragAmount > 50) coroutineScope.launch { handleBack() }
                    else if (dragAmount < -50) coroutineScope.launch { handleNext() }
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
                        2 -> Triple("🛡️", stringResource(R.string.wizard_3_title), stringResource(R.string.wizard_3_body))
                        else -> Triple("✨", stringResource(R.string.wizard_4_title), stringResource(R.string.wizard_4_body))
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
                        1 -> WakeUpStyleStep(selectedPersona, { selectedPersona = it }, isBriefingEnabled, { isBriefingEnabled = it }, isTtsEnabled, { isTtsEnabled = it }, isSoundEnabled, { isSoundEnabled = it }, isVibrate, { isVibrate = it })
                        2 -> AntiSnoozeGuardStep(buddyName, buddyPhone, { showBuddyDialog = true }, selectedMathDifficulty, { selectedMathDifficulty = it }, mathProblemCount, { mathProblemCount = it }, smileToDismiss, { smileToDismiss = it })
                        3 -> FinalSummaryStep(selectedTime, selectedDays, alarmLabel, { alarmLabel = it }, personas.find { it.id == selectedPersona } ?: personas[0], selectedMathDifficulty, smileToDismiss, buddyName)
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
            "Repeat Frequency",
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
    onVibrateChange: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Text(
            "AI Wake-Up Persona",
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
            "Wake-Up Features",
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
                    title = "Daily Briefing",
                    desc = "AI summary of your day",
                    checked = isBriefingEnabled,
                    onCheckedChange = onBriefingEnabledChange,
                    icon = "📻"
                )
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                
                FeatureToggle(
                    title = "Voice Synthesis",
                    desc = "Hear your persona speak",
                    checked = isTtsEnabled,
                    onCheckedChange = onTtsEnabledChange,
                    icon = "🗣️",
                    enabled = isBriefingEnabled
                )
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                
                FeatureToggle(
                    title = "Alarm Sound",
                    checked = isSoundEnabled,
                    onCheckedChange = onSoundEnabledChange,
                    icon = "🔔"
                )
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                
                FeatureToggle(
                    title = "Vibration",
                    checked = isVibrate,
                    onCheckedChange = onVibrateChange,
                    icon = "📳"
                )
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
                persona.title,
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
    buddyName: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Text(
            "Review & Label",
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
                            if (selectedDays.isEmpty()) "Once" else "Repeats on ${selectedDays.size} days",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                
                SummaryRow("Persona", "${persona.emoji} ${persona.title}")
                if (mathDifficulty > 0) SummaryRow("Challenge", "🧮 Math (${if(mathDifficulty==1) "Easy" else if(mathDifficulty==2) "Med" else "Hard"})")
                if (smileToDismiss) SummaryRow("Challenge", "😊 Smile to Dismiss")
                if (buddyName.isNotBlank()) SummaryRow("Guardian", "👤 $buddyName")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "Alarm Label",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = alarmLabel,
            onValueChange = onLabelChange,
            placeholder = { Text("e.g. Work Morning (Optional)") },
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
fun AntiSnoozeGuardStep(
    buddyName: String,
    buddyPhone: String,
    onBuddyClick: () -> Unit,
    mathDifficulty: Int,
    onMathDifficultyChange: (Int) -> Unit,
    mathProblemCount: Int,
    onMathProblemCountChange: (Int) -> Unit,
    smileToDismiss: Boolean,
    onSmileToDismissChange: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Text(
            "Accountability Buddy",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Surface(
            onClick = onBuddyClick,
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(if (buddyName.isNotBlank()) buddyName.take(1) else "👤", style = MaterialTheme.typography.titleLarge)
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (buddyName.isNotBlank()) buddyName else "Add Accountability Buddy",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (buddyPhone.isNotBlank()) buddyPhone else "They'll be alerted if you don't wake up",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "Wake-Up Challenges",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        // Math Challenge Card
        ChallengeCard(
            title = "Math Challenge",
            desc = "Solve problems to dismiss",
            icon = "➕",
            checked = mathDifficulty > 0,
            onCheckedChange = { if (it) onMathDifficultyChange(1) else onMathDifficultyChange(0) }
        ) {
            Column(modifier = Modifier.padding(top = 16.dp)) {
                Text("Difficulty", style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = mathDifficulty.toFloat(),
                    onValueChange = { onMathDifficultyChange(it.toInt()) },
                    valueRange = 1f..3f,
                    steps = 1
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Easy", style = MaterialTheme.typography.labelSmall)
                    Text("Medium", style = MaterialTheme.typography.labelSmall)
                    Text("Hard", style = MaterialTheme.typography.labelSmall)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Number of Problems: $mathProblemCount", style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = mathProblemCount.toFloat(),
                    onValueChange = { onMathProblemCountChange(it.toInt()) },
                    valueRange = 1f..10f,
                    steps = 8
                )
            }
        }

        // Face Challenge Card
        ChallengeCard(
            title = "Face Challenge",
            desc = "Smile to dismiss (AI verified)",
            icon = "😊",
            checked = smileToDismiss,
            onCheckedChange = onSmileToDismissChange
        ) {
            Text(
                "Our AI will use your camera to verify you're truly awake and smiling before dismissing the alarm.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
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
