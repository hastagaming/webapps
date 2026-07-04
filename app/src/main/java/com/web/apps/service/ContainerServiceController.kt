package com.web.apps.service

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContainerServiceController @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun startService() {
        val intent = ContainerForegroundService.buildStartIntent(context)
        ContextCompat.startForegroundService(context, intent)
    }

    fun openContainer(containerId: Long) {
        sendAction(ContainerForegroundService.ACTION_OPEN_CONTAINER, containerId)
    }

    fun refreshContainer(containerId: Long) {
        sendAction(ContainerForegroundService.ACTION_REFRESH_CONTAINER, containerId)
    }

    fun stopContainer(containerId: Long) {
        sendAction(ContainerForegroundService.ACTION_STOP_CONTAINER, containerId)
    }

    fun refreshAll() {
        sendAction(ContainerForegroundService.ACTION_REFRESH_ALL, containerId = null)
    }

    fun stopAll() {
        sendAction(ContainerForegroundService.ACTION_STOP_ALL, containerId = null)
    }

    fun stopService() {
        sendAction(ContainerForegroundService.ACTION_STOP_SERVICE, containerId = null)
    }

    private fun sendAction(action: String, containerId: Long?) {
        val intent = Intent(context, ContainerForegroundService::class.java).apply {
            this.action = action
            if (containerId != null) {
                putExtra(ContainerForegroundService.EXTRA_CONTAINER_ID, containerId)
            }
        }
        ContextCompat.startForegroundService(context, intent)
    }
}