package com.web.apps.core.plugin

data class PluginCatalogEntry(
    val id: String,
    val name: String,
    val description: String,
    val author: String,
    val version: String,
    val type: String,
    val downloadUrl: String,
    val previewColorHex: String? = null
)

data class PluginColors(
    val primary: String,
    val background: String,
    val surface: String,
    val surfaceVariant: String,
    val onSurface: String,
    val onPrimary: String,
    val error: String
)

data class PluginUiTweaks(
    val cornerRadiusDp: Int = 12,
    val gridMinTileWidthDp: Int = 100,
    val fabPosition: String = "end",
    val showSearchBar: Boolean = true,
    val showBackupButton: Boolean = true,
    val showSignOutButton: Boolean = true,
    val showSettingsButton: Boolean = true,
    val showRefreshAllButton: Boolean = true,
    val showStopAllButton: Boolean = true,
    val showPinnedSection: Boolean = true,
    val itemSpacingDp: Int = 4
)

data class PluginManifest(
    val id: String,
    val name: String,
    val version: String,
    val type: String,
    val colors: PluginColors? = null,
    val uiTweaks: PluginUiTweaks? = null
)