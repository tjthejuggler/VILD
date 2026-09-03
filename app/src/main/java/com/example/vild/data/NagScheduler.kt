package com.example.vild.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.vild.ipc.NagReceiver
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Schedules the recurring "nag" alarm that keeps re-posting the reality check
 * notification until the user has hit today's practice goals
 * ([DAILY_READ_GOAL] reads and [DAILY_DONE_GOAL] dones).
 *
 * The interval is ADAPTIVE: the remaining practice rounds for the day are
 * spread evenly over the time left until midnight. Fewer rounds done → shorter
 * interval (more frequent nudges); goals met or day over → no more nagging.
 * The receiver re-schedules itself on every fire with a freshly computed
 * interval.
 */
object NagScheduler {

    private const val TAG = "NagScheduler"
    const val ACTION_NAG = "com.example.vild.ACTION_NAG"

    /** Fallback interval used when the day's state is unknown (errors). */
    const val INTERVAL_MS = 30L * 60_000L // 30 minutes

    /** Nagging is spread over the waking part of the day, not the small hours. */
    private const val LAST_NAG_HOUR = 22

    /** Never nag more often than this, even late in the day. */
    private const val MIN_INTERVAL_MS = 20L * 60_000L // 20 minutes

    /** Never go quieter than this, so the day's goals stay in sight. */
    private const val MAX_INTERVAL_MS = 3L * 60 * 60_000L // 3 hours

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, NagReceiver::class.java).apply { action = ACTION_NAG }
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /**
     * Computes how long to wait before the next nag, given today's log.
     *
     * The remaining rounds ([RealityCheckDayLog.unitsRemaining]) are spread
     * evenly across the time left in the day (until [LAST_NAG_HOUR]): fewer
     * rounds done → shorter interval → more frequent notifications. Returns
     * null when the daily goals are already met or the day is over — the nag
     * cycle should stop in that case.
     */
    fun nextIntervalMs(log: RealityCheckDayLog): Long? {
        if (log.goalsMet) return null

        val remaining = log.unitsRemaining.coerceAtLeast(1)
        val now = LocalDateTime.now()
        val endOfDay = LocalDate.now().atTime(LocalTime.of(LAST_NAG_HOUR, 0))
        val msLeft = java.time.Duration.between(now, endOfDay).toMillis()
        if (msLeft <= 0) return null

        return (msLeft / remaining).coerceIn(MIN_INTERVAL_MS, MAX_INTERVAL_MS)
    }

    /** Arms the next nag alarm (one-shot; the receiver re-arms itself). */
    fun schedule(context: Context, intervalMs: Long = INTERVAL_MS) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val at = System.currentTimeMillis() + intervalMs
        // setAndAllowWhileIdle fires even in Doze — the dream must not be escaped.
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pendingIntent(context))
        Log.d(TAG, "Nag armed for +${intervalMs / 60000} min")
    }

    /** Disarms the nag alarm (called once the day's goals are met). */
    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent(context))
        Log.d(TAG, "Nag disarmed")
    }
}
