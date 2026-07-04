package com.web.apps.ui.permission

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.web.apps.core.permission.ContainerPermissionManager
import com.web.apps.core.permission.WebAppPermissionType
import com.web.apps.data.repository.ContainerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PermissionManagerViewModel @Inject constructor(
    private val permissionManager: ContainerPermissionManager,
    private val containerRepository: ContainerRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val containerId: Long = checkNotNull(savedStateHandle["containerId"])

    private val _uiState = MutableStateFlow(PermissionManagerUiState())
    val uiState: StateFlow<PermissionManagerUiState> = _uiState.asStateFlow()

    init {
        loadPermissions()
    }

    private fun loadPermissions() {
        viewModelScope.launch {
            val container = containerRepository.getContainerById(containerId)
            val entries = WebAppPermissionType.entries.map { type ->
                PermissionEntry(
                    type = type,
                    decision = permissionManager.getDecision(containerId, type),
                    hasSystemPermission = permissionManager.hasSystemPermission(type)
                )
            }
            _uiState.value = PermissionManagerUiState(
                containerName = container?.name ?: "",
                entries = entries
            )
        }
    }

    fun onEvent(event: PermissionManagerEvent) {
        when (event) {
            is PermissionManagerEvent.ChangeDecision -> {
                permissionManager.setDecision(containerId, event.type, event.decision)
                loadPermissions()
            }
        }
    }
}