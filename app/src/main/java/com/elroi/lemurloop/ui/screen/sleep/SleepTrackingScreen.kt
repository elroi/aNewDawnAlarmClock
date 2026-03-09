package com.elroi.lemurloop.ui.screen.sleep

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.elroi.lemurloop.service.SleepTrackingService

@Composable
fun SleepTrackingScreen() {
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "Sleep Tracking", style = MaterialTheme.typography.headlineLarge)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                val intent = Intent(context, SleepTrackingService::class.java)
                if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.FOREGROUND_SERVICE) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    context.startForegroundService(intent)
                }
            }) {
                Text("Start Sleep Tracking")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                val intent = Intent(context, SleepTrackingService::class.java).apply {
                    action = "STOP"
                }
                context.startService(intent)
            }) {
                Text("Stop Tracking")
            }
        }
    }
}
