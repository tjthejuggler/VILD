package com.example.vild.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.vild.MainViewModel
import com.example.vild.data.VibeSettings

/**
 * Snooze section: live countdown, default snooze buttons (15/30/60 min),
 * custom snooze buttons with delete, and an "Add custom" dialog.
 */
@Composable
fun SnoozeSection(settings: VibeSettings, vm: MainViewModel) {
    val countdownText by vm.snoozeCountdownText.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    // ── Countdown display ────────────────────────────────────────────────────
    if (countdownText != null) {
        Text(
            text = countdownText!!,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }

    // ── Default snooze buttons ───────────────────────────────────────────────
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(onClick = { vm.snooze(15 * 60_000L) }) { Text("15 min") }
        Button(onClick = { vm.snooze(30 * 60_000L) }) { Text("30 min") }
        Button(onClick = { vm.snooze(60 * 60_000L) }) { Text("1 hr") }
    }

    // ── Custom snooze buttons ────────────────────────────────────────────────
    if (settings.customSnoozeDurations.isNotEmpty()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            settings.customSnoozeDurations.forEach { durationMs ->
                    val label = formatSnoozeDuration(durationMs)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = { vm.snooze(durationMs) }) {
                            Text(label)
                        }
                        TextButton(onClick = { vm.removeCustomSnoozeDuration(durationMs) }) {
                            Text("×", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
        }
    }

    // ── Add custom snooze ────────────────────────────────────────────────────
    TextButton(
        onClick = { showAddDialog = true },
        modifier = Modifier.padding(top = 4.dp),
    ) {
        Text("+ Add custom snooze")
    }

    if (showAddDialog) {
        AddCustomSnoozeDialog(
            onConfirm = { minutes ->
                vm.addCustomSnoozeDuration(minutes * 60_000L)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false },
        )
    }
}

/** Formats a duration in ms to a human-readable label (e.g. "45 min", "2 hr"). */
private fun formatSnoozeDuration(durationMs: Long): String {
    val totalMinutes = durationMs / 60_000L
    return if (totalMinutes % 60 == 0L) {
        "${totalMinutes / 60} hr"
    } else {
        "$totalMinutes min"
    }
}

@Composable
private fun AddCustomSnoozeDialog(
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var input by remember { mutableStateOf("") }
    val minutes = input.toLongOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add custom snooze") },
        text = {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it.filter { c -> c.isDigit() } },
                label = { Text("Minutes") },
                singleLine = true,
            )
        },
        confirmButton = {
            Button(
                onClick = { if (minutes != null && minutes > 0) onConfirm(minutes) },
                enabled = minutes != null && minutes > 0,
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
