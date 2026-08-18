package com.example.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.BodhiBhasiApplication
import com.example.util.AppLogger
import kotlinx.coroutines.flow.first

class SyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        AppLogger.i("SyncWorker: Starting background synchronization job")
        val app = applicationContext as BodhiBhasiApplication
        val repository = app.container.repository
        val settingsRepository = app.container.settingsRepository

        return try {
            val url = settingsRepository.googleDocLinkFlow.first()
            AppLogger.d("SyncWorker: Synching from URL: $url")
            
            val result = repository.syncFromGoogleDocs(url)
            
            if (result.success) {
                AppLogger.i("SyncWorker: Synced successfully. Added ${result.wordsAdded} words.")
                Result.success()
            } else {
                AppLogger.w("SyncWorker: Sync failed with error: ${result.errorMessage}")
                if (runAttemptCount < 3) {
                    Result.retry()
                } else {
                    Result.failure()
                }
            }
        } catch (e: Exception) {
            AppLogger.e("SyncWorker: Exception during background sync", e)
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}
