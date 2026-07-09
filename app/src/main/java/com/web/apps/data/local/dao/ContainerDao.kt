package com.web.apps.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.web.apps.data.local.entity.ContainerEntity
import com.web.apps.data.local.entity.OrientationMode
import kotlinx.coroutines.flow.Flow

@Dao
interface ContainerDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertContainer(container: ContainerEntity): Long

    @Update
    suspend fun updateContainer(container: ContainerEntity)

    @Delete
    suspend fun deleteContainer(container: ContainerEntity)

    @Query("SELECT * FROM containers ORDER BY position ASC")
    fun observeAllContainers(): Flow<List<ContainerEntity>>

    @Query("SELECT * FROM containers WHERE groupId = :groupId ORDER BY position ASC")
    fun observeContainersByGroup(groupId: Long): Flow<List<ContainerEntity>>

    @Query("SELECT * FROM containers WHERE groupId IS NULL ORDER BY position ASC")
    fun observeUngroupedContainers(): Flow<List<ContainerEntity>>

    @Query("SELECT * FROM containers WHERE containerId = :containerId LIMIT 1")
    suspend fun getContainerById(containerId: Long): ContainerEntity?

    @Query("SELECT * FROM containers WHERE containerId = :containerId LIMIT 1")
    fun observeContainerById(containerId: Long): Flow<ContainerEntity?>

    @Query("SELECT * FROM containers WHERE name LIKE '%' || :query || '%' OR url LIKE '%' || :query || '%' ORDER BY position ASC")
    fun searchContainers(query: String): Flow<List<ContainerEntity>>

    @Query("SELECT * FROM containers")
    suspend fun getAllContainersOnce(): List<ContainerEntity>

    @Query("UPDATE containers SET openCount = openCount + 1 WHERE containerId = :containerId")
    suspend fun incrementOpenCount(containerId: Long)

    @Query("UPDATE containers SET totalUsageMillis = totalUsageMillis + :millis WHERE containerId = :containerId")
    suspend fun addUsageMillis(containerId: Long, millis: Long)

    @Query("UPDATE containers SET isNotificationEnabled = :enabled WHERE containerId = :containerId")
    suspend fun updateNotificationEnabled(containerId: Long, enabled: Boolean)

    @Query("UPDATE containers SET isPinned = :pinned WHERE containerId = :containerId")
    suspend fun updatePinned(containerId: Long, pinned: Boolean)

    @Query("UPDATE containers SET lastAccessedAt = :timestamp WHERE containerId = :containerId")
    suspend fun updateLastAccessed(containerId: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE containers SET faviconUrl = :faviconUrl, faviconLocalPath = :localPath WHERE containerId = :containerId")
    suspend fun updateFavicon(containerId: Long, faviconUrl: String?, localPath: String?)

    @Query("UPDATE containers SET isLocked = :isLocked, lockPinHash = :pinHash WHERE containerId = :containerId")
    suspend fun updateLockState(containerId: Long, isLocked: Boolean, pinHash: String?)

    @Query("UPDATE containers SET isDesktopMode = :enabled WHERE containerId = :containerId")
    suspend fun updateDesktopMode(containerId: Long, enabled: Boolean)

    @Query("UPDATE containers SET orientationMode = :mode WHERE containerId = :containerId")
    suspend fun updateOrientationMode(containerId: Long, mode: OrientationMode)

    @Query("UPDATE containers SET isKeepAliveEnabled = :enabled WHERE containerId = :containerId")
    suspend fun updateKeepAlive(containerId: Long, enabled: Boolean)

    @Query("UPDATE containers SET position = :newPosition WHERE containerId = :containerId")
    suspend fun updatePosition(containerId: Long, newPosition: Int)

    @Query("UPDATE containers SET groupId = :groupId WHERE containerId = :containerId")
    suspend fun moveToGroup(containerId: Long, groupId: Long?)

    @Query("SELECT * FROM containers WHERE isKeepAliveEnabled = 1")
    suspend fun getKeepAliveContainers(): List<ContainerEntity>

    @Query("SELECT COUNT(*) FROM containers")
    suspend fun countContainers(): Int
}