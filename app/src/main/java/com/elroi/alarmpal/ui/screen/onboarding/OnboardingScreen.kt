package com.elroi.alarmpal.ui.screen.onboarding

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.elroi.alarmpal.ui.viewmodel.OnboardingViewModel
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var currentPage by remember { mutableStateOf(0) }
    val totalPages = 4

    // Permission launchers
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* result shown via permission check on re-enter */ }

    val calendarLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* no-op */ }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* no-op */ }

    val pages = listOf(
        OnboardingPage(
            emoji = "⏰",
            title = "Wake Up Smarter",
            body = "AlarmPal is the alarm that doesn't just wake you up — it briefs you. Weather, calendar, a morning mission, all delivered in the voice of a character you choose.",
            primaryLabel = "Let's Go",
            onPrimary = { currentPage++ },
            secondaryLabel = null
        ),
        OnboardingPage(
            emoji = "🔔",
            title = "Allow Notifications & Alarms",
            body = "AlarmPal needs permission to send notifications and schedule exact alarms. Without these, your alarms cannot fire.\n\nThis is required for the app to work.",
            primaryLabel = "Grant Permissions",
            onPrimary = {
                // Notification permission (Android 13+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val granted = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.POST_NOTIFICATIONS
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    if (!granted) {
                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                // Exact alarm permission (Android 12+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val alarmManager = context.getSystemService(AlarmManager::class.java)
                    if (!alarmManager.canScheduleExactAlarms()) {
                        context.startActivity(
                            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                                .setData(Uri.parse("package:${context.packageName}"))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                }
                // Overlay permission (for alarm-over-lock-screen on Android 10+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
                currentPage++
            },
            secondaryLabel = "Skip (Not Recommended)",
            onSecondary = { currentPage++ }
        ),
        OnboardingPage(
            emoji = "🌤️",
            title = "Power Up Your Briefing",
            body = "AlarmPal can read your calendar and location to give you a personalised morning briefing.\n\nThis is optional — you can always enable it later in Settings.",
            primaryLabel = "Enable Briefing Features",
            onPrimary = {
                val calGranted = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.READ_CALENDAR
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                val locGranted = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_COARSE_LOCATION
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                if (!calGranted) calendarLauncher.launch(Manifest.permission.READ_CALENDAR)
                if (!locGranted) locationLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                currentPage++
            },
            secondaryLabel = "Skip for Now",
            onSecondary = { currentPage++ }
        ),
        OnboardingPage(
            emoji = "🚀",
            title = "You're All Set!",
            body = "Create your first alarm with the ➕ button.\n\nChoose a wake-up persona in Settings to personalise your morning briefing.",
            primaryLabel = "Create My First Alarm",
            onPrimary = {
                coroutineScope.launch {
                    viewModel.completeOnboarding()
                    onFinished()
                }
            },
            secondaryLabel = null
        )
    )

    val page = pages[currentPage]

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
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(0.5f))

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

            Spacer(modifier = Modifier.weight(0.5f))

            // Emoji icon
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 4 },
                exit = fadeOut() + slideOutVertically { -it / 4 }
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = page.emoji,
                        fontSize = 56.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Title
            Text(
                text = page.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Body
            Text(
                text = page.body,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.weight(1f))

            // Primary button
            Button(
                onClick = page.onPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = page.primaryLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Secondary / skip button
            if (page.secondaryLabel != null) {
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(
                    onClick = page.onSecondary ?: {},
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = page.secondaryLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
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
    val onSecondary: (() -> Unit)? = null
)
