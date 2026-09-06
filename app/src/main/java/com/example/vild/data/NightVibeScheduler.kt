package com.example.vild.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.vild.ipc.NightVibeReceiver
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.flow.first

/**
 * Arms the one-shot alarm chain that drives the night vibes.
 *
 * Each night is split into two phases:
 * 1. **Gap** — from [NightVibeSettings.nightStartMinutes] for [NightVibeSettings.gapMinutes]
 *    nothing is sent (early night, little REM sleep).
 * 2. **REM phase** — from the end of the gap onwards, one notification per estimated
 *    sleep cycle ([NightVibeSettings.remIntervalMinutes]), aiming at predicted REM
 *    sessions. There is no fixed end time: the night ends when the app leaves night
 *    mode (Tail habit increment via [com.example.vild.ipc.DayModeSwitchReceiver], or
 *    the manual Day/Night toggle). [NightVibeReceiver] only posts while night mode
 *    is active.
 *
 * [NightVibeReceiver] re-arms itself on every fire with a freshly computed time.
 */
object NightVibeScheduler {

    private const val TAG = "NightVibeScheduler"
    const val ACTION_NIGHT_VIBE = "com.example.vild.ACTION_NIGHT_VIBE"

    /** Re-reads settings and arms (or disarms) the next night-vibe alarm. */
    suspend fun scheduleNext(context: Context) {
        val appContext = context.applicationContext
        val settings = AppSettingsRepository(appContext).settingsFlow.first()

        if (!settings.isEnabled) {
            cancel(appContext)
            return
        }

        val at = nextFireMs(settings) ?: run {
            Log.d(TAG, "No valid next fire time — disarming")
            cancel(appContext)
            return
        }

        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        // setAlarmClock is Doze-exempt and fires exactly — required for overnight timing.
        alarmManager.setAlarmClock(
            AlarmManager.AlarmClockInfo(at, null),
            pendingIntent(appContext),
        )
        Log.d(TAG, "Night vibe armed for ${LocalDateTime.ofInstant(Instant.ofEpochMilli(at), ZoneId.systemDefault())}")
    }

    /** Disarms the night-vibe alarm. */
    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent(context))
        Log.d(TAG, "Night vibe disarmed")
    }

    /**
     * Computes the epoch-ms when the next night vibe should fire, or `null` if
     * the computed time is not in the future.
     */
    internal fun nextFireMs(settings: NightVibeSettings): Long? {
        val now = LocalDateTime.now()
        val window = NightWindow.resolve(now, settings)

        val candidate = when {
            // Still inside the gap → fire at gap end (first REM-aimed vibe).
            now < window.gapEnd -> window.gapEnd
            // REM phase (until the next night starts) → next vibe one cycle later.
            now < window.morningEnd -> now.plusMinutes(settings.remIntervalMinutes.toLong())
            // Exactly at the next night's start → that night's gap end.
            else -> NightWindow.resolve(now, settings, nextNight = true).gapEnd
        }

        // A pending snooze delays the next vibe, but never past the window's end.
        val snoozeUntil = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(settings.snoozeUntilTimestamp),
            ZoneId.systemDefault(),
        )
        val effective = if (snoozeUntil > candidate && snoozeUntil < window.morningEnd) {
            snoozeUntil
        } else {
            candidate
        }

        return Duration.between(now, effective).toMillis().takeIf { it > 0 }
    }

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, NightVibeReceiver::class.java).apply { action = ACTION_NIGHT_VIBE }
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}

/**
 * Resolved wall-clock boundaries of a night window.
 *
 * Handles windows that wrap midnight (e.g. start 23:00 → end 08:00). The night
 * "belongs" to the date the window starts on.
 */
data class NightWindow(
    val start: LocalDateTime,
    val gapEnd: LocalDateTime,
    val morningEnd: LocalDateTime,
) {
    /** True if [now] is inside the REM phase (after the gap, before morning end). */
    fun isInRemPhase(now: LocalDateTime): Boolean = now >= gapEnd && now < morningEnd

    companion object {
        /**
         * Resolves the window for the night starting at (or most recently before) [now].
         * The window extends a full 24 h — until the next night starts. With
         * [nextNight] = true, resolves the following night's window instead.
         */
        fun resolve(now: LocalDateTime, settings: NightVibeSettings, nextNight: Boolean = false): NightWindow {
            val startCandidate = now.toLocalDate()
                .atTime(LocalTime.of(settings.nightStartMinutes / 60, settings.nightStartMinutes % 60))
            // The most recent start at-or-before now; if today's start is still ahead,
            // the relevant night began yesterday.
            val start = (if (startCandidate <= now) startCandidate else startCandidate.minusDays(1))
                .let { if (nextNight) it.plusDays(1) else it }
            return NightWindow(
                start = start,
                gapEnd = start.plusMinutes(settings.gapMinutes.toLong()),
                morningEnd = start.plusDays(1),
            )
        }
    }
}
