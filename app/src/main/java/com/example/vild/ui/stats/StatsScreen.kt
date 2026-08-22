package com.example.vild.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.vild.data.RealityCheckDayLog
import com.example.vild.data.RealityCheckStats
import com.example.vild.data.todayEpochDay
import com.example.vild.ui.dream.DreamBackground
import com.example.vild.ui.dream.GlassCard
import com.example.vild.ui.dream.rememberTiltState
import com.example.vild.ui.theme.AuroraTeal
import com.example.vild.ui.theme.Mist
import com.example.vild.ui.theme.MoonLavender
import com.example.vild.ui.theme.StarGold
import java.time.LocalDate

/**
 * Dream Stats — streaks, totals, a per-trigger leaderboard and a recent-days
 * timeline. Everything floats in glass cards over the living dream background.
 */
@Composable
fun StatsScreen(
    logs: List<RealityCheckDayLog>,
    stats: RealityCheckStats,
    onBack: () -> Unit,
) {
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
                        text = "Dream Stats",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MoonLavender,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
            }

            // ── Streaks ────────────────────────────────────────────────────────
            item {
                GlassCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("STREAKS", style = MaterialTheme.typography.labelMedium, color = Mist)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            BigStat(
                                value = "${stats.currentReadStreak}",
                                label = "read streak",
                                accent = StarGold,
                            )
                            BigStat(
                                value = "${stats.currentDoneStreak}",
                                label = "done streak",
                                accent = AuroraTeal,
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            BigStat(
                                value = "${stats.bestReadStreak}",
                                label = "best read",
                                accent = Mist,
                            )
                            BigStat(
                                value = "${stats.bestDoneStreak}",
                                label = "best done",
                                accent = Mist,
                            )
                        }
                    }
                }
            }

            // ── Totals ─────────────────────────────────────────────────────────
            item {
                GlassCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("TOTALS", style = MaterialTheme.typography.labelMedium, color = Mist)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            BigStat(value = "${stats.totalDays}", label = "days", accent = MoonLavender)
                            BigStat(value = "${stats.totalRead}", label = "read", accent = StarGold)
                            BigStat(value = "${stats.totalDone}", label = "done", accent = AuroraTeal)
                        }
                        val pct = if (stats.totalDays == 0) 0
                        else stats.totalDone * 100 / stats.totalDays
                        Text(
                            "$pct% of days fully completed",
                            style = MaterialTheme.typography.bodySmall,
                            color = Mist,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            // ── Trigger leaderboard ────────────────────────────────────────────
            if (stats.perTrigger.isNotEmpty()) {
                item {
                    GlassCard {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text("TRIGGERS", style = MaterialTheme.typography.labelMedium, color = Mist)
                            stats.perTrigger.forEach { trigger ->
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text(
                                            trigger.text,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MoonLavender,
                                            modifier = Modifier.weight(1f, fill = false),
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            "✦${trigger.timesRead} ☾${trigger.timesDone}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Mist,
                                        )
                                    }
                                    // Progress: done / shown
                                    val fraction = if (trigger.timesShown == 0) 0f
                                    else trigger.timesDone.toFloat() / trigger.timesShown
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(3.dp),
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                                                .height(3.dp)
                                                .alpha(0.7f),
                                        ) {
                                            Surface(
                                                color = AuroraTeal.copy(alpha = 0.6f),
                                                shape = CircleShape,
                                                modifier = Modifier.fillMaxSize(),
                                            ) {}
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Recent days timeline ───────────────────────────────────────────
            item {
                GlassCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text("LAST 14 NIGHTS", style = MaterialTheme.typography.labelMedium, color = Mist)
                        val byDay = logs.associateBy { it.epochDay }
                        val today = todayEpochDay()
                        val days = (13L downTo 0L).map { today - it }
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            days.chunked(7).forEach { week ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    week.forEach { day ->
                                        val log = byDay[day]
                                        DayDot(log = log, label = LocalDate.ofEpochDay(day))
                                    }
                                }
                            }
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(top = 4.dp),
                        ) {
                            LegendDot(color = AuroraTeal, label = "read + done")
                            LegendDot(color = StarGold, label = "one of two")
                            LegendDot(color = Color(0x339D94C7), label = "none")
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun BigStat(value: String, label: String, accent: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.displayMedium,
            color = accent,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Mist,
        )
    }
}

@Composable
private fun DayDot(log: RealityCheckDayLog?, label: LocalDate) {
    val (color, glow) = when {
        log == null -> Color(0x339D94C7) to false
        log.isComplete -> AuroraTeal to true
        log.readAt != null || log.doneAt != null -> StarGold to true
        else -> Color(0x339D94C7) to false
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            color = color,
            shape = CircleShape,
            modifier = Modifier
                .size(16.dp)
                .alpha(if (glow) 0.9f else 0.6f),
        ) {}
        Text(
            text = "${label.dayOfMonth}",
            style = MaterialTheme.typography.labelSmall,
            color = Mist,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            color = color,
            shape = CircleShape,
            modifier = Modifier.size(8.dp),
        ) {}
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Mist,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}
