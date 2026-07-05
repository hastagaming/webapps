package com.web.apps.ui.update

data class UpdateUiState(
    val statusMessage: String = "Checking for updates...",
    val progressPercent: Int = 0,
    val isBusy: Boolean = true,
    val isUpToDate: Boolean = false,
    val isInstalling: Boolean = false,
    val errorMessage: String? = null
)