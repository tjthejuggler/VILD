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
import com.example.vild.data.NightVibeSettings
import com.example.vild.data.RealityCheckTrigger
import com.example.vild.ui.SnoozeSection
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
import java.time.format.DateTimeFormatter

private val nightTimeFormat = DateTimeFormatter.ofPattern("HH:mm")

/** Formats minutes-of-day as HH:mm. */
private fun formatMinutesOfDay(minutes: Int): String =
    LocalTime.of(minutes / 60, minutes % 60).format(nightTimeFormat)

/**
 * Secondary settings screen — everything that supports the practice but is
 * not the practice itself: night vibes, snooze, advice and reality check
 * trigger management. Floats in glass over the dream sky.
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
                                "no watch app needed. A quiet gap covers the first part of " +
                                "the night; after that one pulse is sent per sleep cycle, " +
                                "aimed at REM.",
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
                            "Night starts · ${formatMinutesOfDay(settings.nightStartMinutes)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Mist,
                        )
                        Slider(
                            value = settings.nightStartMinutes.toFloat(),
                            onValueChange = { vm.updateNightStart(it.toInt()) },
                            valueRange = 18 * 60f..24 * 60f - 15f,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Text(
                            "Quiet gap · ${settings.gapMinutes / 60}h ${settings.gapMinutes % 60}m" +
                                " (no pulses after night starts)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Mist,
                        )
                        Slider(
                            value = settings.gapMinutes.toFloat(),
                            onValueChange = { vm.updateGapMinutes((it.toInt() / 15 * 15)) },
                            valueRange = 0f..8 * 60f,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Text(
                            "Sleep cycle length · ${settings.remIntervalMinutes} min " +
                                "(one pulse per cycle, aimed at REM)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Mist,
                        )
                        Slider(
                            value = settings.remIntervalMinutes.toFloat(),
                            onValueChange = { vm.updateRemInterval((it.toInt() / 5 * 5)) },
                            valueRange = 60f..120f,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Text(
                            "The night has no set end — it ends when you leave Night mode " +
                                "(Tail habit increment or the Day/Night toggle).",
                            style = MaterialTheme.typography.bodySmall,
                            color = Mist,
                        )

                        OutlinedButton(
                            onClick = { vm.testNightVibe() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MoonLavender),
                        ) {
                            Text("Send test notification")
                        }

                        HorizontalDivider(color = MoonLavender.copy(alpha = 0.15f))

                        NightVibeLogSection(settings = settings, sentTimes = nightLog)
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

/**
 * Shows the recorded night-vibe send times for the most recent nights,
 * grouped by night (date the night started on).
 */
@Composable
private fun NightVibeLogSection(settings: NightVibeSettings, sentTimes: List<Long>) {
    Text(
        "SENT THIS NIGHT",
        style = MaterialTheme.typography.labelMedium,
        color = Mist,
    )

    if (sentTimes.isEmpty()) {
        Text(
            "No night vibes sent yet",
            style = MaterialTheme.typography.bodySmall,
            color = Mist,
        )
        return
    }

    // Group send times by the date of the night window start.
    val zone = ZoneId.systemDefault()
    val nights = sentTimes
        .map { ts ->
            val time = LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), zone)
            // A time before the (possibly late) night start belongs to the previous night.
            var nightDate = time.toLocalDate()
            if (time.toLocalTime() < LocalTime.of(settings.nightStartMinutes / 60, settings.nightStartMinutes % 60)) {
                nightDate = nightDate.minusDays(1)
            }
            nightDate to time
        }
        .groupBy({ it.first }, { it.second })
        .toSortedMap(compareByDescending { it })

    nights.entries.take(3).forEach { (nightDate, times) ->
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    nightDate.let(LocalDate::toString),
                    style = MaterialTheme.typography.bodySmall,
                    color = MoonLavender,
                )
                Text(
                    "${times.size} pulse${if (times.size != 1) "s" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AuroraTeal,
                )
            }
            Text(
                times.joinToString("  ·  ") { it.format(nightTimeFormat) },
                style = MaterialTheme.typography.bodySmall,
                color = Mist,
            )
        }
    }
}
