package com.web.apps.ui.browser

import com.web.apps.data.local.entity.ContainerEntity

data class BrowserUiState(
    val container: ContainerEntity? = null,
    val currentUrl: String = "",
    val loadProgress: Int = 0,
    val isLoading: Boolean = false,
    val faviconBitmap: android.graphics.Bitmap? = null,
    val isLocked: Boolean = false,
    val showUnlockDialog: Boolean = false,
    val showDangerousSiteWarning: Boolean = false,
    val dangerousUrl: String? = null,
    val showHttpBlockedWarning: Boolean = false,
    val blockedUrl: String? = null,
    val isFullscreen: Boolean = false,
    val errorMessage: String? = null
)

sealed class BrowserEvent {
    data class LoadUrl(val url: String) : BrowserEvent()
    object Refresh : BrowserEvent()
    object GoBack : BrowserEvent()
    object GoForward : BrowserEvent()
    object ToggleDesktopMode : BrowserEvent()
    object ToggleFullscreen : BrowserEvent()
    data class UnlockAttempt(val pin: String) : BrowserEvent()
    object DismissDangerousWarning : BrowserEvent()
    object AllowHttpOnce : BrowserEvent()
    object DismissHttpWarning : BrowserEvent()
}