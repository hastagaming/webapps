package com.web.apps

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.web.apps.service.ContainerServiceController
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class WebAppsApplication : Application() {

    @Inject
    lateinit var serviceController: ContainerServiceController

    companion object {
        const val CHANNEL_ID_CONTAINER_SERVICE = "container_service_channel"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        serviceController.startService()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID_CONTAINER_SERVICE,
                "Container Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications for running web app containers"
            }
            manager.createNotificationChannel(channel)
        }
    }
}