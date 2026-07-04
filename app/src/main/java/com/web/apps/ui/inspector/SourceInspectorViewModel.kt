package com.web.apps.ui.inspector

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.web.apps.core.container.ContainerManager
import com.web.apps.core.inspector.InspectedResource
import com.web.apps.core.inspector.SourceInspectorManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class SourceInspectorViewModel @Inject constructor(
    private val containerManager: ContainerManager,
    private val sourceInspectorManager: SourceInspectorManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val containerId: Long = checkNotNull(savedStateHandle["containerId"])

    private val _pageSource = MutableStateFlow("")
    val pageSource: StateFlow<String> = _pageSource.asStateFlow()

    private val _resourceLog = MutableStateFlow<List<InspectedResource>>(emptyList())
    val resourceLog: StateFlow<List<InspectedResource>> = _resourceLog.asStateFlow()

    fun loadPageSource() {
        val session = containerManager.getSession(containerId) ?: run {
            _pageSource.value = "No active session found for this container."
            return
        }
        sourceInspectorManager.fetchPageSource(session.webView) { html ->
            _pageSource.value = html
        }
        _resourceLog.value = sourceInspectorManager.getResourceLog(containerId)
    }

    fun clearResourceLog() {
        sourceInspectorManager.clearLog(containerId)
        _resourceLog.value = emptyList()
    }
}