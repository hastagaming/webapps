package com.web.apps.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GitHubReleaseModel(
    @SerialName("tag_name") val tagName: String,
    @SerialName("name") val releaseName: String,
    @SerialName("body") val releaseNotes: String,
    @SerialName("assets") val assets: List<GitHubReleaseAssetModel>,
    @SerialName("html_url") val htmlUrl: String
)

@Serializable
data class GitHubReleaseAssetModel(
    @SerialName("name") val name: String,
    @SerialName("browser_download_url") val downloadUrl: String,
    @SerialName("size") val size: Long
)