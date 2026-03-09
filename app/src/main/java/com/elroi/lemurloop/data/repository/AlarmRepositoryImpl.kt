package com.elroi.lemurloop.data.repository

import com.elroi.lemurloop.data.local.dao.AlarmDao
import com.elroi.lemurloop.data.local.entity.AlarmEntity
import com.elroi.lemurloop.domain.model.Alarm
import com.elroi.lemurloop.domain.repository.AlarmRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AlarmRepositoryImpl(
    private val dao: AlarmDao
) : AlarmRepository {

    override fun getAllAlarms(): Flow<List<Alarm>> {
        return dao.getAllAlarms().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getAlarmById(id: String): Alarm? {
        return dao.getAlarmById(id)?.toDomain()
    }

    override suspend fun insertAlarm(alarm: Alarm) {
        dao.insertAlarm(AlarmEntity.fromDomain(alarm))
    }

    override suspend fun deleteAlarm(alarm: Alarm) {
        dao.deleteAlarm(AlarmEntity.fromDomain(alarm))
    }

    override suspend fun updateAlarmToggle(id: String, isEnabled: Boolean) {
        val entity = dao.getAlarmById(id)
        entity?.let {
            dao.insertAlarm(it.copy(isEnabled = isEnabled))
        }
    }
}
