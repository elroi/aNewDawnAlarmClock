package com.elroi.alarmpal.domain.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.elroi.alarmpal.domain.generator.BriefingGenerator
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class BriefingWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val briefingGenerator: BriefingGenerator
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        android.util.Log.d("BriefingWorker", "Briefing pre-generation started...")
        val result = briefingGenerator.refreshBriefing()
        return if (result != null) {
            Result.success()
        } else {
            // Retry once if it failed (e.g. transient network issue)
            if (runAttemptCount < 2) Result.retry() else Result.failure()
        }
    }
}
