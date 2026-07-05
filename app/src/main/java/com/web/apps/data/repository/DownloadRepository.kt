package com.web.apps.data.repository

import com.web.apps.data.local.dao.DownloadDao
import com.web.apps.data.local.entity.DownloadEntity
import com.web.apps.data.local.entity.DownloadStatus
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadRepository @Inject constructor(
    private val downloadDao: DownloadDao
) {

    fun observeAllDownloads(): Flow<List<DownloadEntity>> = downloadDao.observeAllDownloads()

    fun observeDownloadsByContainer(containerId: Long): Flow<List<DownloadEntity>> =
        downloadDao.observeDownloadsByContainer(containerId)

    fun observeDownloadById(downloadId: Long): Flow<DownloadEntity?> =
        downloadDao.observeDownloadById(downloadId)

    suspend fun createDownload(
        containerId: Long,
        fileName: String,
        sourceUrl: String,
        mimeType: String,
        totalBytes: Long
    ): Long {
        val download = DownloadEntity(
            containerId = containerId,
            fileName = fileName,
            sourceUrl = sourceUrl,
            mimeType = mimeType,
            totalBytes = totalBytes,
            status = DownloadStatus.PENDING
        )
        return downloadDao.insertDownload(download)
    }

    suspend fun markRunning(downloadId: Long) {
        downloadDao.updateStatus(downloadId, DownloadStatus.RUNNING)
    }

    suspend fun updateProgress(downloadId: Long, downloadedBytes: Long) {
        downloadDao.updateProgress(downloadId, downloadedBytes)
    }

    suspend fun markCompleted(downloadId: Long, localFilePath: String) {
        downloadDao.completeDownload(downloadId, localFilePath)
    }

    suspend fun markFailed(downloadId: Long) {
        downloadDao.updateStatus(downloadId, DownloadStatus.FAILED)
    }

    suspend fun markCancelled(downloadId: Long) {
        downloadDao.updateStatus(downloadId, DownloadStatus.CANCELLED)
    }

    suspend fun getDownloadById(downloadId: Long): DownloadEntity? =
        downloadDao.getDownloadById(downloadId)

    suspend fun clearCompleted() {
        downloadDao.clearDownloadsByStatus(DownloadStatus.COMPLETED)
    }

    suspend fun countRunning(): Int = downloadDao.countRunningDownloads()

    suspend fun deleteDownload(download: DownloadEntity) {
        downloadDao.deleteDownload(download)
    }
}