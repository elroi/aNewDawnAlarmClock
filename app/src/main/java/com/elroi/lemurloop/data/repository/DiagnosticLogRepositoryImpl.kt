package com.elroi.lemurloop.data.repository

import com.elroi.lemurloop.data.local.dao.DiagnosticLogDao
import com.elroi.lemurloop.data.local.entity.DiagnosticLogEntity
import com.elroi.lemurloop.domain.model.DiagnosticLog
import com.elroi.lemurloop.domain.repository.DiagnosticLogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DiagnosticLogRepositoryImpl @Inject constructor(
    private val diagnosticLogDao: DiagnosticLogDao
) : DiagnosticLogRepository {

    override fun getLatestLogs(): Flow<List<DiagnosticLog>> {
        return diagnosticLogDao.getLatestLogs().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun clearAll() {
        diagnosticLogDao.clearAll()
    }

    override suspend fun appendLog(tag: String, message: String, level: String) {
        diagnosticLogDao.insert(
            DiagnosticLogEntity(
                tag = tag,
                message = message,
                level = level
            )
        )
    }

    private fun DiagnosticLogEntity.toDomain(): DiagnosticLog {
        return DiagnosticLog(
            id = id,
            timestamp = timestamp,
            tag = tag,
            message = message,
            level = level
        )
    }
}

