package com.dinofit.app.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.dinofit.app.core.data.StepsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.TimeUnit

/**
 * Relève le podomètre toutes les 15 minutes, même si l'app est fermée,
 * pour que chaque pas soit compté le bon jour.
 */
@HiltWorker
class StepSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val steps: StepsRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!steps.sensorAvailable) return Result.success()
        val raw = withTimeoutOrNull(10_000L) { steps.rawCounter().first() }
        if (raw != null) steps.recordCounter(raw)
        return Result.success()
    }

    companion object {
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<StepSyncWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork("step-sync", ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
