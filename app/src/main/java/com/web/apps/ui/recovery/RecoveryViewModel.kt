package com.web.apps.ui.recovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.web.apps.core.container.ContainerManager
import com.web.apps.core.recovery.RecoveryEvent
import com.web.apps.core.recovery.RecoveryManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class RecoveryViewModel @Inject constructor(
    private val recoveryManager: RecoveryManager,
    private val containerManager: ContainerManager
) : ViewModel() {

    private val _pendingEvent = MutableStateFlow<RecoveryEvent?>(null)
    val pendingEvent: StateFlow<RecoveryEvent?> = _pendingEvent.asStateFlow()

    init {
        recoveryManager.recoveryEvents
            .onEach { event -> _pendingEvent.value = event }
            .launchIn(viewModelScope)
    }

    fun onSoftReset(containerId: Long) {
        recoveryManager.performSoftReset(containerId)
        _pendingEvent.value = null
    }

    fun onHardReset(context: android.content.Context, containerId: Long) {
        containerManager.performHardReset(context, containerId)
        recoveryManager.performSoftReset(containerId)
        _pendingEvent.value = null
    }

    fun triggerManualRecovery(containerId: Long) {
        recoveryManager.triggerManualRecovery(containerId)
    }

    fun dismiss() {
        _pendingEvent.value = null
    }
}