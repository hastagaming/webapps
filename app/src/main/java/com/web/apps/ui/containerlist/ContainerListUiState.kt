package com.web.apps.ui.containerlist

import com.web.apps.data.local.entity.ContainerEntity
import com.web.apps.data.local.entity.GroupEntity

data class ContainerListUiState(
    val groups: List<GroupEntity> = emptyList(),
    val containersByGroup: Map<Long, List<ContainerEntity>> = emptyMap(),
    val ungroupedContainers: List<ContainerEntity> = emptyList(),
    val searchQuery: String = "",
    val searchResults: List<ContainerEntity> = emptyList(),
    val isSearching: Boolean = false,
    val showAddContainerDialog: Boolean = false,
    val showAddGroupDialog: Boolean = false,
    val addContainerTargetGroupId: Long? = null,
    val errorMessage: String? = null
)

sealed class ContainerListEvent {
    data class SearchQueryChanged(val query: String) : ContainerListEvent()
    data class OpenAddContainerDialog(val groupId: Long?) : ContainerListEvent()
    object DismissAddContainerDialog : ContainerListEvent()
    object OpenAddGroupDialog : ContainerListEvent()
    object DismissAddGroupDialog : ContainerListEvent()
    data class ChangeContainerIcon(val containerId: Long, val localPath: String) : ContainerListEvent()
    data class CreateContainer(val name: String, val url: String, val groupId: Long?) : ContainerListEvent()
    data class CreateGroup(val name: String, val colorHex: String, val iconUri: String? = null) : ContainerListEvent()
    data class DeleteContainer(val container: ContainerEntity) : ContainerListEvent()
    data class DeleteGroup(val group: GroupEntity) : ContainerListEvent()
    data class RefreshContainer(val containerId: Long) : ContainerListEvent()
    data class ToggleNotification(val containerId: Long, val enabled: Boolean) : ContainerListEvent()
    data class StopContainer(val containerId: Long) : ContainerListEvent()
    object RefreshAll : ContainerListEvent()
    object StopAll : ContainerListEvent()
    data class MoveContainerToGroup(val containerId: Long, val groupId: Long?) : ContainerListEvent()
    data class MoveContainerUp(val containerId: Long) : ContainerListEvent()
    data class MoveContainerDown(val containerId: Long) : ContainerListEvent()
    data class ToggleKeepAlive(val containerId: Long, val enabled: Boolean) : ContainerListEvent()
    data class MoveContainerToGroup(val containerId: Long, val groupId: Long?) : ContainerListEvent()
}