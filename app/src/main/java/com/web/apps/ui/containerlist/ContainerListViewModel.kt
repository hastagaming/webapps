package com.web.apps.ui.containerlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.web.apps.data.repository.ContainerRepository
import com.web.apps.data.repository.GroupRepository
import com.web.apps.service.ContainerServiceController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ContainerListViewModel @Inject constructor(
    private val containerRepository: ContainerRepository,
    private val groupRepository: GroupRepository,
    private val serviceController: ContainerServiceController,
    private val containerManager: com.web.apps.core.container.ContainerManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ContainerListUiState())
    val uiState: StateFlow<ContainerListUiState> = _uiState.asStateFlow()

    init {
        observeGroupsAndContainers()
    }

    private fun observeGroupsAndContainers() {
        combine(
            groupRepository.observeAllGroups(),
            containerRepository.observeAllContainers()
        ) { groups, allContainers ->
            val byGroup = groups.associate { group ->
                group.groupId to allContainers.filter { it.groupId == group.groupId }
            }
            val ungrouped = allContainers.filter { it.groupId == null }
            Triple(groups, byGroup, ungrouped)
        }.onEach { (groups, byGroup, ungrouped) ->
            _uiState.value = _uiState.value.copy(
                groups = groups,
                containersByGroup = byGroup,
                ungroupedContainers = ungrouped
            )
        }.launchIn(viewModelScope)
    }

    fun getActiveSessionCount(): Int = containerManager.getActiveSessionCount()

    fun onEvent(event: ContainerListEvent) {
        when (event) {
            is ContainerListEvent.SearchQueryChanged -> onSearchQueryChanged(event.query)
            is ContainerListEvent.OpenAddContainerDialog -> {
                _uiState.value = _uiState.value.copy(
                    showAddContainerDialog = true,
                    addContainerTargetGroupId = event.groupId
                )
            }
            is ContainerListEvent.DismissAddContainerDialog -> {
                _uiState.value = _uiState.value.copy(showAddContainerDialog = false)
            }
            is ContainerListEvent.OpenAddGroupDialog -> {
                _uiState.value = _uiState.value.copy(showAddGroupDialog = true)
            }
            is ContainerListEvent.DismissAddGroupDialog -> {
                _uiState.value = _uiState.value.copy(showAddGroupDialog = false)
            }
            is ContainerListEvent.CreateContainer -> createContainer(event.name, event.url, event.groupId)
            is ContainerListEvent.CreateGroup -> createGroup(event.name, event.colorHex, event.iconUri)
            is ContainerListEvent.DeleteContainer -> deleteContainer(event)
            is ContainerListEvent.ChangeContainerIcon -> changeContainerIcon(event.containerId, event.localPath)
            is ContainerListEvent.DeleteGroup -> deleteGroup(event)
            is ContainerListEvent.ToggleNotification -> toggleNotification(event.containerId, event.enabled)
            is ContainerListEvent.RefreshContainer -> serviceController.refreshContainer(event.containerId)
            is ContainerListEvent.StopContainer -> serviceController.stopContainer(event.containerId)
            is ContainerListEvent.RefreshAll -> serviceController.refreshAll()
            is ContainerListEvent.StopAll -> serviceController.stopAll()
            is ContainerListEvent.MoveContainerUp -> moveContainer(event.containerId, -1)
            is ContainerListEvent.MoveContainerDown -> moveContainer(event.containerId, 1)
            is ContainerListEvent.ToggleKeepAlive -> toggleKeepAlive(event.containerId, event.enabled)
            is ContainerListEvent.MoveContainerToGroup -> moveContainerToGroup(event.containerId, event.groupId)
        }
    }

    private fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query, isSearching = query.isNotBlank())
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(searchResults = emptyList())
            return
        }
        containerRepository.searchContainers(query)
            .onEach { results ->
                _uiState.value = _uiState.value.copy(searchResults = results)
            }
            .launchIn(viewModelScope)
    }

    private fun createContainer(name: String, url: String, groupId: Long?) {
        viewModelScope.launch {
            try {
                val newContainerId = containerRepository.createContainer(name = name, url = url, groupId = groupId)
                serviceController.openContainer(newContainerId)
                _uiState.value = _uiState.value.copy(showAddContainerDialog = false, errorMessage = null)
            } catch (e: IllegalArgumentException) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }

    private fun toggleNotification(containerId: Long, enabled: Boolean) {
        viewModelScope.launch {
            containerRepository.setNotificationEnabled(containerId, enabled)
        }
    }

    private fun createGroup(name: String, colorHex: String, iconUri: String?) {
        viewModelScope.launch {
            groupRepository.createGroup(name = name, colorHex = colorHex, iconUri = iconUri)
            _uiState.value = _uiState.value.copy(showAddGroupDialog = false)
        }
    }

    private fun deleteContainer(event: ContainerListEvent.DeleteContainer) {
        viewModelScope.launch {
            serviceController.stopContainer(event.container.containerId)
            containerRepository.deleteContainer(event.container)
        }
    }

    private fun deleteGroup(event: ContainerListEvent.DeleteGroup) {
        viewModelScope.launch {
            groupRepository.deleteGroup(event.group)
        }
    }

    private fun toggleKeepAlive(containerId: Long, enabled: Boolean) {
        viewModelScope.launch {
            containerRepository.setKeepAlive(containerId, enabled)
        }
    }

    private fun moveContainerToGroup(containerId: Long, groupId: Long?) {
        viewModelScope.launch {
            containerRepository.moveToGroup(containerId, groupId)
        }
    }

    private fun changeContainerIcon(containerId: Long, localPath: String) {
        viewModelScope.launch {
            containerRepository.updateFavicon(containerId, faviconUrl = null, localPath = localPath)
        }
    }

    private fun moveContainer(containerId: Long, direction: Int) {
        viewModelScope.launch {
            val all = containerRepository.getAllContainersOnce()
            val container = all.find { it.containerId == containerId } ?: return@launch
            val siblings = all.filter { it.groupId == container.groupId }.sortedBy { it.position }
            val index = siblings.indexOfFirst { it.containerId == containerId }
            val swapIndex = index + direction
            if (swapIndex !in siblings.indices) return@launch

            val other = siblings[swapIndex]
            containerRepository.updateContainer(container.copy(position = other.position))
            containerRepository.updateContainer(other.copy(position = container.position))
        }
    }
}