package com.web.apps.data.repository

import com.web.apps.data.local.dao.GroupDao
import com.web.apps.data.local.entity.GroupEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroupRepository @Inject constructor(
    private val groupDao: GroupDao
) {

    fun observeAllGroups(): Flow<List<GroupEntity>> = groupDao.observeAllGroups()

    suspend fun getGroupById(groupId: Long): GroupEntity? = groupDao.getGroupById(groupId)

    suspend fun createGroup(name: String, colorHex: String): Long {
        val position = groupDao.countGroups()
        val group = GroupEntity(
            name = name,
            colorHex = colorHex,
            position = position
        )
        return groupDao.insertGroup(group)
    }

    suspend fun renameGroup(groupId: Long, newName: String) {
        val existing = groupDao.getGroupById(groupId) ?: return
        groupDao.updateGroup(existing.copy(name = newName))
    }

    suspend fun updateGroupColor(groupId: Long, colorHex: String) {
        val existing = groupDao.getGroupById(groupId) ?: return
        groupDao.updateGroup(existing.copy(colorHex = colorHex))
    }

    suspend fun deleteGroup(group: GroupEntity) {
        groupDao.deleteGroup(group)
    }

    suspend fun reorderGroup(groupId: Long, newPosition: Int) {
        groupDao.updatePosition(groupId, newPosition)
    }
}