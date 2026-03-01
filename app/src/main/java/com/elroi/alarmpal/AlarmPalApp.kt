package com.elroi.alarmpal

import android.app.Application
import androidx.work.Configuration
import com.elroi.alarmpal.domain.worker.BriefingWorker
import androidx.hilt.work.HiltWorkerFactory
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@HiltAndroidApp
class AlarmPalApp : Application(), Configuration.Provider {
    
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        setupCrashHandler()
    }

    private fun setupCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            try {
                // Get stack trace
                val sw = StringWriter()
                exception.printStackTrace(PrintWriter(sw))
                val stackTrace = sw.toString()

                // Generate filename with timestamp
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val filename = "crash_$timestamp.txt"
                
                // Write to cache directory (accessible via adb run-as or Device File Explorer without root)
                val crashFile = File(cacheDir, filename)
                crashFile.writeText("Crash in thread ${thread.name}\n\n$stackTrace")
            } catch (e: Exception) {
                // Ignore errors during crash handling to avoid infinite loops
            } finally {
                // Pass to default handler so Android still shows the crash dialogue / kills the process
                defaultHandler?.uncaughtException(thread, exception)
            }
        }
    }
}
