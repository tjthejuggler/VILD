package com.example.vild.ui.realitycheck

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.vild.data.RealityCheckDayLog
import com.example.vild.ui.dream.GlassCard
import com.example.vild.ui.theme.AuroraTeal
import com.example.vild.ui.theme.MoonLavender
import com.example.vild.ui.theme.Mist
import com.example.vild.ui.theme.StarGold
import com.example.vild.ui.theme.Violet
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val confirmTimeFormat = DateTimeFormatter.ofPattern("HH:mm")

/**
 * The primary card of the app: today's reality check, shown the moment the
 * app opens. The user confirms "I have read it" and "I have done it" here;
 * until both are confirmed the card glows softly — the in-app face of the
 * nagging notification.
 */
@Composable
fun RealityCheckCard(
    log: RealityCheckDayLog?,
    readStreak: Int,
    doneStreak: Int,
    onMarkRead: () -> Unit,
    onMarkDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val read = log?.readAt != null
    val done = log?.doneAt != null
    val complete = read && done

    // Breathing glow while the check is still unconfirmed.
    val glow by rememberInfiniteTransition(label = "rcGlow").animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2_400), RepeatMode.Reverse),
        label = "rcGlowAlpha",
    )

    GlassCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // ── Header ─────────────────────────────────────────────────────────
            Text(
                text = "✦  TODAY'S REALITY CHECK  ✦",
                style = MaterialTheme.typography.labelMedium,
                color = if (complete) AuroraTeal else StarGold.copy(
                    alpha = if (complete) 1f else glow,
                ),
                textAlign = TextAlign.Center,
            )

            // ── The trigger itself — a whispered question ──────────────────────
            Text(
                text = log?.triggerText ?: "…",
                style = MaterialTheme.typography.displayLarge,
                color = MoonLavender,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 4.dp),
            )

            // ── Status line ────────────────────────────────────────────────────
            Text(
                text = when {
                    complete -> "complete — sweet dreams ✧"
                    read -> "read ✓ · now go do it"
                    done -> "done ✓ · mark it read"
                    else -> "waiting for you…"
                },
                style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                color = Mist,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(2.dp))

            // ── Confirmation buttons ───────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ConfirmButton(
                    label = if (read) "✓ Read ${formatTime(log?.readAt)}" else "I read it",
                    confirmed = read,
                    enabled = !read && log != null,
                    onClick = onMarkRead,
                    modifier = Modifier.weight(1f),
                )
                ConfirmButton(
                    label = if (done) "✓ Done ${formatTime(log?.doneAt)}" else "I did it",
                    confirmed = done,
                    enabled = !done && log != null,
                    onClick = onMarkDone,
                    modifier = Modifier.weight(1f),
                )
            }

            // ── Streaks ────────────────────────────────────────────────────────
            if (readStreak > 0 || doneStreak > 0) {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "✦ $readStreak day read streak",
                        style = MaterialTheme.typography.labelSmall,
                        color = StarGold,
                    )
                    Text(
                        text = "☾ $doneStreak day done streak",
                        style = MaterialTheme.typography.labelSmall,
                        color = AuroraTeal,
                    )
                }
            }
        }
    }
}

/**
 * A dreamy pill button. Unconfirmed: outlined with a pulsing border.
 * Confirmed: filled with aurora light.
 */
@Composable
private fun ConfirmButton(
    label: String,
    confirmed: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val glow by rememberInfiniteTransition(label = "btnGlow").animateFloat(
        initialValue = 0.45f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(1_800), RepeatMode.Reverse),
        label = "btnGlowAlpha",
    )

    if (confirmed) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier,
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = AuroraTeal.copy(alpha = 0.85f),
                contentColor = Color(0xFF06231F),
            ),
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier,
            shape = RoundedCornerShape(50),
            border = BorderStroke(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        Violet.copy(alpha = glow),
                        MoonLavender.copy(alpha = 0.4f * glow),
                    ),
                ),
            ),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MoonLavender,
            ),
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

private fun formatTime(epochMs: Long?): String =
    epochMs?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).format(confirmTimeFormat) } ?: ""
