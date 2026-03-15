package com.elroi.lemurloop.ui.screen.alarm

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.elroi.lemurloop.R
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.elroi.lemurloop.domain.model.Alarm
import com.elroi.lemurloop.ui.viewmodel.AlarmViewModel
import java.time.format.DateTimeFormatter
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import com.elroi.lemurloop.util.AlarmUtils
import java.time.LocalDateTime
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AlarmListScreen(
    onNavigateToDetail: (String?) -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: AlarmViewModel = hiltViewModel()
) {
    val alarms by viewModel.alarms.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // Split and sort alarms
    val activeAlarms = remember(alarms) {
        alarms.filter { it.isEnabled }.sortedWith(compareBy({ it.time.hour }, { it.time.minute }))
    }
    val inactiveAlarms = remember(alarms) {
        alarms.filter { !it.isEnabled }.sortedWith(compareBy({ it.time.hour }, { it.time.minute }))
    }
    
    var alarmToDelete by remember { mutableStateOf<Alarm?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    if (alarmToDelete != null) {
        AlertDialog(
            onDismissRequest = { alarmToDelete = null },
            title = { Text(stringResource(R.string.dialog_delete_alarm_title)) },
            text = { Text(stringResource(R.string.dialog_delete_alarm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteAlarm(alarmToDelete!!)
                    alarmToDelete = null
                }) {
                    Text(stringResource(R.string.btn_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { alarmToDelete = null }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }

    val alarmCreationStyle by viewModel.alarmCreationStyle.collectAsState()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.alarm_list_title)) },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.content_desc_settings))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { 
                if (alarmCreationStyle == "WIZARD") {
                    onNavigateToDetail("WIZARD") 
                } else {
                    onNavigateToDetail(null)
                }
            }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.content_desc_add_alarm))
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // -- Active Alarms --
            if (activeAlarms.isNotEmpty()) {
                item(key = "header_active") {
                    Text(
                        text = stringResource(R.string.alarm_list_active),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp).animateItem()
                    )
                }
                
                items(
                    items = activeAlarms,
                    key = { it.id }
                ) { alarm ->
                    var isVisible by remember { mutableStateOf(true) }

                    AnimatedVisibility(
                        visible = isVisible,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                        modifier = Modifier.animateItem()
                    ) {
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { dismissValue ->
                                if (dismissValue == SwipeToDismissBoxValue.StartToEnd || dismissValue == SwipeToDismissBoxValue.EndToStart) {
                                    isVisible = false
                                    coroutineScope.launch {
                                        delay(300)
                                        viewModel.deleteAlarm(alarm)
                                        val is24Hour = android.text.format.DateFormat.is24HourFormat(context)
                                        val pattern = if (is24Hour) "HH:mm" else "h:mm a"
                                        val alarmTimeStr = alarm.time.format(DateTimeFormatter.ofPattern(pattern))
                                        val titleSuffix = if (!alarm.label.isNullOrBlank()) " (${alarm.label})" else ""
                                        val result = snackbarHostState.showSnackbar(
                                            message = context.getString(R.string.alarm_list_deleted, alarmTimeStr, titleSuffix),
                                            actionLabel = context.getString(R.string.alarm_list_undo),
                                            duration = SnackbarDuration.Short
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            viewModel.addAlarm(alarm)
                                        }
                                    }
                                    true
                                } else {
                                    false
                                }
                            }
                        )
                        
                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = {
                                val color by animateColorAsState(if (dismissState.targetValue == SwipeToDismissBoxValue.Settled) Color.Transparent else MaterialTheme.colorScheme.error, label = "dismissColor")
                                val alignment = if (dismissState.targetValue == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(color, MaterialTheme.shapes.medium)
                                        .padding(horizontal = 20.dp),
                                    contentAlignment = alignment
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.content_desc_delete), tint = MaterialTheme.colorScheme.onError)
                                }
                            },
                            content = {
                                AlarmItem(
                                    alarm = alarm,
                                    onToggle = { isEnabled -> viewModel.toggleAlarm(alarm, isEnabled) },
                                    onClick = { onNavigateToDetail(alarm.id) },
                                    onLongClick = { alarmToDelete = alarm }
                                )
                            }
                        )
                    }
                }
            }

            // -- Inactive Alarms --
            if (inactiveAlarms.isNotEmpty()) {
                item(key = "header_inactive") {
                    Text(
                        text = stringResource(R.string.alarm_list_inactive),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(top = if (activeAlarms.isNotEmpty()) 24.dp else 8.dp, bottom = 4.dp)
                            .animateItem()
                    )
                }
                
                items(
                    items = inactiveAlarms,
                    key = { it.id }
                ) { alarm ->
                    var isVisible by remember { mutableStateOf(true) }

                    AnimatedVisibility(
                        visible = isVisible,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                        modifier = Modifier.animateItem()
                    ) {
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { dismissValue ->
                                if (dismissValue == SwipeToDismissBoxValue.StartToEnd || dismissValue == SwipeToDismissBoxValue.EndToStart) {
                                    isVisible = false
                                    coroutineScope.launch {
                                        delay(300)
                                        viewModel.deleteAlarm(alarm)
                                        val is24Hour = android.text.format.DateFormat.is24HourFormat(context)
                                        val pattern = if (is24Hour) "HH:mm" else "h:mm a"
                                        val alarmTimeStr = alarm.time.format(DateTimeFormatter.ofPattern(pattern))
                                        val titleSuffix = if (!alarm.label.isNullOrBlank()) " (${alarm.label})" else ""
                                        val result = snackbarHostState.showSnackbar(
                                            message = context.getString(R.string.alarm_list_deleted, alarmTimeStr, titleSuffix),
                                            actionLabel = context.getString(R.string.alarm_list_undo),
                                            duration = SnackbarDuration.Short
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            viewModel.addAlarm(alarm)
                                        }
                                    }
                                    true
                                } else {
                                    false
                                }
                            }
                        )
                        
                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = {
                                val color by animateColorAsState(if (dismissState.targetValue == SwipeToDismissBoxValue.Settled) Color.Transparent else MaterialTheme.colorScheme.error, label = "dismissColor")
                                val alignment = if (dismissState.targetValue == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(color, MaterialTheme.shapes.medium)
                                        .padding(horizontal = 20.dp),
                                    contentAlignment = alignment
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.content_desc_delete), tint = MaterialTheme.colorScheme.onError)
                                }
                            },
                            content = {
                                AlarmItem(
                                    alarm = alarm,
                                    onToggle = { isEnabled -> viewModel.toggleAlarm(alarm, isEnabled) },
                                    onClick = { onNavigateToDetail(alarm.id) },
                                    onLongClick = { alarmToDelete = alarm },
                                    isDimmed = true
                                )
                            }
                        )
                    }
                }
            }
            
            // Helpful text if completely empty
            if (activeAlarms.isEmpty() && inactiveAlarms.isEmpty()) {
                item(key = "empty_state") {
                    Column(
                        modifier = Modifier.fillParentMaxSize().animateItem(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            stringResource(R.string.alarm_list_empty),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AlarmItem(
    modifier: Modifier = Modifier,
    alarm: Alarm,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    isDimmed: Boolean = false
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = if (isDimmed) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) 
                 else CardDefaults.cardColors()
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                val context = androidx.compose.ui.platform.LocalContext.current
                val is24Hour = android.text.format.DateFormat.is24HourFormat(context)
                val pattern = if (is24Hour) "HH:mm" else "h:mm a"
                Text(
                    text = alarm.time.format(DateTimeFormatter.ofPattern(pattern)),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isDimmed) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) 
                            else MaterialTheme.colorScheme.onSurface
                )
                
                alarm.label?.let {
                    Text(
                        text = if (it.isBlank()) stringResource(R.string.alarm_default_label) else it, 
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isDimmed) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) 
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (alarm.isEnabled) {
                    var currentTime by remember { mutableStateOf(LocalDateTime.now()) }
                    LaunchedEffect(Unit) {
                        while (true) {
                            delay(60000) // Refresh every minute
                            currentTime = LocalDateTime.now()
                        }
                    }
                    val nextOccurrence = remember(alarm, currentTime) { 
                        AlarmUtils.calculateNextOccurrence(alarm, currentTime) 
                    }
                    val isOneTimePast = remember(alarm, currentTime) {
                        alarm.daysOfWeek.isEmpty() && 
                        currentTime.toLocalTime().isAfter(alarm.time)
                    }
                    Text(
                        text = if (isOneTimePast) stringResource(R.string.wizard_once) else AlarmUtils.formatTimeUntil(context.resources, nextOccurrence, currentTime),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Switch(
                checked = alarm.isEnabled,
                onCheckedChange = onToggle
            )
        }
    }
}
