package com.elroi.alarmpal.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.elroi.alarmpal.data.local.entity.DiagnosticLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DiagnosticLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: DiagnosticLogEntity)

    @Query("SELECT * FROM diagnostic_logs ORDER BY timestamp DESC LIMIT 500")
    fun getLatestLogs(): Flow<List<DiagnosticLogEntity>>

    @Query("DELETE FROM diagnostic_logs")
    suspend fun clearAll()

    @Query("DELETE FROM diagnostic_logs WHERE timestamp < :timestampLimit")
    suspend fun deleteOldLogs(timestampLimit: Long)
}
