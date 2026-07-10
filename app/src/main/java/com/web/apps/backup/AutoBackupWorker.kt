package com.web.apps.backup

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class AutoBackupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val backupManager: BackupManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val result = backupManager.exportBackupAutoToDownloads(applicationContext, encrypt = false)
            when (result) {
                is BackupResult.Success -> Result.success()
                is BackupResult.Failure -> Result.retry()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }
}