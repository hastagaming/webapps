package com.web.apps.data.repository

import com.web.apps.core.sync.SupabaseSyncManager
import com.web.apps.data.local.dao.GroupDao
import com.web.apps.data.local.entity.GroupEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroupRepository @Inject constructor(
    private val groupDao: GroupDao,
    private val supabaseSyncManager: SupabaseSyncManager
) {
    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun observeAllGroups(): Flow<List<GroupEntity>> = groupDao.observeAllGroups()

    suspend fun getGroupById(groupId: Long): GroupEntity? = groupDao.getGroupById(groupId)

    suspend fun createGroup(name: String, colorHex: String, iconUri: String? = null): Long {
        val position = groupDao.countGroups()
        val group = GroupEntity(
            name = name,
            colorHex = colorHex,
            iconUri = iconUri,
            position = position
        )
        val newId = groupDao.insertGroup(group)
        syncScope.launch { supabaseSyncManager.pushGroup(group.copy(groupId = newId)) }
        return newId
    }

    suspend fun renameGroup(groupId: Long, newName: String) {
        val existing = groupDao.getGroupById(groupId) ?: return
        val updated = existing.copy(name = newName)
        groupDao.updateGroup(updated)
        syncScope.launch { supabaseSyncManager.pushGroup(updated) }
    }

    suspend fun updateGroupColor(groupId: Long, colorHex: String) {
        val existing = groupDao.getGroupById(groupId) ?: return
        val updated = existing.copy(colorHex = colorHex)
        groupDao.updateGroup(updated)
        syncScope.launch { supabaseSyncManager.pushGroup(updated) }
    }

    suspend fun deleteGroup(group: GroupEntity) {
        groupDao.deleteGroup(group)
        syncScope.launch { supabaseSyncManager.deleteGroupRemote(group.supabaseId) }
    }

    suspend fun reorderGroup(groupId: Long, newPosition: Int) {
        groupDao.updatePosition(groupId, newPosition)
        val existing = groupDao.getGroupById(groupId)
        if (existing != null) {
            syncScope.launch { supabaseSyncManager.pushGroup(existing) }
        }
    }
}