package com.web.apps.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

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
    indices = [Index("groupId"), Index("url")]
)
data class ContainerEntity(
    @PrimaryKey(autoGenerate = true)
    val containerId: Long = 0,
    val name: String,
    val url: String,
    val faviconUrl: String? = null,
    val faviconLocalPath: String? = null,
    val groupId: Long? = null,
    val position: Int = 0,
    val isDesktopMode: Boolean = false,
    val orientationMode: OrientationMode = OrientationMode.SYSTEM,
    val isKeepAliveEnabled: Boolean = false,
    val isFullscreenEnabled: Boolean = false,
    val isLocked: Boolean = false,
    val lockPinHash: String? = null,
    val isHttpAllowed: Boolean = false,
    val userAgentOverride: String? = null,
    val lastAccessedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)