package com.web.apps.core.qr

import kotlinx.serialization.Serializable

@Serializable
data class QrGroupModel(
    val n: String,
    val c: String,
    val p: Int
)

@Serializable
data class QrContainerModel(
    val n: String,
    val u: String,
    val g: Int?,
    val p: Int
)

@Serializable
data class QrExportPayload(
    val groups: List<QrGroupModel>,
    val containers: List<QrContainerModel>
)