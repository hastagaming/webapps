package com.web.apps

import android.app.Application
import android.app.NotificationChannel
import android.os.Build
import com.web.apps.core.crash.CrashHandler
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class WebAppsApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Thread.setDefaultUncaughtExceptionHandler(CrashHandler(this))
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val containerChannel = NotificationChannel(
                CHANNEL_ID_CONTAINER_SERVICE,
                "Container Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications for running web app containers"
            }
            getSystemService(NotificationManager::class.java)
        }
    }
}