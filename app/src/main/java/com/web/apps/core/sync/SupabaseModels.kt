package com.web.apps.core.sync

import kotlinx.serialization.Serializable

@Serializable
data class GroupRemote(
    val user_email: String,
    val cloud_id: String,
    val name: String,
    val color_hex: String,
    val icon_uri: String? = null,
    val position: Int
)

@Serializable
data class ContainerRemote(
    val user_email: String,
    val cloud_id: String,
    val group_cloud_id: String? = null,
    val name: String,
    val url: String,
    val favicon_url: String? = null,
    val position: Int,
    val is_desktop_mode: Boolean,
    val orientation_mode: String,
    val is_keep_alive_enabled: Boolean,
    val is_fullscreen_enabled: Boolean,
    val is_http_allowed: Boolean,
    val user_agent_override: String? = null
)