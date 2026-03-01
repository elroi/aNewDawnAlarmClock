package com.elroi.alarmpal.ui.screen.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Help & Features") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            HelpSection(
                title = "Wake-Up Briefing Generation",
                content = "The AlarmPal intelligence generates your custom wake-up briefing specifically tailored to you. To ensure your briefing is ready to read immediately when you successfully dismiss the alarm, the invisible generation process actually starts in the background the exact moment your alarm begins to ring. By the time you finish your math or face challenges, your briefing is already pre-computed and awaiting your attention!"
            )
            
            HelpSection(
                title = "Evasive Snooze",
                content = "If you find yourself mindlessly hitting snooze, this feature is for you. Evasive Snooze makes the snooze button jump to random spots on the screen each time you try to press it, forcing your brain to wake up just a little bit more to track it."
            )
            
            HelpSection(
                title = "Gentle Wake (Crescendo)",
                content = "Instead of jarring you awake at maximum volume, Gentle Wake starts your alarm completely silently and slowly increases the volume over your specified duration. This provides a much more natural and less stressful waking experience."
            )
            
            HelpSection(
                title = "Face Game Dismissal",
                content = "This dismissal method uses your phone's front camera and requires you to mimic 3 random facial expressions (like smiling, winking, or blinking). Moving your facial muscles is proven to increase blood flow to the brain, helping you wake up faster!"
            )
            
            HelpSection(
                title = "Smart Wake-Up (Accountability)",
                content = "Ever dismiss an alarm and immediately fall back asleep? Smart Wake-Up checks on you a few minutes after you dismiss your alarm. If you don't respond to the prompt in time, it assumes you fell asleep and can even automatically text an accountability buddy to call and wake you up."
            )
        }
    }
}

@Composable
fun HelpSection(title: String, content: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 22.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
