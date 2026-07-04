package com.web.apps.ui.containerlock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PinPad(
    title: String,
    subtitle: String? = null,
    errorMessage: String? = null,
    maxLength: Int = 8,
    onPinComplete: (String) -> Unit,
    onCancel: (() -> Unit)? = null
) {
    var enteredPin by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = title, style = MaterialTheme.typography.titleLarge)
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Text(
            text = "*".repeat(enteredPin.length).ifEmpty { " " },
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(vertical = 24.dp)
        )

        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        val digitRows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("", "0", "backspace")
        )

        digitRows.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(vertical = 6.dp)
            ) {
                row.forEach { key ->
                    when {
                        key.isEmpty() -> {
                            Column(modifier = Modifier.aspectRatio(1f).padding(4.dp)) {}
                        }
                        key == "backspace" -> {
                            IconButton(
                                onClick = {
                                    if (enteredPin.isNotEmpty()) {
                                        enteredPin = enteredPin.dropLast(1)
                                    }
                                },
                                modifier = Modifier.aspectRatio(1f).padding(4.dp)
                            ) {
                                Icon(Icons.Filled.Backspace, contentDescription = "Delete last digit")
                            }
                        }
                        else -> {
                            OutlinedButton(
                                onClick = {
                                    if (enteredPin.length < maxLength) {
                                        enteredPin += key
                                    }
                                },
                                modifier = Modifier.aspectRatio(1f).padding(4.dp)
                            ) {
                                Text(key, style = MaterialTheme.typography.titleLarge)
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (onCancel != null) {
                OutlinedButton(onClick = onCancel) {
                    Text("Cancel")
                }
            }
            OutlinedButton(
                onClick = { onPinComplete(enteredPin) }
            ) {
                Text("Confirm")
            }
        }
    }
}