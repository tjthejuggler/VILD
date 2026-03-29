package com.example.vild

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vild.shared.VibeConstants
import com.example.vild.ui.theme.VILDTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VILDTheme {
                VildApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VildApp(vm: MainViewModel = viewModel()) {
    val settings by vm.settings.collectAsState()
    val nodes by vm.nodes.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("VILD – Vibration Remote") })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Spacer(Modifier.height(8.dp))

            // ── Master toggle ────────────────────────────────────────────────
            SectionLabel("Reminders")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Enable vibration reminders", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = settings.isEnabled,
                    onCheckedChange = { vm.updateIsEnabled(it) },
                )
            }

            HorizontalDivider()

            // ── Node selector ────────────────────────────────────────────────
            SectionLabel("Active Watch")
            NodeSelector(
                nodes = nodes.map { it.id to it.displayName },
                selectedNodeId = settings.targetNodeId,
                onNodeSelected = { vm.updateTargetNode(it) },
                onRefresh = { vm.refreshNodes() },
            )

            HorizontalDivider()

            // ── Frequency ────────────────────────────────────────────────────
            SectionLabel("Reminder Frequency")
            Text(
                "Min interval: ${settings.freqMinMinutes} min",
                style = MaterialTheme.typography.bodyMedium,
            )
            Slider(
                value = settings.freqMinMinutes.toFloat(),
                onValueChange = { vm.updateFreqMin(it.toInt()) },
                valueRange = 1f..120f,
                steps = 118,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "Max interval: ${settings.freqMaxMinutes} min",
                style = MaterialTheme.typography.bodyMedium,
            )
            Slider(
                value = settings.freqMaxMinutes.toFloat(),
                onValueChange = { vm.updateFreqMax(it.toInt()) },
                valueRange = 1f..120f,
                steps = 118,
                modifier = Modifier.fillMaxWidth(),
            )

            HorizontalDivider()

            // ── Intensity ────────────────────────────────────────────────────
            SectionLabel("Vibration Intensity")
            Text(
                "Intensity: ${settings.vibrationIntensity}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Slider(
                value = settings.vibrationIntensity.toFloat(),
                onValueChange = { vm.updateIntensity(it.toInt()) },
                valueRange = 1f..255f,
                steps = 253,
                modifier = Modifier.fillMaxWidth(),
            )

            HorizontalDivider()

            // ── Snooze ───────────────────────────────────────────────────────
            SectionLabel("Snooze")
            if (settings.snoozeUntilTimestamp > System.currentTimeMillis()) {
                val formatted = SimpleDateFormat("HH:mm", Locale.getDefault())
                    .format(Date(settings.snoozeUntilTimestamp))
                Text(
                    "Snoozed until $formatted",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SnoozeButton("15 min", 15 * 60_000L, vm)
                SnoozeButton("30 min", 30 * 60_000L, vm)
                SnoozeButton("1 hr", 60 * 60_000L, vm)
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ── Helpers ──────────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun SnoozeButton(label: String, durationMs: Long, vm: MainViewModel) {
    Button(onClick = { vm.snooze(durationMs) }) {
        Text(label)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NodeSelector(
    nodes: List<Pair<String, String>>,
    selectedNodeId: String,
    onNodeSelected: (String) -> Unit,
    onRefresh: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    // Build display options: "All watches" + each discovered node
    val allOption = VibeConstants.VALUE_TARGET_NODE_ALL to "All watches"
    val options = listOf(allOption) + nodes

    val selectedLabel = options.firstOrNull { it.first == selectedNodeId }?.second
        ?: "All watches"

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.weight(1f),
        ) {
            OutlinedTextField(
                value = selectedLabel,
                onValueChange = {},
                readOnly = true,
                label = { Text("Active watch") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                options.forEach { (id, name) ->
                    DropdownMenuItem(
                        text = { Text(name) },
                        onClick = {
                            onNodeSelected(id)
                            expanded = false
                        },
                    )
                }
            }
        }
        Button(onClick = onRefresh) {
            Text("Refresh")
        }
    }
}
