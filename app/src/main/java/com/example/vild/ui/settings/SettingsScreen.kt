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
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.example.vild.data.NightVibeEntry
import com.example.vild.data.NightVibeMark
import com.example.vild.data.NightVibeSettings
import com.example.vild.data.mark
import com.example.vild.data.withMark
import com.example.vild.data.groupNights
import com.example.vild.ui.theme.StarGold
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.Color
import com.example.vild.data.RealityCheckTrigger
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
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import com.example.vild.data.SleepLearning
import java.time.format.DateTimeFormatter

private val nightTimeFormat = DateTimeFormatter.ofPattern("HH:mm")

/** Formats minutes-of-day as HH:mm (values ≥ 24 h wrap, e.g. 1470 → "00:30"). */
private fun formatMinutesOfDay(minutes: Int): String {
    val wrapped = minutes % (24 * 60)
    return LocalTime.of(wrapped / 60, wrapped % 60).format(nightTimeFormat)
}

/**
 * Secondary settings screen — everything that supports the practice but is
 * not the practice itself: night vibes, advice and reality check trigger
 * management. Floats in glass over the dream sky.
 */
@Composable
fun SettingsScreen(
    vm: MainViewModel,
    onBack: () -> Unit,
) {
    val settings by vm.settings.collectAsState()
    val nightLog by vm.nightVibeLog.collectAsState()
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

            // ── Night vibes ─────────────────────────────────────────────────────
            item {
                GlassCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            "NIGHT VIBES",
                            style = MaterialTheme.typography.labelMedium,
                            color = Mist,
                        )
                        Text(
                            "Your paired watch vibrates when these notifications arrive — " +
                                "no watch app needed. Each evening the app asks if you're " +
                                "going to sleep; your Goodnight anchors tonight's schedule: " +
                                "a quiet gap, then one pulse per sleep cycle aimed at REM.",
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
                                "Enable night vibes",
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

                        Text(
                            "Bedtime prompt · ${formatMinutesOfDay(settings.bedtimePromptMinutes)} " +
                                "(asks \"Going to sleep?\" with a Goodnight button)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Mist,
                        )
                        Slider(
                            value = settings.bedtimePromptMinutes.toFloat(),
                            onValueChange = { vm.updateBedtimePrompt((it.toInt() / 15 * 15)) },
                            valueRange = 19 * 60f..25 * 60f - 15f, // up to 01:00 (25h = next-day 01:00 label via formatMinutesOfDay)
                            modifier = Modifier.fillMaxWidth(),
                        )

                        // Bedtime anchor status
                        val anchored = settings.bedtimeAnchorMs > 0L
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                if (anchored) "Goodnight confirmed · ${SleepLearning.formatTime(settings.bedtimeAnchorMs)}"
                                else "Waiting for tonight's Goodnight",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (anchored) AuroraTeal else Mist,
                            )
                            OutlinedButton(
                                onClick = { vm.confirmBedtime() },
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MoonLavender),
                            ) {
                                Text(
                                    if (anchored) "Re-anchor now" else "Goodnight now",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }

                        Text(
                            "Quiet gap · ${settings.gapMinutes / 60}h ${settings.gapMinutes % 60}m" +
                                " (no pulses after Goodnight)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Mist,
                        )
                        Slider(
                            value = settings.gapMinutes.toFloat(),
                            onValueChange = { vm.updateGapMinutes((it.toInt() / 15 * 15)) },
                            valueRange = 0f..8 * 60f,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "Adapt to my sleep",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MoonLavender,
                            )
                            Switch(
                                checked = settings.adaptiveInterval,
                                onCheckedChange = { vm.setAdaptiveInterval(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MoonLavender,
                                    checkedTrackColor = AuroraTeal.copy(alpha = 0.5f),
                                ),
                            )
                        }

                        Text(
                            "Sleep cycle length · ${settings.remIntervalMinutes} min " +
                                (if (settings.adaptiveInterval)
                                    "(auto-tuned from your Dream/Woke feedback)"
                                else "(one pulse per cycle, aimed at REM)"),
                            style = MaterialTheme.typography.bodySmall,
                            color = Mist,
                        )
                        Slider(
                            enabled = !settings.adaptiveInterval,
                            value = settings.remIntervalMinutes.toFloat(),
                            onValueChange = { vm.updateRemInterval((it.toInt() / 5 * 5)) },
                            valueRange = SleepLearning.MIN_INTERVAL_MINUTES.toFloat()..
                                SleepLearning.MAX_INTERVAL_MINUTES.toFloat(),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        if (settings.adaptiveInterval) {
                            val pending = vm.pendingFeedbackCount(nightLog)
                            Text(
                                if (pending >= SleepLearning.MIN_FEEDBACK_ENTRIES)
                                    "$pending annotations collected — the next pulse may retune the interval"
                                else
                                    "Feedback: $pending/${SleepLearning.MIN_FEEDBACK_ENTRIES} annotations before the next tune-up",
                                style = MaterialTheme.typography.bodySmall,
                                color = AuroraTeal,
                            )
                        }

                        Text(
                            "Night ends · ${formatMinutesOfDay(settings.nightEndMinutes)} " +
                                "(no pulses after this — daytime shows only the trigger of the day)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Mist,
                        )
                        Slider(
                            value = settings.nightEndMinutes.toFloat(),
                            onValueChange = { vm.updateNightEnd(it.toInt() / 15 * 15) },
                            valueRange = 60f..12 * 60f,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        OutlinedButton(
                            onClick = { vm.testNightVibe() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MoonLavender),
                        ) {
                            Text("Send test notification")
                        }

                        HorizontalDivider(color = MoonLavender.copy(alpha = 0.15f))

                        NightVibeLogSection(settings = settings, entries = nightLog, vm = vm)
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

/**
 * Shows the recorded night-vibe pulses for the last two nights, grouped by
 * the night they belong to (newest first). Each pulse shows its send time and
 * radio-style markers — exactly one of Unnoticed (default) / Dream / Woke.
 * Older nights remain in the log and are reachable via the full-history page.
 */
@Composable
private fun NightVibeLogSection(
    settings: NightVibeSettings,
    entries: List<NightVibeEntry>,
    vm: MainViewModel,
) {
    Text(
        "PREVIOUS NIGHTS",
        style = MaterialTheme.typography.labelMedium,
        color = Mist,
    )

    OutlinedButton(
        onClick = { vm.openNightChart() },
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MoonLavender),
    ) {
        Text("Full history & chart")
    }

    if (entries.isEmpty()) {
        Text(
            "No night vibes recorded yet — they appear here the morning after",
            style = MaterialTheme.typography.bodySmall,
            color = Mist,
        )
        return
    }

    val nights = groupNights(entries, settings.nightStartMinutes)
    val zone = ZoneId.systemDefault()

    nights.entries.take(2).forEach { (nightDate, nightEntries) ->
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "Night of ${nightDate.let(LocalDate::toString)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MoonLavender,
                )
                Text(
                    "${nightEntries.size} pulse${if (nightEntries.size != 1) "s" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AuroraTeal,
                )
            }
            nightEntries.forEach { entry ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        Instant.ofEpochMilli(entry.timestampMs).atZone(zone).format(nightTimeFormat),
                        style = MaterialTheme.typography.bodySmall,
                        color = Mist,
                        modifier = Modifier.width(44.dp),
                    )
                    // Radio semantics: exactly one marker per pulse — Unnoticed is
                    // the default until the user picks one of the other two.
                    EntryFlagChip("Unnoticed", entry.mark == NightVibeMark.UNNOTICED, Mist) {
                        vm.updateNightVibeEntry(entry.withMark(NightVibeMark.UNNOTICED))
                    }
                    EntryFlagChip("Dream", entry.mark == NightVibeMark.IN_DREAM, StarGold) {
                        vm.updateNightVibeEntry(entry.withMark(NightVibeMark.IN_DREAM))
                    }
                    EntryFlagChip("Woke", entry.mark == NightVibeMark.WOKE_ME, Color(0xFFEF7A7A)) {
                        vm.updateNightVibeEntry(entry.withMark(NightVibeMark.WOKE_ME))
                    }
                }
            }
        }
    }
}

/** Compact toggle chip for a night-vibe entry marker. */
@Composable
private fun EntryFlagChip(
    label: String,
    checked: Boolean,
    activeColor: Color,
    onToggle: (Boolean) -> Unit,
) {
    FilterChip(
        selected = checked,
        onClick = { onToggle(!checked) },
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = activeColor.copy(alpha = 0.35f),
            selectedLabelColor = activeColor,
        ),
    )
}
