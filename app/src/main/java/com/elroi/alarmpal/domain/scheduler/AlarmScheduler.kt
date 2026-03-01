package com.elroi.alarmpal.domain.scheduler

import com.elroi.alarmpal.domain.model.Alarm

interface AlarmScheduler {
    fun schedule(alarm: Alarm)
    fun cancel(alarm: Alarm)
}
