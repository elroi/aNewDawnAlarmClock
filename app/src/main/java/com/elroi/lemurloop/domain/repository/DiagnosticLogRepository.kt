package com.elroi.lemurloop.domain.repository

import com.elroi.lemurloop.domain.model.DiagnosticLog
import kotlinx.coroutines.flow.Flow

interface DiagnosticLogRepository {
    fun getLatestLogs(): Flow<List<DiagnosticLog>>
    suspend fun clearAll()
    suspend fun appendLog(tag: String, message: String, level: String = "INFO")
}

