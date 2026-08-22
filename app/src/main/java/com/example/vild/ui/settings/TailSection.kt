package com.example.vild.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.vild.TailUiState
import com.example.vild.data.TailHabit
import com.example.vild.data.TailIntegrationRepository
import com.example.vild.ui.theme.AuroraTeal
import com.example.vild.ui.theme.Mist
import com.example.vild.ui.theme.MoonLavender
import com.example.vild.ui.theme.StarGold

/**
 * Tail app integration section (dream-styled port of the WAGS pattern):
 * two habit slots — "read" (fired once) and "done" (fired on every tap) —
 * a searchable habit picker fed by Tail's Content Provider, and a backfill
 * button that pushes today's and all past days' values to Tail.
 */
@Composable
fun TailAppSection(
    state: TailUiState,
    onSelectHabit: (TailIntegrationRepository.Slot, TailHabit) -> Unit,
    onClearHabit: (TailIntegrationRepository.Slot) -> Unit,
    onRefresh: () -> Unit,
    onBackfill: () -> Unit,
    onDismissMessage: () -> Unit,
) {
    var pickerSlot by remember { mutableStateOf<TailIntegrationRepository.Slot?>(null) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "TAIL APP",
                style = MaterialTheme.typography.labelMedium,
                color = Mist,
            )
            if (state.loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = AuroraTeal,
                    strokeWidth = 2.dp,
                )
            } else {
                TextButton(onClick = onRefresh) {
                    Text("Refresh", style = MaterialTheme.typography.bodySmall, color = AuroraTeal)
                }
            }
        }
        Text(
            "Log reality checks as Tail habits. “Read” fires once a day; " +
                "“Done” fires every time you tap it.",
            style = MaterialTheme.typography.bodySmall,
            color = Mist,
        )

        when {
            state.unavailable ->
                Text(
                    "Tail app not found. Make sure it is installed, then tap Refresh.",
                    style = MaterialTheme.typography.bodySmall,
                    color = StarGold,
                )
            !state.loading && state.habits.isEmpty() ->
                Text(
                    "No habits loaded yet — tap Refresh to fetch them from Tail.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Mist,
                )
        }

        TailSlotRow(
            label = "Reality check read",
            selectedHabit = state.readHabit,
            onClick = { pickerSlot = TailIntegrationRepository.Slot.READ },
            onClear = { onClearHabit(TailIntegrationRepository.Slot.READ) },
        )
        TailSlotRow(
            label = "Reality check done",
            selectedHabit = state.doneHabit,
            onClick = { pickerSlot = TailIntegrationRepository.Slot.DONE },
            onClear = { onClearHabit(TailIntegrationRepository.Slot.DONE) },
        )

        // ── Backfill ────────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Backfill history",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MoonLavender,
                )
                Text(
                    "Replaces Tail's values for every day VILD has a log — wrong " +
                        "or not-done points are cleared. Connecting a habit " +
                        "backfills automatically.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Mist,
                )
            }
            if (state.backfilling) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = AuroraTeal,
                    strokeWidth = 2.dp,
                )
            } else {
                TextButton(onClick = onBackfill) {
                    Text("Send", style = MaterialTheme.typography.bodySmall, color = AuroraTeal)
                }
            }
        }

        state.message?.let { msg ->
            Text(
                msg,
                style = MaterialTheme.typography.bodySmall,
                color = AuroraTeal,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDismissMessage() },
            )
        }
        state.error?.let { err ->
            Text(
                err,
                style = MaterialTheme.typography.bodySmall,
                color = StarGold,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDismissMessage() },
            )
        }
    }

    // ── Searchable habit picker ─────────────────────────────────────────────
    pickerSlot?.let { slot ->
        val selected = when (slot) {
            TailIntegrationRepository.Slot.READ -> state.readHabit
            TailIntegrationRepository.Slot.DONE -> state.doneHabit
        }
        val otherSelected = when (slot) {
            TailIntegrationRepository.Slot.READ -> state.doneHabit
            TailIntegrationRepository.Slot.DONE -> state.readHabit
        }
        TailHabitPickerDialog(
            slot = slot,
            habitList = state.habits,
            selectedName = selected,
            otherSlotHabitName = otherSelected,
            onSelect = { habit ->
                onSelectHabit(slot, habit)
                pickerSlot = null
            },
            onDismiss = { pickerSlot = null },
        )
    }
}

@Composable
private fun TailSlotRow(
    label: String,
    selectedHabit: String,
    onClick: () -> Unit,
    onClear: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MoonLavender)
            Text(
                selectedHabit.ifBlank { "Not set" },
                style = MaterialTheme.typography.bodySmall,
                color = if (selectedHabit.isBlank()) Mist else AuroraTeal,
            )
        }
        if (selectedHabit.isNotBlank()) {
            IconButton(onClick = onClear, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Clear,
                    contentDescription = "Clear",
                    tint = Mist,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        OutlinedButton(onClick = onClick) {
            Text(if (selectedHabit.isNotBlank()) "Change" else "Set")
        }
    }
}

/** Searchable, alphabetically sorted habit picker dialog for one Tail slot. */
@Composable
private fun TailHabitPickerDialog(
    slot: TailIntegrationRepository.Slot,
    habitList: List<TailHabit>,
    selectedName: String,
    otherSlotHabitName: String,
    onSelect: (TailHabit) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }

    val filtered = remember(habitList, query) {
        val sorted = habitList.sortedBy { it.habitName.lowercase() }
        if (query.isBlank()) sorted
        else sorted.filter { it.habitName.contains(query.trim(), ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    "Select Habit — ${if (slot == TailIntegrationRepository.Slot.READ) "Read" else "Done"}",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    when (slot) {
                        TailIntegrationRepository.Slot.READ ->
                            "Fires once, when you confirm you read the check"
                        TailIntegrationRepository.Slot.DONE ->
                            "Fires every time you tap “I did it”"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Mist,
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search habits") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Mist)
                    },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = "Clear search",
                                    tint = Mist,
                                )
                            }
                        }
                    },
                    singleLine = true,
                )
                androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))
                when {
                    habitList.isEmpty() ->
                        Text(
                            "No habits available. Tap Refresh on the Tail section.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Mist,
                        )
                    filtered.isEmpty() ->
                        Text(
                            "No habits match \"${query.trim()}\".",
                            style = MaterialTheme.typography.bodySmall,
                            color = Mist,
                        )
                    else ->
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 360.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            items(filtered) { habit ->
                                // A habit already mapped to the OTHER slot is disabled —
                                // mapping both slots to one habit is what previously let
                                // a read point land on the "done" habit.
                                val taken = otherSlotHabitName.isNotBlank() &&
                                    habit.habitName == otherSlotHabitName
                                Surface(
                                    onClick = { if (!taken) onSelect(habit) },
                                    enabled = !taken,
                                    color = if (habit.habitName == selectedName) {
                                        AuroraTeal.copy(alpha = 0.15f)
                                    } else {
                                        androidx.compose.ui.graphics.Color.Transparent
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = habit.habitName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = if (taken) Mist else MoonLavender,
                                            )
                                            if (taken) {
                                                Text(
                                                    "Already used by the other slot",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = StarGold,
                                                )
                                            }
                                        }
                                        if (habit.habitName == selectedName) {
                                            Text("✓", color = AuroraTeal)
                                        }
                                    }
                                }
                            }
                        }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
