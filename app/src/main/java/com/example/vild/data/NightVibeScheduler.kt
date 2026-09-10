package com.example.vild.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.vild.ipc.BedtimePromptReceiver
import com.example.vild.ipc.NightVibeReceiver
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.flow.first

/**
 * Arms the alarm chain that drives the night vibes — **relative to the user's
 * real bedtime**, not fixed clock times:
 *
 * 1. **Bedtime prompt** — every day at [NightVibeSettings.bedtimePromptMinutes]
 *    the app asks "Going to sleep?" ([BedtimePromptReceiver]). Tapping
 *    *Goodnight* stamps the bedtime anchor ([NightVibeSettings.bedtimeAnchorMs]).
 * 2. **Quiet gap** — for [NightVibeSettings.gapMinutes] after the anchor nothing
 *    is sent (early night, little REM sleep).
 * 3. **REM phase** — from the gap end onwards, one notification per estimated
 *    sleep cycle ([NightVibeSettings.remIntervalMinutes], optionally auto-tuned
 *    by [SleepLearning]). The night ends at the wall-clock cap
 *    [NightVibeSettings.nightEndMinutes] — however late the user went to bed,
 *    the day stays free of vibes.
 *
 * [NightVibeReceiver] and [BedtimePromptReceiver] re-arm themselves on every
 * fire with freshly computed times.
 */
object NightVibeScheduler {

    private const val TAG = "NightVibeScheduler"
    const val ACTION_NIGHT_VIBE = "com.example.vild.ACTION_NIGHT_VIBE"
    const val ACTION_BEDTIME_PROMPT = "com.example.vild.ACTION_BEDTIME_PROMPT"

    private const val RC_VIBE = 0
    private const val RC_PROMPT = 1

    /** An anchor older than this is stale (user never woke up via the app). */
    private const val MAX_ANCHOR_AGE_HOURS = 20L

    /** Re-reads settings and arms both the bedtime prompt and (if anchored) the next vibe. */
    suspend fun scheduleNext(context: Context) {
        val appContext = context.applicationContext
        val settings = AppSettingsRepository(appContext).settingsFlow.first()

        if (!settings.isEnabled) {
            cancel(appContext)
            return
        }

        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // 1. Bedtime prompt — always armed while the feature is on.
        val promptAt = nextPromptMs(LocalDateTime.now(), settings.bedtimePromptMinutes)
        alarmManager.setAlarmClock(
            AlarmManager.AlarmClockInfo(promptAt, null),
            promptPendingIntent(appContext),
        )
        Log.d(TAG, "Bedtime prompt armed for ${format(promptAt)}")

        // 2. Vibe chain — only when a fresh bedtime anchor exists.
        val vibeAt = nextVibeMs(settings, LocalDateTime.now())
        if (vibeAt != null) {
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(vibeAt, null),
                vibePendingIntent(appContext),
            )
            Log.d(TAG, "Night vibe armed for ${format(vibeAt)}")
        } else {
            alarmManager.cancel(vibePendingIntent(appContext))
            Log.d(TAG, "No vibe armed — waiting for Goodnight")
        }
    }

    /** Disarms both alarms. */
    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(vibePendingIntent(context))
        alarmManager.cancel(promptPendingIntent(context))
        Log.d(TAG, "Night vibes disarmed")
    }

    /**
     * Stamps **now** as the user's bedtime anchor and starts the vibe chain.
     * Called when the user taps *Goodnight* on the prompt notification.
     */
    suspend fun confirmBedtime(context: Context) {
        val appContext = context.applicationContext
        val now = System.currentTimeMillis()
        val repo = AppSettingsRepository(appContext)
        repo.save(repo.settingsFlow.first().copy(bedtimeAnchorMs = now))
        NightVibeLogRepository(appContext).recordBedtime(now)
        Log.d(TAG, "Bedtime anchor set at ${format(now)}")
        scheduleNext(appContext)
    }

    /**
     * Ends tonight's vibe chain: clears the anchor so no further vibes fire
     * until the next *Goodnight*. Called on the wake-up signal (Day-mode
     * switch), replacing the legacy snooze-based pause.
     */
    suspend fun pauseUntilNextNight(context: Context) {
        val appContext = context.applicationContext
        val repo = AppSettingsRepository(appContext)
        val settings = repo.settingsFlow.first()
        if (settings.bedtimeAnchorMs != 0L) {
            repo.save(settings.copy(bedtimeAnchorMs = 0L))
        }
        (appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager)
            .cancel(vibePendingIntent(appContext))
        Log.d(TAG, "Vibe chain paused until next Goodnight")
        // The bedtime prompt stays armed — it re-opens the cycle tonight.
    }

    /**
     * Epoch-ms of the next daily bedtime prompt (today if still ahead, else
     * tomorrow).
     */
    internal fun nextPromptMs(now: LocalDateTime, promptMinutes: Int): Long {
        var candidate = now.toLocalDate()
            .atTime(LocalTime.of(promptMinutes / 60, promptMinutes % 60))
        if (!candidate.isAfter(now)) candidate = candidate.plusDays(1)
        return candidate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    /**
     * Resolves tonight's window from the bedtime anchor. The window ends at
     * [NightVibeSettings.nightEndMinutes] (wrapping past midnight when the
     * anchor is before that end time) — never a full 24 h, so vibes can never
     * leak into the day no matter when the user goes to bed.
     */
    internal fun anchorWindow(anchorMs: Long, settings: NightVibeSettings): AnchorWindow? {
        if (anchorMs <= 0L) return null
        val anchor = LocalDateTime.ofInstant(Instant.ofEpochMilli(anchorMs), ZoneId.systemDefault())
        val endCandidate = anchor.toLocalDate()
            .atTime(LocalTime.of(settings.nightEndMinutes / 60, settings.nightEndMinutes % 60))
        val morningEnd = if (endCandidate > anchor) endCandidate else endCandidate.plusDays(1)
        val gapEnd = minOf(anchor.plusMinutes(settings.gapMinutes.toLong()), morningEnd)
        return AnchorWindow(anchor = anchor, gapEnd = gapEnd, morningEnd = morningEnd)
    }

    /**
     * Epoch-ms of the next vibe, or `null` when no vibe should currently be
     * armed: no fresh anchor, anchor stale, window over, or everything pushed
     * past the night-end cap (incl. by a snooze).
     */
    internal fun nextVibeMs(settings: NightVibeSettings, now: LocalDateTime): Long? {
        val anchorValid = settings.bedtimeAnchorMs > 0L &&
            Duration.between(
                Instant.ofEpochMilli(settings.bedtimeAnchorMs),
                now.atZone(ZoneId.systemDefault()).toInstant(),
            ).toHours() < MAX_ANCHOR_AGE_HOURS
        if (!anchorValid) return null

        val window = anchorWindow(settings.bedtimeAnchorMs, settings) ?: return null

        val candidate = when {
            now < window.anchor -> null // clock skew — ignore
            now < window.gapEnd -> window.gapEnd
            now < window.morningEnd -> now.plusMinutes(settings.remIntervalMinutes.toLong())
            else -> null // tonight is over — wait for the next Goodnight
        } ?: return null

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

        if (!effective.isAfter(now)) return null
        return effective.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    private fun vibePendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, NightVibeReceiver::class.java).apply { action = ACTION_NIGHT_VIBE }
        return PendingIntent.getBroadcast(
            context,
            RC_VIBE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun promptPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, BedtimePromptReceiver::class.java).apply { action = ACTION_BEDTIME_PROMPT }
        return PendingIntent.getBroadcast(
            context,
            RC_PROMPT,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun format(epochMs: Long): String =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMs), ZoneId.systemDefault()).toString()

    private fun format(localDateTime: LocalDateTime): String = localDateTime.toString()
}

/**
 * Resolved boundaries of tonight's anchor-based window. The night "ends" at
 * [morningEnd] regardless of when [anchor] happened — late bedtimes simply get
 * a shorter night.
 */
data class AnchorWindow(
    val anchor: LocalDateTime,
    val gapEnd: LocalDateTime,
    val morningEnd: LocalDateTime,
)
