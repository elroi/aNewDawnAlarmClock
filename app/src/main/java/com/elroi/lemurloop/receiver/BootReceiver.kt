package com.elroi.lemurloop.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.elroi.lemurloop.domain.repository.AlarmRepository
import com.elroi.lemurloop.domain.scheduler.AlarmScheduler
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface BootReceiverEntryPoint {
        fun alarmRepository(): AlarmRepository
        fun alarmScheduler(): AlarmScheduler
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        Log.d("BootReceiver", "Device rebooted — restoring alarms")

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val entryPoint = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    BootReceiverEntryPoint::class.java
                )
                val alarms = entryPoint.alarmRepository().getAllAlarms().first()
                var count = 0
                for (alarm in alarms) {
                    if (alarm.isEnabled) {
                        entryPoint.alarmScheduler().schedule(alarm)
                        count++
                    }
                }
                Log.d("BootReceiver", "Rescheduled $count alarm(s) after reboot")
            } catch (e: Exception) {
                Log.e("BootReceiver", "Failed to restore alarms after reboot", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
