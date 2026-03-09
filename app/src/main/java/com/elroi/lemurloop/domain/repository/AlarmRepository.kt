package com.elroi.lemurloop.domain.repository

import com.elroi.lemurloop.domain.model.Alarm
import kotlinx.coroutines.flow.Flow

interface AlarmRepository {
    fun getAllAlarms(): Flow<List<Alarm>>
    suspend fun getAlarmById(id: String): Alarm?
    suspend fun insertAlarm(alarm: Alarm)
    suspend fun deleteAlarm(alarm: Alarm)
    suspend fun updateAlarmToggle(id: String, isEnabled: Boolean)
}
