package com.elroi.lemurloop.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "sleep_records")
data class SleepRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bedtime: LocalDateTime?, // When the user went to bed (can be inferred or manual)
    val wakeTime: LocalDateTime, // When the alarm was dismissed
    val scheduledTime: LocalDateTime, // When the alarm was set for
    val snoozeCount: Int,
    val timeToDismissSeconds: Long // How long it took to dismiss
)
