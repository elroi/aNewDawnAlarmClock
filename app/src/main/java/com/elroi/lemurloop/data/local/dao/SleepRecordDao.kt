package com.elroi.lemurloop.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.elroi.lemurloop.data.local.entity.SleepRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SleepRecordDao {
    @Insert
    suspend fun insert(record: SleepRecordEntity)

    @Query("SELECT * FROM sleep_records ORDER BY wakeTime DESC")
    fun getAllRecords(): Flow<List<SleepRecordEntity>>

    @Query("SELECT AVG(snoozeCount) FROM sleep_records")
    fun getAverageSnoozeCount(): Flow<Float?>

    @Query("DELETE FROM sleep_records")
    suspend fun deleteAllRecords()
}
