package com.elroi.lemurloop.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.elroi.lemurloop.data.local.dao.AlarmDao
import com.elroi.lemurloop.data.local.entity.AlarmEntity

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE alarms ADD COLUMN crescendoDurationMinutes INTEGER NOT NULL DEFAULT 1")
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Room maps Kotlin Booleans to INTEGER (0 for false, 1 for true)
        db.execSQL("ALTER TABLE alarms ADD COLUMN isTtsEnabled INTEGER NOT NULL DEFAULT 1")
    }
}

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE alarms ADD COLUMN isEvasiveSnooze INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE alarms ADD COLUMN evasiveSnoozesBeforeMoving INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE alarms ADD COLUMN isSmoothFadeOut INTEGER NOT NULL DEFAULT 1")
    }
}

val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE alarms ADD COLUMN isVibrateOnly INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE alarms ADD COLUMN isSoundEnabled INTEGER NOT NULL DEFAULT 1")
    }
}

val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE alarms ADD COLUMN isSmartWakeupEnabled INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE alarms ADD COLUMN wakeupCheckDelayMinutes INTEGER NOT NULL DEFAULT 3")
        db.execSQL("ALTER TABLE alarms ADD COLUMN wakeupCheckTimeoutSeconds INTEGER NOT NULL DEFAULT 60")
    }
}

val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE alarms ADD COLUMN smileFallbackMethod TEXT NOT NULL DEFAULT 'MATH'")
    }
}

val MIGRATION_15_16 = object : Migration(15, 16) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE alarms ADD COLUMN mathProblemCount INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE alarms ADD COLUMN mathGraduallyIncreaseDifficulty INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_16_17 = object : Migration(16, 17) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE alarms ADD COLUMN isBriefingEnabled INTEGER NOT NULL DEFAULT 1")
    }
}

val MIGRATION_17_18 = object : Migration(17, 18) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE alarms ADD COLUMN isSnoozeEnabled INTEGER NOT NULL DEFAULT 1")
    }
}

val MIGRATION_18_19 = object : Migration(18, 19) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE alarms ADD COLUMN briefingTimeoutSeconds INTEGER NOT NULL DEFAULT 30")
    }
}

val MIGRATION_19_20 = object : Migration(19, 20) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `diagnostic_logs` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `timestamp` INTEGER NOT NULL, 
                `tag` TEXT NOT NULL, 
                `message` TEXT NOT NULL, 
                `level` TEXT NOT NULL
            )
        """.trimIndent())
    }
}

val MIGRATION_20_21 = object : Migration(20, 21) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE alarms ADD COLUMN vibrationPattern TEXT NOT NULL DEFAULT 'BASIC'")
        db.execSQL("ALTER TABLE alarms ADD COLUMN vibrationCrescendoStartGapSeconds INTEGER NOT NULL DEFAULT 30")
    }
}

@Database(entities = [
    AlarmEntity::class, 
    com.elroi.lemurloop.data.local.entity.SleepRecordEntity::class,
    com.elroi.lemurloop.data.local.entity.DiagnosticLogEntity::class
], version = 21, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao
    abstract fun sleepRecordDao(): com.elroi.lemurloop.data.local.dao.SleepRecordDao
    abstract fun diagnosticLogDao(): com.elroi.lemurloop.data.local.dao.DiagnosticLogDao
}
