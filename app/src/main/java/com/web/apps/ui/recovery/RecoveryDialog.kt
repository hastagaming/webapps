package com.web.apps.ui.recovery

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.web.apps.core.recovery.RecoveryEvent
import com.web.apps.core.recovery.RecoveryReason

@Composable
fun RecoveryDialog(
    event: RecoveryEvent,
    onSoftReset: () -> Unit,
    onHardReset: () -> Unit,
    onDismiss: () -> Unit
) {
    val title = when (event.reason) {
        RecoveryReason.CRASH_LOOP -> "Container Keeps Crashing"
        RecoveryReason.LOAD_TIMEOUT -> "Page Is Taking Too Long"
        RecoveryReason.RENDER_PROCESS_GONE -> "Container Stopped Responding"
        RecoveryReason.MANUAL_TRIGGER -> "Container Recovery"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Text(
                text = "${event.message}\n\nChoose a recovery action to continue. " +
                    "Soft Reset reloads the container. Hard Reset also clears its cache and cookies."
            )
        },
        confirmButton = {
            TextButton(onClick = onHardReset) {
                Text("Hard Reset (Clear Cache and Data)")
            }
        },
        dismissButton = {
            TextButton(onClick = onSoftReset) {
                Text("Soft Reset (Reload Only)")
            }
        }
    )
}