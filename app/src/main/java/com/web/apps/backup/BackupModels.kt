package com.web.apps.backup

import kotlinx.serialization.Serializable

@Serializable
data class BackupGroupModel(
    val groupId: Long,
    val name: String,
    val colorHex: String,
    val position: Int,
    val createdAt: Long
)

@Serializable
data class BackupContainerModel(
    val containerId: Long,
    val name: String,
    val url: String,
    val faviconUrl: String?,
    val groupId: Long?,
    val position: Int,
    val isDesktopMode: Boolean,
    val orientationMode: String,
    val isKeepAliveEnabled: Boolean,
    val isFullscreenEnabled: Boolean,
    val isLocked: Boolean,
    val isHttpAllowed: Boolean,
    val userAgentOverride: String?,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class BackupPayload(
    val backupVersion: Int = 1,
    val exportedAt: Long,
    val appVersionName: String,
    val groups: List<BackupGroupModel>,
    val containers: List<BackupContainerModel>
)