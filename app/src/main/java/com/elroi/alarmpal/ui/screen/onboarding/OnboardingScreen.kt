package com.elroi.alarmpal.ui.screen.onboarding

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import com.elroi.alarmpal.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.zIndex
import com.elroi.alarmpal.ui.viewmodel.OnboardingViewModel
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    isReplay: Boolean = false,
    onFinished: (Boolean) -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    var showInternalSplash by remember { mutableStateOf(isReplay) }

    LaunchedEffect(isReplay) {
        if (isReplay) {
            kotlinx.coroutines.delay(1500)
            showInternalSplash = false
        }
    }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var currentPage by remember { mutableStateOf(0) }
    val totalPages = 6

    val userName by viewModel.userName.collectAsState()

    // Permission launchers
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* result shown via permission check on re-enter */ }

    val calendarLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* no-op */ }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted -> 
        if (isGranted) {
            viewModel.setAutoLocation(true)
        }
    }

    val smsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* result shown via permission check */ }

    var grantedCount by remember { mutableStateOf(0) }
    var totalCount by remember { mutableStateOf(0) }
    
    var briefingGrantedCount by remember { mutableStateOf(0) }
    var briefingTotalCount by remember { mutableStateOf(0) }
    
    var isSmsPermissionGranted by remember { mutableStateOf(false) }
    
    fun handleBack() {
        if (currentPage > 0) currentPage--
    }

    fun handleNext(createAlarm: Boolean = true) {
        when (currentPage) {
            1 -> {
                // Permissions Page Logic
                if (createAlarm) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val granted = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.POST_NOTIFICATIONS
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                        if (!granted) {
                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            return
                        }
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val alarmManager = context.getSystemService(AlarmManager::class.java)
                        if (!alarmManager.canScheduleExactAlarms()) {
                            context.startActivity(
                                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                                    .setData(Uri.parse("package:${context.packageName}"))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                            return
                        }
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                        return
                    }
                }
                currentPage++
            }
            2 -> {
                // Name & Extras Page - Request permissions sequentially
                val calGranted = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.READ_CALENDAR
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                if (!calGranted && createAlarm) {
                    calendarLauncher.launch(Manifest.permission.READ_CALENDAR)
                    return
                }

                val locGranted = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_COARSE_LOCATION
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                if (!locGranted && createAlarm) {
                    locationLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                    return
                }
                
                currentPage++
            }
            3 -> {
                // AI Features Page - no special permissions needed
                currentPage++
            }
            4 -> {
                // Buddy Page
                val smsGranted = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.SEND_SMS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                
                if (!smsGranted && createAlarm) {
                    smsLauncher.launch(arrayOf(Manifest.permission.SEND_SMS, Manifest.permission.RECEIVE_SMS))
                    return
                }
                currentPage++
            }
            totalPages - 1 -> {
                coroutineScope.launch {
                    viewModel.completeOnboarding()
                    onFinished(createAlarm)
                }
            }
            else -> {
                if (currentPage < totalPages - 1) currentPage++
            }
        }
    }

    // Handle system back press
    BackHandler(enabled = currentPage > 0) {
        handleBack()
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                var granted = 0
                var total = 0
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    total++
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) granted++
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    total++
                    val alarmManager = context.getSystemService(AlarmManager::class.java)
                    if (alarmManager.canScheduleExactAlarms()) granted++
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    total++
                    if (Settings.canDrawOverlays(context)) granted++
                }
                
                grantedCount = granted
                totalCount = total

                // Track Briefing Permissions (Page 3)
                var bGranted = 0
                var bTotal = 2 // Calendar and Weather (Location)
                
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == android.content.pm.PackageManager.PERMISSION_GRANTED) bGranted++
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) bGranted++
                
                briefingGrantedCount = bGranted
                briefingTotalCount = bTotal

                // Track SMS Permission (Page 4)
                isSmsPermissionGranted = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.SEND_SMS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .pointerInput(Unit) {
                var offsetX = 0f
                detectHorizontalDragGestures(
                    onDragStart = { offsetX = 0f },
                    onDragEnd = {
                        if (offsetX > 150f) {
                            handleBack()
                        } else if (offsetX < -150f) {
                            handleNext()
                        }
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount
                    }
                )
            }
    ) {
        // Internal Splash Screen for Replay
        androidx.compose.animation.AnimatedVisibility(
            visible = showInternalSplash,
            enter = androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.fadeOut(),
            modifier = Modifier.zIndex(10f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFFFF8E1)), // LemurLoop Cream
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = R.mipmap.ic_launcher_foreground),
                    contentDescription = "LemurLoop Logo",
                    modifier = Modifier.size(192.dp)
                )
            }
        }
        // Back Button
        if (currentPage > 0) {
            TextButton(
                onClick = { handleBack() },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(stringResource(R.string.onboarding_back))
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // Page indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(totalPages) { index ->
                    val isActive = index == currentPage
                    val width by animateDpAsState(
                        targetValue = if (isActive) 24.dp else 8.dp,
                        animationSpec = tween(300),
                        label = "indicator"
                    )
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(width)
                            .clip(CircleShape)
                            .background(
                                if (isActive) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Animated content transition
            AnimatedContent<Int>(
                targetState = currentPage,
                transitionSpec = {
                    if (targetState > initialState) {
                        (fadeIn(tween(400)) + slideInHorizontally(tween(400)) { it / 2 })
                            .togetherWith(fadeOut(tween(400)) + slideOutHorizontally(tween(400)) { -it / 2 })
                    } else {
                        (fadeIn(tween(400)) + slideInHorizontally(tween(400)) { -it / 2 })
                            .togetherWith(fadeOut(tween(400)) + slideOutHorizontally(tween(400)) { it / 2 })
                    }
                },
                label = "page_content",
                modifier = Modifier.fillMaxWidth()
            ) { targetPage ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val emoji: String
                    val title: String
                    val body: String
                    val primaryLabel: String
                    val onPrimary: () -> Unit
                    val secondaryLabel: String?
                    val onSecondary: (() -> Unit)?
                    var customContent: (@Composable () -> Unit)? = null

                    when (targetPage) {
                        0 -> {
                            emoji = "⏰"
                            title = stringResource(R.string.onboarding_1_title)
                            body = stringResource(R.string.onboarding_1_body)
                            primaryLabel = stringResource(R.string.onboarding_1_primary)
                            onPrimary = { handleNext() }
                            secondaryLabel = null
                            onSecondary = null
                        }
                        1 -> {
                            emoji = "🛡️"
                            title = stringResource(R.string.onboarding_2_title)
                            body = stringResource(R.string.onboarding_2_body)
                            primaryLabel = if (grantedCount == totalCount && totalCount > 0)
                                stringResource(R.string.onboarding_2_continue)
                            else
                                stringResource(R.string.onboarding_2_primary)
                            onPrimary = { handleNext() }
                            secondaryLabel = stringResource(R.string.onboarding_2_secondary)
                            onSecondary = { handleNext(false) }
                            customContent = {
                                if (totalCount > 0) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "Core Setup: $grantedCount / $totalCount",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            textAlign = TextAlign.Center
                                        )

                                        Spacer(modifier = Modifier.height(16.dp))

                                        // Permission Checklist
                                        val notifGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                        } else true

                                        val alarmGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                            context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
                                        } else true

                                        val overlayGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                            Settings.canDrawOverlays(context)
                                        } else true

                                        PermissionStatusRow(
                                            icon = "🔔",
                                            title = stringResource(R.string.onboarding_2_permission_notifications),
                                            desc = stringResource(R.string.onboarding_2_desc_notifications),
                                            isGranted = notifGranted
                                        )
                                        PermissionStatusRow(
                                            icon = "⏰",
                                            title = stringResource(R.string.onboarding_2_permission_alarms),
                                            desc = stringResource(R.string.onboarding_2_desc_alarms),
                                            isGranted = alarmGranted
                                        )
                                        PermissionStatusRow(
                                            icon = "🪟",
                                            title = stringResource(R.string.onboarding_2_permission_overlay),
                                            desc = stringResource(R.string.onboarding_2_desc_overlay),
                                            isGranted = overlayGranted
                                        )

                                        Spacer(modifier = Modifier.height(16.dp))
                                        
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(0.6f)
                                                .height(8.dp)
                                                .background(
                                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), 
                                                    CircleShape
                                                )
                                                .clip(CircleShape)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth(if (totalCount > 0) grantedCount.toFloat() / totalCount else 0f)
                                                    .fillMaxHeight()
                                                    .background(MaterialTheme.colorScheme.primary)
                                            )
                                        }
                                        
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = if (grantedCount == totalCount) "All set! Click below to continue." else "Grant permissions to proceed",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                        2 -> {
                            emoji = "🌤️"
                            title = stringResource(R.string.onboarding_3_title)
                            body = stringResource(R.string.onboarding_3_body) + "\n" + stringResource(R.string.onboarding_3_personas)
                            primaryLabel = if (briefingGrantedCount == briefingTotalCount)
                                stringResource(R.string.onboarding_3_continue)
                            else
                                stringResource(R.string.onboarding_3_primary)
                            onPrimary = { handleNext() }
                            secondaryLabel = stringResource(R.string.onboarding_3_secondary)
                            onSecondary = { handleNext(false) }
                            customContent = {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Text(
                                        stringResource(R.string.onboarding_3_label_name),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                        OutlinedTextField(
                                            value = userName,
                                            onValueChange = { viewModel.updateUserName(it) },
                                            placeholder = { 
                                                Text(
                                                    stringResource(R.string.onboarding_3_hint_name),
                                                    modifier = Modifier.fillMaxWidth(),
                                                    textAlign = TextAlign.Center
                                                ) 
                                            },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth(),
                                            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                    }

                                    // Briefing Setup Progress Card
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = stringResource(R.string.onboarding_3_setup_progress, briefingGrantedCount, briefingTotalCount),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            textAlign = TextAlign.Center
                                        )
                                        
                                        Spacer(modifier = Modifier.height(16.dp))

                                        // Permission Checklist
                                        val calGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                        val locGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED

                                        PermissionStatusRow(
                                            icon = "📅",
                                            title = stringResource(R.string.onboarding_3_permission_calendar),
                                            desc = stringResource(R.string.onboarding_3_desc_calendar),
                                            isGranted = calGranted
                                        )
                                        PermissionStatusRow(
                                            icon = "🌤️",
                                            title = stringResource(R.string.onboarding_3_permission_weather),
                                            desc = stringResource(R.string.onboarding_3_desc_weather),
                                            isGranted = locGranted
                                        )

                                        Spacer(modifier = Modifier.height(16.dp))
                                        
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(0.6f)
                                                .height(8.dp)
                                                .background(
                                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), 
                                                    CircleShape
                                                )
                                                .clip(CircleShape)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth(if (briefingTotalCount > 0) briefingGrantedCount.toFloat() / briefingTotalCount else 0f)
                                                    .fillMaxHeight()
                                                    .background(MaterialTheme.colorScheme.primary)
                                            )
                                        }
                                        
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = if (briefingGrantedCount == briefingTotalCount) 
                                                stringResource(R.string.onboarding_3_setup_complete) 
                                            else 
                                                stringResource(R.string.onboarding_3_setup_instructions),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                        3 -> {
                            emoji = "🧠"
                            title = stringResource(R.string.onboarding_ai_title)
                            body = stringResource(R.string.onboarding_ai_body)
                            primaryLabel = stringResource(R.string.onboarding_ai_primary)
                            onPrimary = { handleNext() }
                            secondaryLabel = null
                            onSecondary = null
                        }
                        4 -> {
                            emoji = "🤝"
                            title = stringResource(R.string.onboarding_4_title)
                            body = stringResource(R.string.onboarding_4_body)
                            
                            val smsGranted = isSmsPermissionGranted
                            
                            primaryLabel = if (smsGranted) stringResource(R.string.onboarding_4_primary) else stringResource(R.string.onboarding_4_enable)
                            onPrimary = { 
                                if (smsGranted) handleNext() 
                                else smsLauncher.launch(arrayOf(Manifest.permission.SEND_SMS, Manifest.permission.RECEIVE_SMS))
                            }
                            secondaryLabel = if (smsGranted) null else stringResource(R.string.onboarding_4_secondary)
                            onSecondary = { handleNext(false) }
                            customContent = {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    PermissionStatusRow(
                                        icon = "✉️",
                                        title = stringResource(R.string.onboarding_4_permission_sms),
                                        desc = stringResource(R.string.onboarding_4_desc_sms),
                                        isGranted = isSmsPermissionGranted
                                    )
                                }
                            }
                        }
                        else -> {
                            emoji = "🚀"
                            title = stringResource(R.string.onboarding_5_title)
                            body = stringResource(R.string.onboarding_5_body)
                            primaryLabel = stringResource(R.string.onboarding_5_primary)
                            onPrimary = { handleNext(true) }
                            secondaryLabel = stringResource(R.string.onboarding_5_secondary)
                            onSecondary = { handleNext(false) }
                            customContent = null
                        }
                    }
                    
                    // Render the page content
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(32.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = emoji,
                            fontSize = 56.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = body,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 24.sp
                    )

                    customContent?.invoke()

                    Spacer(modifier = Modifier.height(48.dp))

                    Button(
                        onClick = onPrimary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = primaryLabel,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (secondaryLabel != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        TextButton(
                            onClick = onSecondary ?: {},
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = secondaryLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }
}

@Composable
private fun PermissionStatusRow(icon: String, title: String, desc: String, isGranted: Boolean) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isGranted) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(icon, fontSize = 24.sp, modifier = Modifier.padding(end = 12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isGranted) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isGranted) {
                Text("✅", fontSize = 16.sp)
            }
        }
    }
}

private data class OnboardingPage(
    val emoji: String,
    val title: String,
    val body: String,
    val primaryLabel: String,
    val onPrimary: () -> Unit,
    val secondaryLabel: String?,
    val onSecondary: (() -> Unit)? = null,
    val customContent: (@Composable () -> Unit)? = null
)
