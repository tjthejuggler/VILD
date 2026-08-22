package com.example.vild.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.vild.ipc.NagReceiver

/**
 * Schedules the recurring "nag" alarm that keeps re-posting the reality check
 * notification until the user confirms they have READ and DONE today's check.
 *
 * The receiver re-schedules itself on every fire, so the cycle only ends when
 * [NagReceiver] observes a complete day log (or [cancel] is called).
 */
object NagScheduler {

    private const val TAG = "NagScheduler"
    const val ACTION_NAG = "com.example.vild.ACTION_NAG"

    /** How often the user is re-bothered while the check is unconfirmed. */
    const val INTERVAL_MS = 30L * 60_000L // 30 minutes

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, NagReceiver::class.java).apply { action = ACTION_NAG }
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /** Arms the next nag alarm (one-shot; the receiver re-arms itself). */
    fun schedule(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val at = System.currentTimeMillis() + INTERVAL_MS
        // setAndAllowWhileIdle fires even in Doze — the dream must not be escaped.
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pendingIntent(context))
        Log.d(TAG, "Nag armed for +${INTERVAL_MS / 60000} min")
    }

    /** Disarms the nag alarm (called once the day's check is complete). */
    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent(context))
        Log.d(TAG, "Nag disarmed")
    }
}
