package com.web.apps.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

enum class OrientationMode {
    SYSTEM, PORTRAIT, LANDSCAPE
}

@Entity(
    tableName = "containers",
    foreignKeys = [
        ForeignKey(
            entity = GroupEntity::class,
            parentColumns = ["groupId"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("groupId"), Index("url"), Index("cloudId")]
)
data class ContainerEntity(
    @PrimaryKey(autoGenerate = true)
    val containerId: Long = 0,
    val cloudId: String = UUID.randomUUID().toString(),
    val name: String,
    val url: String,
    val faviconUrl: String? = null,
    val openCount: Int = 0,
    val totalUsageMillis: Long = 0,
    val faviconLocalPath: String? = null,
    val groupId: Long? = null,
    val position: Int = 0,
    val isDesktopMode: Boolean = false,
    val orientationMode: OrientationMode = OrientationMode.SYSTEM,
    val isKeepAliveEnabled: Boolean = false,
    val isFullscreenEnabled: Boolean = false,
    val isLocked: Boolean = false,
    val isNotificationEnabled: Boolean = false,
    val lockPinHash: String? = null,
    val isHttpAllowed: Boolean = false,
    val userAgentOverride: String? = null,
    val lastAccessedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)