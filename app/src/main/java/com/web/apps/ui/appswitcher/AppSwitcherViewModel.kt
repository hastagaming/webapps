package com.web.apps.ui.appswitcher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.web.apps.core.container.ContainerManager
import com.web.apps.data.repository.ContainerRepository
import com.web.apps.core.appswitcher.AppSwitcherTrigger
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppSwitcherViewModel @Inject constructor(
    private val containerManager: ContainerManager,
    private val containerRepository: ContainerRepository,
    trigger: AppSwitcherTrigger
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppSwitcherUiState())
    val uiState: StateFlow<AppSwitcherUiState> = _uiState.asStateFlow()

    init {
        trigger.triggers
            .onEach {
                if (_uiState.value.isVisible) hide() else show()
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: AppSwitcherEvent) {
        when (event) {
            is AppSwitcherEvent.Show -> show()
            is AppSwitcherEvent.Hide -> hide()
            is AppSwitcherEvent.SwitchToContainer -> hide()
            is AppSwitcherEvent.DismissContainer -> dismissContainer(event.containerId)
            is AppSwitcherEvent.DismissAll -> dismissAll()
        }
    }

    private fun show() {
        viewModelScope.launch {
            val activeIds = containerManager.getAllActiveContainerIds()
            val infoList = activeIds.mapNotNull { containerId ->
                val container = containerRepository.getContainerById(containerId) ?: return@mapNotNull null
                val session = containerManager.getSession(containerId)
                ActiveContainerInfo(
                    containerId = containerId,
                    name = container.name,
                    url = session?.currentUrl?.ifBlank { container.url } ?: container.url,
                    faviconBitmap = session?.faviconBitmap,
                    isKeepAliveEnabled = container.isKeepAliveEnabled
                )
            }
            _uiState.value = _uiState.value.copy(isVisible = true, activeContainers = infoList)
        }
    }

    private fun hide() {
        _uiState.value = _uiState.value.copy(isVisible = false)
    }

    private fun dismissContainer(containerId: Long) {
        containerManager.stopContainer(containerId)
        _uiState.value = _uiState.value.copy(
            activeContainers = _uiState.value.activeContainers.filterNot { it.containerId == containerId }
        )
    }

    private fun dismissAll() {
        viewModelScope.launch {
            val keepAliveIds = containerManager.getKeepAliveContainerIds()
            containerManager.stopAll(keepAliveIds)
            _uiState.value = _uiState.value.copy(
                activeContainers = _uiState.value.activeContainers.filter { it.containerId in keepAliveIds },
                isVisible = false
            )
        }
    }
}