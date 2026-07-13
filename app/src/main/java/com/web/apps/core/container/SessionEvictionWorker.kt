package com.web.apps.core.container

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.web.apps.core.preferences.SessionEvictionPreferenceManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SessionEvictionWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val containerManager: ContainerManager,
    private val preferenceManager: SessionEvictionPreferenceManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            if (!preferenceManager.isEnabledBlocking()) {
                return Result.success()
            }

            val idleMinutes = preferenceManager.getIdleMinutesBlocking()
            val idleMillis = idleMinutes * 60_000L
            val keepAliveIds = containerManager.getKeepAliveContainerIds()

            containerManager.evictInactiveSessions(idleMillis, keepAliveIds)

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}