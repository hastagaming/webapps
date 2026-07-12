package com.web.apps.core.plugin

import kotlinx.serialization.Serializable

@Serializable
data class PluginCatalogEntryRaw(
    val name: String,
    val description: String,
    val author: String,
    val version: String,
    val type: String,
    val previewColorHex: String? = null
)

@Serializable
data class PluginCatalog(
    val plugins: Map<String, PluginCatalogEntryRaw> = emptyMap()
)

data class PluginCatalogEntry(
    val id: String,
    val name: String,
    val description: String,
    val author: String,
    val version: String,
    val downloadUrl: String,
    val type: String,
    val previewColorHex: String? = null
)

@Serializable
data class PluginColors(
    val primary: String,
    val background: String,
    val surface: String,
    val surfaceVariant: String,
    val onSurface: String,
    val onPrimary: String,
    val error: String
)

@Serializable
data class PluginUiTweaks(
    val cornerRadiusDp: Int = 12,
    val gridMinTileWidthDp: Int = 100
)

@Serializable
data class PluginManifest(
    val id: String,
    val name: String,
    val version: String,
    val type: String,
    val colors: PluginColors,
    val uiTweaks: PluginUiTweaks = PluginUiTweaks()
)