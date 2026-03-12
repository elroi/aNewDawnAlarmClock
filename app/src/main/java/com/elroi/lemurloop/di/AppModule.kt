package com.elroi.lemurloop.di

import android.app.Application
import androidx.room.Room
import com.elroi.lemurloop.data.local.AppDatabase
import com.elroi.lemurloop.data.local.dao.AlarmDao
import com.elroi.lemurloop.data.local.dao.DiagnosticLogDao
import com.elroi.lemurloop.data.repository.AlarmRepositoryImpl
import com.elroi.lemurloop.data.repository.AppDataRepositoryImpl
import com.elroi.lemurloop.data.repository.DiagnosticLogRepositoryImpl
import com.elroi.lemurloop.domain.repository.AlarmRepository
import com.elroi.lemurloop.domain.repository.AppDataRepository
import com.elroi.lemurloop.domain.repository.DiagnosticLogRepository
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
                com.elroi.lemurloop.data.local.MIGRATION_6_7,
                com.elroi.lemurloop.data.local.MIGRATION_7_8,
                com.elroi.lemurloop.data.local.MIGRATION_8_9,
                com.elroi.lemurloop.data.local.MIGRATION_9_10,
                com.elroi.lemurloop.data.local.MIGRATION_10_11,
                com.elroi.lemurloop.data.local.MIGRATION_11_12,
                com.elroi.lemurloop.data.local.MIGRATION_12_13,
                com.elroi.lemurloop.data.local.MIGRATION_13_14,
                com.elroi.lemurloop.data.local.MIGRATION_14_15,
                com.elroi.lemurloop.data.local.MIGRATION_15_16,
                com.elroi.lemurloop.data.local.MIGRATION_16_17,
                com.elroi.lemurloop.data.local.MIGRATION_17_18,
                com.elroi.lemurloop.data.local.MIGRATION_18_19,
                com.elroi.lemurloop.data.local.MIGRATION_19_20,
                com.elroi.lemurloop.data.local.MIGRATION_20_21
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
    fun provideDiagnosticLogDao(db: AppDatabase): DiagnosticLogDao {
        return db.diagnosticLogDao()
    }

    @Provides
    @Singleton
    fun provideAlarmRepository(dao: AlarmDao): AlarmRepository {
        return AlarmRepositoryImpl(dao)
    }

    @Provides
    @Singleton
    fun provideDiagnosticLogRepository(
        diagnosticLogDao: DiagnosticLogDao
    ): DiagnosticLogRepository {
        return DiagnosticLogRepositoryImpl(diagnosticLogDao)
    }

    @Provides
    @Singleton
    fun provideAppDataRepository(
        appDatabase: AppDatabase,
        settingsManager: com.elroi.lemurloop.domain.manager.SettingsManager
    ): AppDataRepository {
        return AppDataRepositoryImpl(appDatabase, settingsManager)
    }

    @Provides
    @Singleton
    fun provideAlarmScheduler(
        app: Application,
        briefingGenerator: com.elroi.lemurloop.domain.generator.BriefingGenerator
    ): com.elroi.lemurloop.domain.scheduler.AlarmScheduler {
        return com.elroi.lemurloop.data.scheduler.AndroidAlarmScheduler(app, briefingGenerator)
    }

    @Provides
    @Singleton
    fun provideDemoAlarmSeeder(
        alarmRepository: AlarmRepository,
        alarmScheduler: com.elroi.lemurloop.domain.scheduler.AlarmScheduler
    ): com.elroi.lemurloop.domain.manager.DemoAlarmSeeder {
        return com.elroi.lemurloop.domain.manager.DemoAlarmSeeder(alarmRepository, alarmScheduler)
    }

    @Provides
    @Singleton
    fun provideTtsEngine(
        manager: com.elroi.lemurloop.domain.manager.TextToSpeechManager
    ): com.elroi.lemurloop.domain.manager.TtsEngine = manager

    @Provides
    @Singleton
    fun provideGoogleCloudTtsEngine(
        app: Application,
        settingsManager: com.elroi.lemurloop.domain.manager.SettingsManager
    ): com.elroi.lemurloop.data.local.GoogleCloudTtsEngine {
        return com.elroi.lemurloop.data.local.GoogleCloudTtsEngine(app, settingsManager)
    }
}
