package com.web.apps.core.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

enum class WebAppPermissionType {
    CAMERA, MICROPHONE, LOCATION, NOTIFICATIONS, STORAGE
}

enum class PermissionDecision {
    ALLOWED, DENIED, ASK_EVERY_TIME
}

@Singleton
class ContainerPermissionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val containerPermissionMap = mutableMapOf<Long, MutableMap<WebAppPermissionType, PermissionDecision>>()

    fun setDecision(containerId: Long, type: WebAppPermissionType, decision: PermissionDecision) {
        val map = containerPermissionMap.getOrPut(containerId) { mutableMapOf() }
        map[type] = decision
    }

    fun getDecision(containerId: Long, type: WebAppPermissionType): PermissionDecision {
        return containerPermissionMap[containerId]?.get(type) ?: PermissionDecision.ASK_EVERY_TIME
    }

    fun hasSystemPermission(type: WebAppPermissionType): Boolean {
        val androidPermission = when (type) {
            WebAppPermissionType.CAMERA -> Manifest.permission.CAMERA
            WebAppPermissionType.MICROPHONE -> Manifest.permission.RECORD_AUDIO
            WebAppPermissionType.LOCATION -> Manifest.permission.ACCESS_FINE_LOCATION
            WebAppPermissionType.NOTIFICATIONS -> Manifest.permission.POST_NOTIFICATIONS
            WebAppPermissionType.STORAGE -> Manifest.permission.WRITE_EXTERNAL_STORAGE
        }
        return ContextCompat.checkSelfPermission(context, androidPermission) == PackageManager.PERMISSION_GRANTED
    }

    fun clearContainerPermissions(containerId: Long) {
        containerPermissionMap.remove(containerId)
    }

    fun getAllDecisionsForContainer(containerId: Long): Map<WebAppPermissionType, PermissionDecision> {
        return containerPermissionMap[containerId]?.toMap() ?: emptyMap()
    }
}