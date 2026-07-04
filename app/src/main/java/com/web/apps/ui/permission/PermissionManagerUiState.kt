package com.web.apps.ui.permission

import com.web.apps.core.permission.PermissionDecision
import com.web.apps.core.permission.WebAppPermissionType

data class PermissionEntry(
    val type: WebAppPermissionType,
    val decision: PermissionDecision,
    val hasSystemPermission: Boolean
)

data class PermissionManagerUiState(
    val containerName: String = "",
    val entries: List<PermissionEntry> = emptyList()
)

sealed class PermissionManagerEvent {
    data class ChangeDecision(val type: WebAppPermissionType, val decision: PermissionDecision) : PermissionManagerEvent()
}