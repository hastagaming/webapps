package com.web.apps.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.web.apps.data.local.entity.DownloadEntity
import com.web.apps.data.local.entity.DownloadStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertDownload(download: DownloadEntity): Long

    @Update
    suspend fun updateDownload(download: DownloadEntity)

    @Delete
    suspend fun deleteDownload(download: DownloadEntity)

    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun observeAllDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE containerId = :containerId ORDER BY createdAt DESC")
    fun observeDownloadsByContainer(containerId: Long): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE downloadId = :downloadId LIMIT 1")
    suspend fun getDownloadById(downloadId: Long): DownloadEntity?

    @Query("SELECT * FROM downloads WHERE downloadId = :downloadId LIMIT 1")
    fun observeDownloadById(downloadId: Long): Flow<DownloadEntity?>

    @Query("UPDATE downloads SET status = :status, updatedAt = :timestamp WHERE downloadId = :downloadId")
    suspend fun updateStatus(downloadId: Long, status: DownloadStatus, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE downloads SET downloadedBytes = :downloadedBytes, updatedAt = :timestamp WHERE downloadId = :downloadId")
    suspend fun updateProgress(downloadId: Long, downloadedBytes: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE containers SET openCount = openCount + 1 WHERE containerId = :containerId")
    suspend fun incrementOpenCount(containerId: Long)

    @Query("UPDATE containers SET totalUsageMillis = totalUsageMillis + :millis WHERE containerId = :containerId")
    suspend fun addUsageMillis(containerId: Long, millis: Long)

    @Query("UPDATE containers SET isNotificationEnabled = :enabled WHERE containerId = :containerId")
    suspend fun updateNotificationEnabled(containerId: Long, enabled: Boolean)

    @Query("UPDATE downloads SET localFilePath = :path, status = :status, updatedAt = :timestamp WHERE downloadId = :downloadId")
    suspend fun completeDownload(
        downloadId: Long,
        path: String,
        status: DownloadStatus = DownloadStatus.COMPLETED,
        timestamp: Long = System.currentTimeMillis()
    )

    @Query("SELECT * FROM downloads WHERE status = :status")
    suspend fun getDownloadsByStatus(status: DownloadStatus): List<DownloadEntity>

    @Query("DELETE FROM downloads WHERE status = :status")
    suspend fun clearDownloadsByStatus(status: DownloadStatus)

    @Query("SELECT COUNT(*) FROM downloads WHERE status = 'RUNNING'")
    suspend fun countRunningDownloads(): Int
}