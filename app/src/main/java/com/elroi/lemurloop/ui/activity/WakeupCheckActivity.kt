package com.elroi.lemurloop.ui.activity

import android.app.Activity
import android.app.AlarmManager
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elroi.lemurloop.service.AlarmIntentExtras
import com.elroi.lemurloop.ui.theme.LemurLoopTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay

@AndroidEntryPoint
class WakeupCheckActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        turnScreenOnAndKeyguardOff()
        
        setContent {
            LemurLoopTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val timeoutSeconds = intent.getIntExtra(AlarmIntentExtras.EXTRA_WAKEUP_CHECK_TIMEOUT, 60)
                    WakeupCheckScreen(
                        timeoutSeconds = timeoutSeconds,
                        onConfirmed = { finish() },
                        onExpired = { retriggerAlarm() }
                    )
                }
            }
        }
    }

    private fun retriggerAlarm() {
        val alarmId = intent.getStringExtra(AlarmIntentExtras.EXTRA_ALARM_ID) ?: return
        val alarmLabel = intent.getStringExtra(AlarmIntentExtras.EXTRA_ALARM_LABEL)
        
        val retriggerIntent = Intent(this, com.elroi.lemurloop.receiver.AlarmReceiver::class.java).apply {
            putExtras(intent)
            // Ensure we don't infinitely schedule wakeup checks if something goes wrong
            // though the current logic only schedules it on dismissal.
        }
        sendBroadcast(retriggerIntent)
        finish()
    }

    private fun turnScreenOnAndKeyguardOff() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val km = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
            km.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
    }
}

@Composable
fun WakeupCheckScreen(
    timeoutSeconds: Int,
    onConfirmed: () -> Unit,
    onExpired: () -> Unit
) {
    var timeLeft by remember { mutableIntStateOf(timeoutSeconds) }

    LaunchedEffect(Unit) {
        while (timeLeft > 0) {
            delay(1000)
            timeLeft--
        }
        onExpired()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Notifications,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Are you awake?",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Tap the button below to confirm you're awake, otherwise the alarm will re-trigger!",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Text(
            text = timeLeft.toString(),
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Black,
            color = if (timeLeft <= 10) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        )
        
        Text(
            text = "seconds remaining",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(64.dp))
        
        Button(
            onClick = onConfirmed,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Text(
                "I'm Awake!",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
