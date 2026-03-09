package com.elroi.lemurloop.ui.screen.settings

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
                content = "The LemurLoop intelligence starts generating your custom briefing the moment your alarm rings. It gathers weather data, your calendar events, and a fun fact, then uses advanced AI to rewrite it in your selected persona's voice. By the time you dismiss the alarm, your briefing is ready! Note: Cloud AI requires a stable internet connection."
            )
            
            HelpSection(
                title = "Evasive Snooze",
                content = "For those who snooze in their sleep! Enable this to make the snooze button jump to a different random location on the screen every time you try to tap it. This forces you to focus your eyes and track the button, making it much harder to accidentally fall back asleep."
            )
            
            HelpSection(
                title = "Gentle Wake (Crescendo)",
                content = "A peaceful way to start your day. Instead of a sudden loud noise, Gentle Wake starts your chosen alarm sound at 0% volume and gradually increases it to maximum over several minutes. You can customize the ramp-up duration in the advanced settings."
            )
            
            HelpSection(
                title = "Smooth Fade-Out",
                content = "When you dismiss or snooze your alarm, the sound won't just 'snap' off. LemurLoop gently fades the volume down over one second, providing a smoother transition from your alarm to your briefing or extra sleep."
            )
            
            HelpSection(
                title = "Math & Face Challenges",
                content = "Prove you're awake! Math challenges require solving problems of your chosen difficulty. Face challenges use your front camera to ensure you're making specific expressions. Moving your facial muscles and performing mental arithmetic are scientifically proven to help wake up your brain."
            )
            
            HelpSection(
                title = "Smart Wake-Up (Accountability)",
                content = "The ultimate safety net. A few minutes after you dismiss your alarm, LemurLoop will send you a 'Pulse Check'. If you don't respond within 60 seconds, it assumes you fell back asleep. It will then automatically trigger a loud secondary alarm or even text an alert to your chosen Accountability Buddy."
            )

            HelpSection(
                title = "Accountability Buddy",
                content = "Add a trusted friend as your buddy. If you fail to respond to a Smart Wake-Up check, your buddy will receive a text message (standard SMS rates apply) letting them know you're stuck in bed and need a real-life wake-up call!"
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
