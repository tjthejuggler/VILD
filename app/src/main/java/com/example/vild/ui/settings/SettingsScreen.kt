package com.example.vild.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import com.example.vild.data.AdviceItem
import com.example.vild.data.RealityCheckTrigger
import com.example.vild.shared.VibeConstants
import com.example.vild.ui.PresetSection
import com.example.vild.ui.SnoozeSection
import com.example.vild.ui.VibrationSection
import com.example.vild.ui.advice.AdviceDialog
import com.example.vild.ui.advice.AdviceSection
import com.example.vild.ui.dream.DreamBackground
import com.example.vild.ui.dream.GlassCard
import com.example.vild.ui.dream.rememberTiltState
import com.example.vild.ui.realitycheck.RealityCheckDialog
import com.example.vild.ui.technique.TechniqueDialog
import com.example.vild.ui.theme.AuroraTeal
import com.example.vild.ui.theme.Mist
import com.example.vild.ui.theme.MoonLavender
import com.example.vild.ui.theme.Void

/**
 * Secondary settings screen — everything that supports the practice but is
 * not the practice itself: watch vibration tuning, presets, snooze, advice
 * and reality check trigger management. Floats in glass over the dream sky.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    vm: MainViewModel,
    onBack: () -> Unit,
) {
    val settings by vm.settings.collectAsState()
    val nodes by vm.nodes.collectAsState()
    val adviceState by vm.adviceState.collectAsState()
    val triggers by vm.triggers.collectAsState()
    val techniqueState by vm.techniqueState.collectAsState()
    val autoSwitchDayOnHabit by vm.autoSwitchDayOnHabit.collectAsState()
    val tailState by vm.tailState.collectAsState()

    var openAdviceSection by remember { mutableStateOf<String?>(null) }
    var openTriggers by remember { mutableStateOf(false) }
    var openTechniques by remember { mutableStateOf(false) }

    val tilt = rememberTiltState()

    Box(modifier = Modifier.fillMaxSize()) {
        DreamBackground(tilt = tilt)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { Spacer(Modifier.height(52.dp)) }

            // ── Header ─────────────────────────────────────────────────────────
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MoonLavender,
                        )
                    }
                    Text(
                        "Settings",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MoonLavender,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
            }

            // ── Vibration & Watch (moved from the old main screen) ─────────────
            item {
                GlassCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            "VIBRATION & WATCH",
                            style = MaterialTheme.typography.labelMedium,
                            color = Mist,
                        )
                        Text(
                            "The watch buzzes as a secondary reminder. The reality check " +
                                "itself lives on the main screen.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Mist,
                        )

                        HorizontalDivider(color = MoonLavender.copy(alpha = 0.15f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "Enable vibration reminders",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MoonLavender,
                            )
                            Switch(
                                checked = settings.isEnabled,
                                onCheckedChange = { vm.updateIsEnabled(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MoonLavender,
                                    checkedTrackColor = AuroraTeal.copy(alpha = 0.5f),
                                ),
                            )
                        }

                        NodeSelector(
                            nodes = nodes.map { it.id to it.displayName },
                            selectedNodeId = settings.targetNodeId,
                            onNodeSelected = { vm.updateTargetNode(it) },
                            onRefresh = { vm.refreshNodes() },
                        )

                        Text(
                            "Reminder frequency",
                            style = MaterialTheme.typography.titleSmall,
                            color = MoonLavender,
                        )
                        Text(
                            "Min interval: ${settings.freqMinMinutes} min",
                            style = MaterialTheme.typography.bodySmall,
                            color = Mist,
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
                            style = MaterialTheme.typography.bodySmall,
                            color = Mist,
                        )
                        Slider(
                            value = settings.freqMaxMinutes.toFloat(),
                            onValueChange = { vm.updateFreqMax(it.toInt()) },
                            valueRange = 1f..120f,
                            steps = 118,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        HorizontalDivider(color = MoonLavender.copy(alpha = 0.15f))

                        VibrationSection(settings = settings, vm = vm)
                    }
                }
            }

            // ── Presets ─────────────────────────────────────────────────────────
            item {
                GlassCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("PRESETS", style = MaterialTheme.typography.labelMedium, color = Mist)
                        PresetSection(vm = vm)
                    }
                }
            }

            // ── Snooze ─────────────────────────────────────────────────────────
            item {
                GlassCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("SNOOZE", style = MaterialTheme.typography.labelMedium, color = Mist)
                        SnoozeSection(settings = settings, vm = vm)
                    }
                }
            }

            // ── Tail habits (read/done → Tail app) ─────────────────────────────
            item {
                GlassCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        TailAppSection(
                            state = tailState,
                            onSelectHabit = { slot, habit -> vm.selectTailHabit(slot, habit) },
                            onClearHabit = { slot -> vm.clearTailHabit(slot) },
                            onRefresh = { vm.refreshTailHabits() },
                            onBackfill = { vm.backfillTail() },
                            onDismissMessage = { vm.dismissTailMessage() },
                        )
                    }
                }
            }

            // ── Tail integration ───────────────────────────────────────────────
            if (vm.isTailInstalled) {
                item {
                    GlassCard {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text("TAIL INTEGRATION", style = MaterialTheme.typography.labelMedium, color = Mist)
                            Text(
                                "Automatically switch from Night to Day mode when you record " +
                                    "a habit in the Tail app (useful as a wake-up signal).",
                                style = MaterialTheme.typography.bodySmall,
                                color = Mist,
                            )
                            HorizontalDivider(color = MoonLavender.copy(alpha = 0.15f))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    "Auto switch to Day on habit",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MoonLavender,
                                )
                                Switch(
                                    checked = autoSwitchDayOnHabit,
                                    onCheckedChange = { vm.setAutoSwitchDayOnHabit(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MoonLavender,
                                        checkedTrackColor = AuroraTeal.copy(alpha = 0.5f),
                                    ),
                                )
                            }
                        }
                    }
                }
            }

            // ── Advice ─────────────────────────────────────────────────────────
            item {
                GlassCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("ADVICE", style = MaterialTheme.typography.labelMedium, color = Mist)
                        Text(
                            "Personal reminders that appear on the main screen. " +
                                "Day advice shows in Day mode, Night advice in Night mode.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Mist,
                        )
                        HorizontalDivider(color = MoonLavender.copy(alpha = 0.15f))
                        AdviceSection.all.forEach { section ->
                            val count = adviceState.adviceBySection[section]?.size ?: 0
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        AdviceSection.label(section),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MoonLavender,
                                    )
                                    Text(
                                        if (count == 0) "No advice set"
                                        else "$count piece${if (count != 1) "s" else ""} of advice",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (count > 0) AuroraTeal else Mist,
                                    )
                                }
                                OutlinedButton(
                                    onClick = { openAdviceSection = section },
                                    modifier = Modifier.height(32.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MoonLavender),
                                ) {
                                    Text(
                                        if (count > 0) "Manage" else "Add",
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Reality Check Techniques ───────────────────────────────────────
            item {
                GlassCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("REALITY CHECK TECHNIQUES", style = MaterialTheme.typography.labelMedium, color = Mist)
                        Text(
                            "Ideas for how to test whether you're dreaming — shown in the " +
                                "banner on the main screen. ✦ marks the classic methods.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Mist,
                        )
                        HorizontalDivider(color = MoonLavender.copy(alpha = 0.15f))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Technique library",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MoonLavender,
                                )
                                Text(
                                    "${techniqueState.techniques.size} techniques " +
                                        "(${techniqueState.techniques.count { it.isSeeded }} classic)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AuroraTeal,
                                )
                            }
                            OutlinedButton(
                                onClick = { openTechniques = true },
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MoonLavender),
                            ) {
                                Text("Manage", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            // ── Reality Check Triggers ─────────────────────────────────────────
            item {
                GlassCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("REALITY CHECK TRIGGERS", style = MaterialTheme.typography.labelMedium, color = Mist)
                        Text(
                            "One trigger is chosen each morning at 8 AM to be your reality " +
                                "check for the day — shown on the main screen until you confirm it.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Mist,
                        )
                        HorizontalDivider(color = MoonLavender.copy(alpha = 0.15f))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Daily trigger",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MoonLavender,
                                )
                                Text(
                                    if (triggers.isEmpty()) "No triggers set"
                                    else "${triggers.size} trigger${if (triggers.size != 1) "s" else ""} configured",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (triggers.isNotEmpty()) AuroraTeal else Mist,
                                )
                            }
                            OutlinedButton(
                                onClick = { openTriggers = true },
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MoonLavender),
                            ) {
                                Text(
                                    if (triggers.isNotEmpty()) "Manage" else "Add",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    // ── Advice dialog ─────────────────────────────────────────────────────────
    openAdviceSection?.let { section ->
        AdviceDialog(
            section = section,
            adviceList = adviceState.adviceBySection[section] ?: emptyList(),
            onAdd = { text -> vm.addAdvice(section, text) },
            onUpdate = { item: AdviceItem, text -> vm.updateAdvice(item, text) },
            onDelete = { id -> vm.deleteAdvice(id) },
            onDismiss = { openAdviceSection = null },
        )
    }

    // ── Reality check techniques dialog ────────────────────────────────────────
    if (openTechniques) {
        TechniqueDialog(
            techniques = techniqueState.techniques,
            onAdd = { text -> vm.addTechnique(text) },
            onUpdate = { item, text -> vm.updateTechnique(item, text) },
            onDelete = { id -> vm.deleteTechnique(id) },
            onDismiss = { openTechniques = false },
        )
    }

    // ── Reality check triggers dialog ─────────────────────────────────────────
    if (openTriggers) {
        RealityCheckDialog(
            triggers = triggers,
            onAdd = { text -> vm.addTrigger(text) },
            onUpdate = { item: RealityCheckTrigger, text -> vm.updateTrigger(item, text) },
            onDelete = { id -> vm.deleteTrigger(id) },
            onDismiss = { openTriggers = false },
        )
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

    val allOption = VibeConstants.VALUE_TARGET_NODE_ALL to "All watches"
    val options = listOf(allOption) + nodes

    val selectedLabel = options.firstOrNull { it.first == selectedNodeId }?.second
        ?: "All watches"

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth(),
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
        OutlinedButton(onClick = onRefresh) {
            Text("Refresh watches")
        }
    }
}
