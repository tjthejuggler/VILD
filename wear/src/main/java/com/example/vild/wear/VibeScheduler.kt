package com.example.vild.wear

import android.app.AlarmManager
import android.app.AlarmManager.AlarmClockInfo
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.vild.shared.VibeConstants
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.random.Random

private const val TAG = "VibeScheduler"

/**
 * Schedules the next vibration alarm using [AlarmManager.setAlarmClock], which is
 * **exempt from Doze mode rate-limiting**.
 *
 * [setExactAndAllowWhileIdle] has a system-enforced minimum interval of ~10 minutes
 * on API 31+, which makes it unsuitable for short reminder intervals (e.g. 1–5 min).
 * [setAlarmClock] has no such restriction and always fires at the exact requested time.
 * The trade-off is a small alarm icon in the status bar, which is acceptable for a
 * reminder app.
 *
 * Before scheduling, checks that this node's ID matches [VibeConstants.KEY_TARGET_NODE_ID]
 * (or that the target is [VibeConstants.VALUE_TARGET_NODE_ALL]).
 */
object VibeScheduler {

    private const val REQUEST_CODE = 1001

    /**
     * Schedules the next alarm if this watch is the active target.
     * Must be called from a coroutine scope because it fetches the local node ID.
     *
     * Uses [Context.getApplicationContext] internally to avoid issues with
     * short-lived BroadcastReceiver contexts.
     */
    suspend fun schedule(context: Context) {
        val appContext = context.applicationContext
        try {
            if (!isThisNodeTargeted(appContext)) {
                Log.d(TAG, "schedule: this node is NOT targeted — cancelling alarm")
                cancel(appContext)
                return
            }

            if (!VibeSettingsRepository.isEnabled(appContext)) {
                Log.d(TAG, "schedule: vibrations are DISABLED — cancelling alarm")
                cancel(appContext)
                return
            }

            scheduleAlarm(appContext)
        } catch (e: Exception) {
            Log.e(TAG, "schedule: FAILED to schedule alarm", e)
        }
    }

    fun cancel(context: Context) {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(buildPendingIntent(appContext))
        Log.d(TAG, "cancel: alarm cancelled")
    }

    /**
     * Computes the next trigger time and sets the alarm using [AlarmManager.setAlarmClock].
     *
     * [setAlarmClock] is used instead of [setExactAndAllowWhileIdle] because:
     * - It is exempt from Doze mode rate-limiting (no ~10 min minimum interval)
     * - It always fires at the exact requested time
     * - It works reliably on all API levels ≥ 21
     */
    private fun scheduleAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val now = System.currentTimeMillis()
        val snoozeUntil = VibeSettingsRepository.snoozeUntilTimestamp(context)

        val nextTriggerTime: Long = if (now < snoozeUntil) {
            Log.d(TAG, "scheduleAlarm: currently snoozed until $snoozeUntil — scheduling at snooze end")
            snoozeUntil
        } else {
            val minMs = VibeSettingsRepository.freqMinMinutes(context) * 60_000L
            val maxMs = VibeSettingsRepository.freqMaxMinutes(context) * 60_000L
            val randomMs = if (maxMs > minMs) Random.nextLong(minMs, maxMs + 1) else minMs
            Log.d(TAG, "scheduleAlarm: minMs=$minMs, maxMs=$maxMs, randomMs=$randomMs")
            now + randomMs
        }

        val delaySeconds = (nextTriggerTime - now) / 1000
        Log.d(TAG, "scheduleAlarm: nextTriggerTime=$nextTriggerTime (in ${delaySeconds}s)")

        val pendingIntent = buildPendingIntent(context)

        // setAlarmClock is exempt from Doze rate-limiting and always fires on time.
        // The second parameter (showIntent) can be null — we don't need to show a UI
        // when the user taps the alarm icon.
        val alarmClockInfo = AlarmClockInfo(nextTriggerTime, null)
        alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
        Log.d(TAG, "scheduleAlarm: setAlarmClock SET successfully (in ${delaySeconds}s)")
    }

    /**
     * Returns true if the stored target node matches this device or is set to "all".
     *
     * Uses a 5-second timeout on the Play Services call to prevent hanging
     * when called from a BroadcastReceiver context. Defaults to true on
     * timeout or failure so the alarm chain is never broken.
     */
    private suspend fun isThisNodeTargeted(context: Context): Boolean {
        val targetNodeId = VibeSettingsRepository.targetNodeId(context)
        if (targetNodeId == VibeConstants.VALUE_TARGET_NODE_ALL) {
            Log.d(TAG, "isThisNodeTargeted: target is ALL — returning true")
            return true
        }
        return try {
            val localNode = withTimeoutOrNull(5_000L) {
                Wearable.getNodeClient(context).localNode.await()
            }
            if (localNode == null) {
                Log.w(TAG, "isThisNodeTargeted: timed out getting local node ID — defaulting to true")
                return true
            }
            val matched = localNode.id == targetNodeId
            Log.d(TAG, "isThisNodeTargeted: localNodeId=${localNode.id}, targetNodeId=$targetNodeId, matched=$matched")
            matched
        } catch (e: Exception) {
            // If we can't determine the local node ID, default to true so the alarm
            // is still scheduled rather than silently dropped.
            Log.w(TAG, "isThisNodeTargeted: failed to get local node ID — defaulting to true", e)
            true
        }
    }

    private fun buildPendingIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, VibeReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
}
