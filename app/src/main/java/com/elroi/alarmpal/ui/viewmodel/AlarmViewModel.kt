package com.elroi.alarmpal.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elroi.alarmpal.domain.model.Alarm
import com.elroi.alarmpal.domain.manager.SettingsManager
import com.elroi.alarmpal.domain.manager.AlarmDefaults
import com.elroi.alarmpal.domain.repository.AlarmRepository
import com.elroi.alarmpal.domain.scheduler.AlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject

@HiltViewModel
class AlarmViewModel @Inject constructor(
    private val repository: AlarmRepository,
    private val scheduler: AlarmScheduler,
    private val settingsManager: SettingsManager
) : ViewModel() {

    val alarms: StateFlow<List<Alarm>> = repository.getAllAlarms()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val defaultAlarmSettings: StateFlow<AlarmDefaults> = settingsManager.alarmDefaultsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AlarmDefaults())

    fun toggleAlarm(alarm: Alarm, isEnabled: Boolean) {
        viewModelScope.launch {
            repository.updateAlarmToggle(alarm.id, isEnabled)
            if (isEnabled) {
                scheduler.schedule(alarm)
            } else {
                scheduler.cancel(alarm)
            }
        }
    }

    fun addAlarm(alarm: Alarm) {
        viewModelScope.launch {
            repository.insertAlarm(alarm)
            if (alarm.isEnabled) {
                scheduler.schedule(alarm)
            }
        }
    }

    fun deleteAlarm(alarm: Alarm) {
        viewModelScope.launch {
            scheduler.cancel(alarm)
            repository.deleteAlarm(alarm)
        }
    }
    
    suspend fun getAlarm(id: String): Alarm? {
        return repository.getAlarmById(id)
    }
}
