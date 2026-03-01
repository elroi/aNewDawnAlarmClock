package com.elroi.alarmpal.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.elroi.alarmpal.service.AlarmService

class WakeupCheckReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getStringExtra(AlarmService.EXTRA_ALARM_ID) ?: return
        Log.d("WakeupCheck", "Wakeup check triggered for alarm $alarmId")

        val activityIntent = Intent(context, com.elroi.alarmpal.ui.activity.WakeupCheckActivity::class.java).apply {
            putExtras(intent)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        context.startActivity(activityIntent)
    }
}
