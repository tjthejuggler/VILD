package com.example.vild.ui.night

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.vild.data.AppSettingsRepository
import com.example.vild.data.NightVibeEntry
import com.example.vild.data.NightVibeLogRepository
import com.example.vild.data.groupNights
import com.example.vild.ui.theme.AuroraTeal
import com.example.vild.ui.theme.Mist
import com.example.vild.ui.theme.MoonLavender
import com.example.vild.ui.theme.StarGold
import com.example.vild.ui.theme.VILDTheme
import com.example.vild.ui.theme.Void
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val entryTimeFormat = DateTimeFormatter.ofPattern("HH:mm")
private val nightLabelFormat = DateTimeFormatter.ofPattern("MM/dd")

/**
 * Full-screen landscape chart of night vibes: one bar per night (height =
 * pulses sent, gold overlay = pulses noticed), newest on the right. Tapping a
 * bar reveals that night's pulses with their times and lets the user mark
 * whether a pulse was noticed, noticed inside a dream, or woke them up.
 */
class NightVibeChartActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VILDTheme {
                NightVibeChartScreen(onClose = { finish() })
            }
        }
    }
}

@Composable
private fun NightVibeChartScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    var entries by remember { mutableStateOf<List<NightVibeEntry>>(emptyList()) }
    var nightStartMinutes by remember { mutableStateOf(23 * 60) }
    var selectedNight by remember { mutableStateOf<LocalDate?>(null) }

    // Keep the entry list live while the screen is open.
    val logRepo = remember { NightVibeLogRepository(context) }
    LaunchedEffect(Unit) {
        nightStartMinutes = AppSettingsRepository(context).settingsFlow.first().nightStartMinutes
        logRepo.entriesFlow.collect { entries = it }
    }

    val nights = remember(entries, nightStartMinutes) {
        groupNights(entries, nightStartMinutes)
            .map { (date, list) -> date to list } // newest night first
    }

    // Default-select the most recent night.
    LaunchedEffect(nights) {
        if (selectedNight == null || nights.none { it.first == selectedNight }) {
            selectedNight = nights.firstOrNull()?.first
        }
    }

    val zone = ZoneId.systemDefault()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Void),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // ── Header ────────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Night vibes · per night",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MoonLavender,
                )
                TextButton(onClick = onClose) { Text("Close") }
            }

            // ── Bar chart ─────────────────────────────────────────────────────
            NightBarsChart(
                nights = nights,
                selectedNight = selectedNight,
                onSelect = { selectedNight = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LegendSwatch(AuroraTeal, "sent")
                LegendSwatch(StarGold, "noticed")
            }

            // ── Selected night detail ────────────────────────────────────────
            selectedNight?.let { night ->
                val nightEntries = nights.firstOrNull { it.first == night }?.second.orEmpty()
                Text(
                    "Night of ${night.format(nightLabelFormat)} — ${nightEntries.size} pulse" +
                        if (nightEntries.size != 1) "s" else "",
                    style = MaterialTheme.typography.titleMedium,
                    color = MoonLavender,
                )
                nightEntries.forEach { entry ->
                    EntryRow(
                        entry = entry,
                        onChange = { updated ->
                            entries = entries.map {
                                if (it.timestampMs == updated.timestampMs) updated else it
                            }
                            lifecycleScopeLaunch(context) { logRepo.updateEntry(updated) }
                        },
                        zone = zone,
                    )
                }
            }
        }
    }
}

/** Launches [block] on the hosting activity's lifecycle scope. */
private fun lifecycleScopeLaunch(context: android.content.Context, block: suspend () -> Unit) {
    (context as? ComponentActivity)?.lifecycleScope?.launch { block() }
}

/**
 * Horizontal bar chart: newest night on the right. Each bar's total height is
 * the pulse count; the gold lower segment is the noticed count. Tapping a
 * bar's column selects that night.
 */
@Composable
private fun NightBarsChart(
    nights: List<Pair<LocalDate, List<NightVibeEntry>>>,
    selectedNight: LocalDate?,
    onSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val maxCount = nights.maxOfOrNull { it.second.size } ?: 1
    val labelColor = Mist
    val selectedColor = MoonLavender.copy(alpha = 0.25f)

    Column(modifier = modifier) {
        val scroll = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .horizontalScroll(scroll),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
            verticalAlignment = Alignment.Bottom,
        ) {
            nights.reversed().forEach { (night, list) -> // oldest left → newest right
                val count = list.size
                val noticed = list.count { it.noticed }
                val isSelected = night == selectedNight
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onSelect(night) },
                ) {
                    Text(
                        "$count",
                        fontSize = 11.sp,
                        color = if (isSelected) StarGold else labelColor,
                    )
                    Canvas(
                        modifier = Modifier
                            .width(26.dp)
                            .height(150.dp * (count.toFloat() / maxCount))
                            .background(
                                if (isSelected) selectedColor else Color.Transparent,
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
                            ),
                    ) {
                        val totalH = size.height
                        val noticedH = totalH * (noticed.toFloat() / count)
                        drawRect(
                            color = AuroraTeal.copy(alpha = 0.85f),
                            size = Size(size.width, totalH),
                        )
                        if (noticed > 0) {
                            drawRect(
                                color = StarGold.copy(alpha = 0.9f),
                                size = Size(size.width, noticedH),
                                topLeft = Offset(0f, totalH - noticedH),
                            )
                        }
                    }
                    Text(
                        night.format(nightLabelFormat),
                        fontSize = 10.sp,
                        color = if (isSelected) StarGold else labelColor,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendSwatch(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.width(12.dp).height(12.dp)) {
            drawRect(color = color)
        }
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = Mist)
    }
}

/** One pulse row: time + Noticed / In dream / Woke me toggles. */
@Composable
private fun EntryRow(
    entry: NightVibeEntry,
    onChange: (NightVibeEntry) -> Unit,
    zone: ZoneId,
) {
    val time = Instant.ofEpochMilli(entry.timestampMs).atZone(zone).format(entryTimeFormat)
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(time, style = MaterialTheme.typography.titleSmall, color = MoonLavender)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FlagChip("Noticed", entry.noticed, AuroraTeal) { onChange(entry.copy(noticed = it)) }
            FlagChip("In dream", entry.inDream, StarGold) { onChange(entry.copy(inDream = it)) }
            FlagChip("Woke me", entry.wokeMeUp, Color(0xFFEF7A7A)) { onChange(entry.copy(wokeMeUp = it)) }
        }
    }
}

@Composable
private fun FlagChip(
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
