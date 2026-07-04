package com.web.apps.ui.appswitcher

data class ActiveContainerInfo(
    val containerId: Long,
    val name: String,
    val url: String,
    val faviconBitmap: android.graphics.Bitmap?,
    val isKeepAliveEnabled: Boolean
)

data class AppSwitcherUiState(
    val isVisible: Boolean = false,
    val activeContainers: List<ActiveContainerInfo> = emptyList()
)

sealed class AppSwitcherEvent {
    object Show : AppSwitcherEvent()
    object Hide : AppSwitcherEvent()
    data class SwitchToContainer(val containerId: Long) : AppSwitcherEvent()
    data class DismissContainer(val containerId: Long) : AppSwitcherEvent()
    object DismissAll : AppSwitcherEvent()
}