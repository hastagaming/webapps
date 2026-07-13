package com.web.apps.core.container

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionEvictionScheduler @Inject constructor() {

    companion object {
        private const val WORK_NAME = "session_eviction_work"
        private const val MIN_INTERVAL_MINUTES = 15L
    }

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<SessionEvictionWorker>(
            MIN_INTERVAL_MINUTES, TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}