package com.example.vild.data

import android.util.Log
import java.time.Instant
import java.time.ZoneId

/**
 * Adaptive sleep-cycle tuning ("REM learning").
 *
 * The user annotates each pulse after the fact (Unnoticed / Dream / Woke).
 * Those annotations are the training signal for one number: the sleep-cycle
 * interval between pulses. To avoid chasing noise, feedback is **accumulated**
 * and applied in batches:
 *
 * - Only **annotated** pulses count (deliberate taps, not the Unnoticed default).
 * - At least [MIN_FEEDBACK_ENTRIES] annotations are required per adjustment.
 * - The strongest signal wins: any Dream hit pulls the interval *toward* the
 *   successful spacing (good timing → repeat it); Woke pushes *away* (we were
 *   too close to surfacing); Unnoticed is neutral (no change).
 * - Movement is capped at [MAX_STEP_MINUTES] per batch and the interval is
 *   clamped to [MIN_INTERVAL_MINUTES]…[MAX_INTERVAL_MINUTES].
 * - Newest annotations weigh more ([RECENCY_DECAY]).
 *
 * A "dream hit" is a pulse the user experienced *inside the dream* — the exact
 * state VILD wants to induce. The interval that produced it is therefore a
 * keeper and the engine biases toward reproducing it.
 */
object SleepLearning {

    private const val TAG = "SleepLearning"

    /** Annotations needed before the engine acts. */
    const val MIN_FEEDBACK_ENTRIES = 3

    /** Max |change| per adjustment batch (minutes). */
    const val MAX_STEP_MINUTES = 10

    /** Sanity clamps for the learned cycle interval. */
    const val MIN_INTERVAL_MINUTES = 60
    const val MAX_INTERVAL_MINUTES = 120

    /** Older feedback counts less: weight = 0.5^(age / RECENCY_DECAY). */
    private const val RECENCY_DECAY_ENTRIES = 6.0

    /**
     * Result of one learning batch.
     *
     * @property applied   True if the interval changed.
     * @property newInterval The (possibly unchanged) interval in minutes.
     * @property reason    Human-readable explanation for the Settings UI.
     * @property consumed  The annotated entries this decision was based on
     *                     (they are "spent" and won't fire the engine again).
     */
    data class Adjustment(
        val applied: Boolean,
        val newInterval: Int,
        val reason: String,
        val consumed: List<NightVibeEntry>,
    )

    /**
     * Runs one learning pass over [entries] given the [currentInterval].
     * Returns null when there is not enough accumulated feedback yet.
     */
    fun evaluate(entries: List<NightVibeEntry>, currentInterval: Int): Adjustment? {
        val annotated = entries
            .filter { it.annotated }
            .sortedByDescending { it.timestampMs }

        if (annotated.size < MIN_FEEDBACK_ENTRIES) return null

        val now = annotated.first().timestampMs
        val weights = annotated.asSequence().mapIndexed { idx, entry ->
            entry to 0.5.pow(idx / RECENCY_DECAY_ENTRIES)
        }

        var dreamWeight = 0.0
        var wokeWeight = 0.0
        var lastDreamGapMinutes: Long? = null
        var lastWokeGapMinutes: Long? = null

        // Adjacent pulse spacing: the effective interval the user actually experienced.
        val sortedAsc = annotated.sortedBy { it.timestampMs }
        val gapsByTimestamp = sortedAsc
            .zipWithNext { prev, cur -> prev.timestampMs to (cur.timestampMs - prev.timestampMs) / 60_000 }

        for ((entry, weight) in weights) {
            when (entry.mark) {
                NightVibeMark.IN_DREAM -> {
                    dreamWeight += weight
                    gapsByTimestamp.firstOrNull { it.first == entry.timestampMs }
                        ?.let { lastDreamGapMinutes = it.second }
                }
                NightVibeMark.WOKE_ME -> {
                    wokeWeight += weight
                    gapsByTimestamp.firstOrNull { it.first == entry.timestampMs }
                        ?.let { lastWokeGapMinutes = it.second }
                }
                NightVibeMark.UNNOTICED -> Unit // neutral — no timing evidence either way
            }
        }

        // Need a dominant signal; balanced feedback means "keep as is".
        if (dreamWeight == 0.0 && wokeWeight == 0.0) return null

        var delta = 0
        var reason: String

        when {
            // Dream hits dominate → steer the interval toward the spacing that worked.
            dreamWeight >= wokeWeight -> {
                val target = lastDreamGapMinutes?.toInt()
                if (target != null && target != currentInterval) {
                    delta = (target - currentInterval).coerceIn(-MAX_STEP_MINUTES, MAX_STEP_MINUTES)
                }
                reason = buildString {
                    append("Dream detections outweigh wake-ups ")
                    append("(%.1f vs %.1f weighted)".format(dreamWeight, wokeWeight))
                    lastDreamGapMinutes?.let { append(" — good spacing was ~$it min") }
                }
            }
            // Wake-ups dominate → pulses are landing too close to awakening; widen.
            else -> {
                val target = lastWokeGapMinutes?.toInt()
                    ?.let { it + MAX_STEP_MINUTES } // aim past the disruptive spacing
                    ?: (currentInterval + MAX_STEP_MINUTES)
                delta = (target - currentInterval).coerceIn(-MAX_STEP_MINUTES, MAX_STEP_MINUTES)
                reason = "Pulses woke you up lately — spacing them further apart " +
                    "(%.1f vs %.1f weighted)".format(wokeWeight, dreamWeight)
            }
        }

        if (delta == 0) {
            return Adjustment(
                applied = false,
                newInterval = currentInterval,
                reason = "$reason — interval already optimal",
                consumed = annotated,
            )
        }

        val newInterval = (currentInterval + delta).coerceIn(MIN_INTERVAL_MINUTES, MAX_INTERVAL_MINUTES)
        Log.i(TAG, "Adjusting interval $currentInterval → $newInterval min. $reason")
        return Adjustment(
            applied = true,
            newInterval = newInterval,
            reason = "$reason — interval $currentInterval → $newInterval min",
            consumed = annotated,
        )
    }

    /** Number of pending (annotated but unconsumed) feedback pulses — for the UI. */
    fun pendingCount(entries: List<NightVibeEntry>): Int =
        entries.count { it.annotated }

    private fun Double.pow(n: Double): Double = Math.pow(this, n)

    /** Formats an epoch-ms instant as HH:mm for UI display. */
    fun formatTime(epochMs: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        Instant.ofEpochMilli(epochMs).atZone(zone).toLocalTime().toString().take(5)
}
