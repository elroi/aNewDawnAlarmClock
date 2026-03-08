package com.elroi.alarmpal.di

import android.app.Application
import androidx.room.Room
import com.elroi.alarmpal.data.local.AppDatabase
import com.elroi.alarmpal.data.local.dao.AlarmDao
import com.elroi.alarmpal.data.repository.AlarmRepositoryImpl
import com.elroi.alarmpal.domain.repository.AlarmRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(app: Application): AppDatabase {
        return Room.databaseBuilder(
            app,
            AppDatabase::class.java,
            "alarm_pal_db"
        )
            .addMigrations(
                com.elroi.alarmpal.data.local.MIGRATION_6_7,
                com.elroi.alarmpal.data.local.MIGRATION_7_8,
                com.elroi.alarmpal.data.local.MIGRATION_8_9,
                com.elroi.alarmpal.data.local.MIGRATION_9_10,
                com.elroi.alarmpal.data.local.MIGRATION_10_11,
                com.elroi.alarmpal.data.local.MIGRATION_11_12,
                com.elroi.alarmpal.data.local.MIGRATION_12_13,
                com.elroi.alarmpal.data.local.MIGRATION_13_14,
                com.elroi.alarmpal.data.local.MIGRATION_14_15,
                com.elroi.alarmpal.data.local.MIGRATION_15_16,
                com.elroi.alarmpal.data.local.MIGRATION_16_17,
                com.elroi.alarmpal.data.local.MIGRATION_17_18,
                com.elroi.alarmpal.data.local.MIGRATION_18_19,
                com.elroi.alarmpal.data.local.MIGRATION_19_20
            )
            .build()
    }

    @Provides
    @Singleton
    fun provideAlarmDao(db: AppDatabase): AlarmDao {
        return db.alarmDao()
    }

    @Provides
    @Singleton
    fun provideDiagnosticLogDao(db: AppDatabase): com.elroi.alarmpal.data.local.dao.DiagnosticLogDao {
        return db.diagnosticLogDao()
    }

    @Provides
    @Singleton
    fun provideAlarmRepository(dao: AlarmDao): AlarmRepository {
        return AlarmRepositoryImpl(dao)
    }

    @Provides
    @Singleton
    fun provideAlarmScheduler(
        app: Application,
        briefingGenerator: com.elroi.alarmpal.domain.generator.BriefingGenerator
    ): com.elroi.alarmpal.domain.scheduler.AlarmScheduler {
        return com.elroi.alarmpal.data.scheduler.AndroidAlarmScheduler(app, briefingGenerator)
    }
}
