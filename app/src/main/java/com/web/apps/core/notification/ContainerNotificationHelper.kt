package com.web.apps.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.web.apps.MainActivity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContainerNotificationHelper @Inject constructor(
    private val badgeCountManager: BadgeCountManager
) {

    companion object {
        const val CHANNEL_ID = "container_alerts"
    }

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Container Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                setShowBadge(true)
            }
            manager.createNotificationChannel(channel)
        }
    }

    fun showUnreadNotification(context: Context, containerId: Long, containerName: String, title: String) {
        ensureChannel(context)
        badgeCountManager.incrementBadge(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("EXTRA_CONTAINER_ID", containerId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            context,
            containerId.toInt(),
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle(containerName)
            .setContentText(title)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setNumber(badgeCountManager.getCurrentCount(context))
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(containerId.toInt(), notification)
    }
}