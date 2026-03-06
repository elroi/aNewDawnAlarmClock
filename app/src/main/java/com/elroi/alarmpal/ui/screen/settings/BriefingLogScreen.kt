package com.elroi.alarmpal.ui.screen.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elroi.alarmpal.domain.manager.BriefingLogEntry
import com.elroi.alarmpal.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BriefingLogScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val logs by viewModel.briefingLogs.collectAsState()
    var showClearDialog by remember { mutableStateOf(false) }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear Logs") },
            text = { Text("Delete all briefing generation logs? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearBriefingLogs()
                    showClearDialog = false
                }) { Text("Clear", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Briefing Logs") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showClearDialog = true }, enabled = logs.isNotEmpty()) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear Logs")
                    }
                }
            )
        }
    ) { padding ->
        if (logs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                    Text("No briefings logged yet", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Text("Generate a briefing to see logs here.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(logs) { log ->
                    BriefingLogCard(log)
                }
            }
        }
    }
}

@Composable
fun BriefingLogCard(log: BriefingLogEntry) {
    var expanded by remember { mutableStateOf(false) }

    val (bgColor, resultIcon) = when (log.result) {
        "SUCCESS" -> Pair(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f), Icons.Default.CheckCircle)
        else -> Pair(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f), Icons.Default.Warning)
    }

    val engineLabel = when (log.engineUsed) {
        "CLOUD" -> "☁️ Cloud (Gemini)"
        "ADVANCED" -> "🦙 Local (Gemma 2B)"
        "DRAFT_ONLY" -> "📝 Draft Only (Standard)"
        else -> log.engineUsed
    }

    Surface(
        onClick = { expanded = !expanded },
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(resultIcon, contentDescription = null, modifier = Modifier.size(18.dp),
                    tint = if (log.result == "SUCCESS") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                Column(modifier = Modifier.weight(1f)) {
                    Text(log.timeFormatted, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Text(engineLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                }
                if (log.isFallbackTriggered) {
                    Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.tertiaryContainer) {
                        Text("fallback", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall)
                    }
                }
                Icon(if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null)
            }
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(log.details, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                    if (log.briefingScript.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Briefing Script:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Text(
                            log.briefingScript,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
