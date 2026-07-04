package com.web.apps.ui.backup

data class BackupUiState(
    val isProcessing: Boolean = false,
    val encryptBackup: Boolean = true,
    val mergeStrategy: com.web.apps.backup.ImportMergeStrategy = com.web.apps.backup.ImportMergeStrategy.MERGE_KEEP_BOTH,
    val resultMessage: String? = null,
    val isError: Boolean = false
)

sealed class BackupEvent {
    data class ExportRequested(val uri: android.net.Uri) : BackupEvent()
    data class ImportRequested(val uri: android.net.Uri) : BackupEvent()
    data class ToggleEncryption(val enabled: Boolean) : BackupEvent()
    data class ChangeMergeStrategy(val strategy: com.web.apps.backup.ImportMergeStrategy) : BackupEvent()
    object DismissResult : BackupEvent()
}