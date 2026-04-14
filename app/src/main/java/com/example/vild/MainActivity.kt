package com.example.vild

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vild.shared.VibeConstants
import com.example.vild.ui.PresetSection
import com.example.vild.ui.SnoozeSection
import com.example.vild.ui.VibrationSection
import com.example.vild.data.AdviceItem
import com.example.vild.ui.advice.AdviceBanner
import com.example.vild.ui.advice.AdviceNotesDialog
import com.example.vild.ui.settings.SettingsScreen
import com.example.vild.ui.theme.VILDTheme

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* result ignored — best-effort */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request POST_NOTIFICATIONS permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

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
    val syncStatus by vm.syncStatus.collectAsState()
    val activeMode by vm.activeMode.collectAsState()
    val adviceState by vm.adviceState.collectAsState()
    val autoSwitchDayOnHabit by vm.autoSwitchDayOnHabit.collectAsState()
    val triggers by vm.triggers.collectAsState()

    var showSettings by remember { mutableStateOf(false) }
    var notesAdvice by remember { mutableStateOf<AdviceItem?>(null) }

    if (showSettings) {
        SettingsScreen(
            adviceBySection = adviceState.adviceBySection,
            triggers = triggers,
            isTailInstalled = vm.isTailInstalled,
            autoSwitchDayOnHabit = autoSwitchDayOnHabit,
            onAutoSwitchDayOnHabitChanged = { vm.setAutoSwitchDayOnHabit(it) },
            onAddAdvice = { section, text -> vm.addAdvice(section, text) },
            onUpdateAdvice = { item, text -> vm.updateAdvice(item, text) },
            onDeleteAdvice = { id -> vm.deleteAdvice(id) },
            onAddTrigger = { text -> vm.addTrigger(text) },
            onUpdateTrigger = { item, text -> vm.updateTrigger(item, text) },
            onDeleteTrigger = { id -> vm.deleteTrigger(id) },
            onBack = { showSettings = false },
        )
        return
    }

    // Shared scroll state for parallax effect.
    val scrollState = rememberScrollState()

    // Solid black background with parallax icon behind everything.
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Parallax background icon: moves at 30% of scroll speed.
        val parallaxOffset = -(scrollState.value * 0.3f)
        Image(
            painter = painterResource(R.drawable.vild_icon),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .align(Alignment.Center)
                .offset { IntOffset(0, parallaxOffset.toInt()) }
                .graphicsLayer(scaleX = 2.5f, scaleY = 2.5f),
            contentScale = ContentScale.Fit,
            alpha = 0.7f,
        )

        Scaffold(
            containerColor = Color.Transparent,
            contentColor = Color.White,
            topBar = {
                TopAppBar(
                    title = { Text("VILD – Vibration Remote") },
                    actions = {
                        IconButton(onClick = { showSettings = true }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = Color.White,
                            )
                        }
                    },
                    colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White,
                    ),
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // ── Advice banner ─────────────────────────────────────────────
                val currentSection = activeMode
                val adviceList = adviceState.adviceBySection[currentSection] ?: emptyList()
                val currentIndex = adviceState.currentIndex[currentSection] ?: 0
                AdviceBanner(
                    section = currentSection,
                    adviceList = adviceList,
                    currentIndex = currentIndex,
                    onNext = { vm.nextRandomAdvice(currentSection) },
                    onPrevious = { vm.previousAdvice(currentSection) },
                    onTap = { item -> notesAdvice = item },
                )

                notesAdvice?.let { item ->
                    AdviceNotesDialog(
                        advice = item,
                        onSave = { notes -> vm.updateAdviceNotes(item.id, notes) },
                        onDismiss = { notesAdvice = null },
                    )
                }

                SyncStatusBar(syncStatus = syncStatus)

                DayNightToggle(activeMode = activeMode, onToggle = { vm.toggleMode() })

                // ── Master toggle ────────────────────────────────────────────
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

                // ── Node selector ────────────────────────────────────────────
                SectionLabel("Active Watch")
                NodeSelector(
                    nodes = nodes.map { it.id to it.displayName },
                    selectedNodeId = settings.targetNodeId,
                    onNodeSelected = { vm.updateTargetNode(it) },
                    onRefresh = { vm.refreshNodes() },
                )

                HorizontalDivider()

                // ── Frequency ────────────────────────────────────────────────
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

                // ── Vibration settings + Vibrate Now ─────────────────────────
                SectionLabel("Vibration")
                VibrationSection(settings = settings, vm = vm)

                HorizontalDivider()

                // ── Presets ──────────────────────────────────────────────────
                SectionLabel("Presets")
                PresetSection(vm = vm)

                HorizontalDivider()

                // ── Snooze ───────────────────────────────────────────────────
                SectionLabel("Snooze")
                SnoozeSection(settings = settings, vm = vm)

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

// ── Helpers ──────────────────────────────────────────────────────────────────

/**
 * A two-button segmented toggle for switching between Day and Night modes.
 * The active mode button is filled; the inactive one is outlined.
 * Uses plain-text symbols (not colour emojis) to stay greyscale.
 */
@Composable
private fun DayNightToggle(activeMode: String, onToggle: () -> Unit) {
    val isDay = activeMode == "day"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (isDay) {
            Button(
                onClick = {},
                modifier = Modifier.weight(1f),
            ) {
                Text("☼  Day")
            }
            OutlinedButton(
                onClick = onToggle,
                modifier = Modifier.weight(1f),
            ) {
                Text("☽  Night")
            }
        } else {
            OutlinedButton(
                onClick = onToggle,
                modifier = Modifier.weight(1f),
            ) {
                Text("☼  Day")
            }
            Button(
                onClick = {},
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                ),
            ) {
                Text("☽  Night")
            }
        }
    }
}


/**
 * A slim banner shown at the top of the content area indicating the last sync result.
 *
 * - Never synced: hidden (no banner shown).
 * - Last sync succeeded: green tint + "✓ Synced X sec ago".
 * - Last sync failed: error tint + "✗ Sync failed".
 */
@Composable
private fun SyncStatusBar(syncStatus: SyncStatus) {
    if (syncStatus.lastSyncTimestamp == 0L) return

    val secondsAgo = ((System.currentTimeMillis() - syncStatus.lastSyncTimestamp) / 1_000).toInt()
    val label = if (syncStatus.lastSyncSuccess) {
        "✓ Synced ${secondsAgo}s ago"
    } else {
        "✗ Sync failed"
    }
    val containerColor = if (syncStatus.lastSyncSuccess) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.errorContainer
    }
    val contentColor = if (syncStatus.lastSyncSuccess) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onErrorContainer
    }

    Surface(
        color = containerColor,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp),
    )
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
