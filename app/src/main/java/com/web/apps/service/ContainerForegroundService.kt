package com.web.apps.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.web.apps.MainActivity
import com.web.apps.R
import com.web.apps.WebAppsApplication
import com.web.apps.core.container.ContainerManager
import com.web.apps.data.repository.ContainerRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ContainerForegroundService : Service() {

    @Inject
    lateinit var containerManager: ContainerManager

    @Inject
    lateinit var containerRepository: ContainerRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    companion object {
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.web.apps.action.START"
        const val ACTION_OPEN_CONTAINER = "com.web.apps.action.OPEN_CONTAINER"
        const val ACTION_REFRESH_CONTAINER = "com.web.apps.action.REFRESH_CONTAINER"
        const val ACTION_STOP_CONTAINER = "com.web.apps.action.STOP_CONTAINER"
        const val ACTION_REFRESH_ALL = "com.web.apps.action.REFRESH_ALL"
        const val ACTION_STOP_ALL = "com.web.apps.action.STOP_ALL"
        const val ACTION_STOP_SERVICE = "com.web.apps.action.STOP_SERVICE"
        const val EXTRA_CONTAINER_ID = "extra_container_id"

        fun buildStartIntent(context: android.content.Context): Intent {
            return Intent(context, ContainerForegroundService::class.java).apply {
                action = ACTION_START
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> updateNotification()

            ACTION_OPEN_CONTAINER -> {
                val containerId = intent.getLongExtra(EXTRA_CONTAINER_ID, -1L)
                if (containerId != -1L) {
                    openContainerActivity(containerId)
                }
            }

            ACTION_REFRESH_CONTAINER -> {
                val containerId = intent.getLongExtra(EXTRA_CONTAINER_ID, -1L)
                if (containerId != -1L) {
                    containerManager.refreshContainer(containerId)
                    updateNotification()
                }
            }

            ACTION_STOP_CONTAINER -> {
                val containerId = intent.getLongExtra(EXTRA_CONTAINER_ID, -1L)
                if (containerId != -1L) {
                    containerManager.stopContainer(containerId)
                    updateNotification()
                }
            }

            ACTION_REFRESH_ALL -> {
                containerManager.refreshAll()
                updateNotification()
            }

            ACTION_STOP_ALL -> {
                serviceScope.launch {
                    val keepAliveIds = containerManager.getKeepAliveContainerIds()
                    containerManager.stopAll(keepAliveIds)
                    updateNotification()
                }
            }

            ACTION_STOP_SERVICE -> {
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?) = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.launch {
            val keepAliveIds = containerManager.getKeepAliveContainerIds()
            containerManager.stopAll(keepAliveIds)
        }
    }

    private fun updateNotification() {
        val notification = buildNotification()
        val manager = NotificationManagerCompat.from(this)
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            manager.notify(NOTIFICATION_ID, notification)
        }
    }

    private fun openContainerActivity(containerId: Long) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_CONTAINER_ID, containerId)
        }
        startActivity(intent)
    }

    private fun buildNotification(): Notification {
        val activeCount = containerManager.getActiveSessionCount()

        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val refreshAllIntent = buildActionPendingIntent(ACTION_REFRESH_ALL, requestCode = 100)
        val stopAllIntent = buildActionPendingIntent(ACTION_STOP_ALL, requestCode = 101)

        return NotificationCompat.Builder(this, com.web.apps.WebAppsApplication.CHANNEL_ID_CONTAINER_SERVICE)
            .setContentTitle("WebApps Is Running")
            .setContentText("$activeCount active containers")
            .setSmallIcon(R.drawable.ic_notification_container)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(0, "Refresh All", refreshAllIntent)
            .addAction(0, "Stop All", stopAllIntent)
            .build()
    }

    private fun buildActionPendingIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, ContainerForegroundService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(
            this, requestCode, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    fun buildContainerActionIntent(containerId: Long, action: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, ContainerForegroundService::class.java).apply {
            this.action = action
            putExtra(EXTRA_CONTAINER_ID, containerId)
        }
        return PendingIntent.getService(
            this, requestCode, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}