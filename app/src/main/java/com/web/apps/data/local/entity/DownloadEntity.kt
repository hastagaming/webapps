package com.web.apps.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class DownloadStatus {
    PENDING, RUNNING, COMPLETED, FAILED, CANCELLED
}

@Entity(
    tableName = "downloads",
    foreignKeys = [
        ForeignKey(
            entity = ContainerEntity::class,
            parentColumns = ["containerId"],
            childColumns = ["containerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("containerId")]
)
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true)
    val downloadId: Long = 0,
    val containerId: Long,
    val fileName: String,
    val sourceUrl: String,
    val mimeType: String,
    val localFilePath: String? = null,
    val totalBytes: Long = 0,
    val downloadedBytes: Long = 0,
    val status: DownloadStatus = DownloadStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)