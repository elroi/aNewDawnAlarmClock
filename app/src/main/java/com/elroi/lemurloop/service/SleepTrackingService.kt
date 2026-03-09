package com.elroi.lemurloop.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.elroi.lemurloop.MainActivity
import com.elroi.lemurloop.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlin.math.sqrt

@AndroidEntryPoint
class SleepTrackingService : Service(), SensorEventListener {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private var movementCount = 0
    private var startTime: Long = 0

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "LemurLoop:SleepTracking")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(2, createNotification())
        startTracking()
        
        return START_STICKY
    }

    private fun startTracking() {
        startTime = System.currentTimeMillis()
        wakeLock?.acquire(10 * 60 * 60 * 1000L /*10 hours*/)
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    private fun stopTracking() {
        sensorManager.unregisterListener(this)
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        // TODO: Save sleep session to Database
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            val x = it.values[0]
            val y = it.values[1]
            val z = it.values[2]
            
            val acceleration = sqrt((x * x + y * y + z * z).toDouble())
            if (acceleration > 12) { // Threshold for movement
                movementCount++
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    private fun createNotification(): Notification {
        val channelId = "sleep_tracking_channel"
        val channelName = "Sleep Tracking"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }

        val stopIntent = Intent(this, SleepTrackingService::class.java).apply {
            action = "STOP"
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Sleep Tracking Active")
            .setContentText("Monitoring your sleep...")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(R.mipmap.ic_launcher, "Stop", stopPendingIntent)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTracking()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
