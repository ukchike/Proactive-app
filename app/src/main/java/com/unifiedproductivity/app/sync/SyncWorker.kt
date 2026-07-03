package com.unifiedproductivity.app.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.unifiedproductivity.app.UnifiedProductivityApp
import java.util.concurrent.TimeUnit

/**
 * Background sync entry point (spec: "Sync Service (WorkManager)"). Runs a two-way
 * Drive sync when the user is signed in; otherwise it's a no-op so the app keeps
 * working fully offline.
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? UnifiedProductivityApp ?: return Result.success()
        val drive = app.container.driveSyncManager
        if (!drive.isSignedIn) return Result.success()

        return try {
            val ok = drive.syncNow()
            if (ok) Result.success()
            else if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
        } catch (t: Throwable) {
            // Exponential backoff is handled by WorkManager's retry policy.
            if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
        }
    }

    companion object {
        private const val MAX_RETRIES = 3
        private const val WORK_NAME = "periodic_drive_sync"

        /** Schedule the periodic Wi-Fi-friendly sync described in the spec. */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
