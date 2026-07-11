package com.web.apps.ui.recovery

import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Arrangement
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
                Icon(
                    imageVector = Icons.Filled.RestartAlt,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Hard Reset (Clear Cache and Data)")
            }
        },
        dismissButton = {
            TextButton(onClick = onSoftReset) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Soft Reset (Reload Only)")
            }
        }
    )
}