package com.unifiedproductivity.app.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * Background sync entry point (spec: "Sync Service (WorkManager)").
 *
 * The Google Drive upload/download is intentionally stubbed for this MVP: it requires
 * OAuth client credentials that can't be checked into the repo. Wiring
 * [GoogleDriveSyncService] into [doWork] is the single remaining step — the local
 * export/merge machinery ([ConflictResolver]) is already in place.
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            // TODO: GoogleDriveSyncService.sync() once OAuth credentials are configured.
            //  1. Pull remote sync/*.json from the Drive appDataFolder
            //  2. ConflictResolver.merge(local, remote) per entity type
            //  3. Push the merged state back up
            Result.success()
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
