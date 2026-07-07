package com.web.apps

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.web.apps.core.crash.CrashHandler
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class WebAppsApplication : Application() {

    companion object {
        const val CHANNEL_ID_CONTAINER_SERVICE = "container_notifications"
    }

    override fun onCreate() {
        super.onCreate()

        Thread.setDefaultUncaughtExceptionHandler(CrashHandler(this))

        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val containerChannel = NotificationChannel(
                CHANNEL_ID_CONTAINER_SERVICE,
                "Container Notifications",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications for active containers"
            }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(containerChannel)
        }
    }
}