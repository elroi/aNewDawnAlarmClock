package com.elroi.lemurloop.domain.scheduler

import com.elroi.lemurloop.domain.model.Alarm

interface AlarmScheduler {
    fun schedule(alarm: Alarm)
    fun cancel(alarm: Alarm)
}
