package com.example.vild.wear

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.vild.shared.VibeConstants
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

private const val TAG = "VibeScheduler"

/**
 * Schedules the next vibration alarm using [AlarmManager.setExactAndAllowWhileIdle]
 * so it fires even during Doze mode.  Falls back to [AlarmManager.setAndAllowWhileIdle]
 * when the exact-alarm permission has not been granted (API 31+).
 *
 * Before scheduling, checks that this node's ID matches [VibeConstants.KEY_TARGET_NODE_ID]
 * (or that the target is [VibeConstants.VALUE_TARGET_NODE_ALL]).
 */
object VibeScheduler {

    private const val REQUEST_CODE = 1001

    /**
     * Schedules the next alarm if this watch is the active target.
     * Must be called from a coroutine scope because it fetches the local node ID.
     */
    suspend fun schedule(context: Context) {
        try {
            if (!isThisNodeTargeted(context)) {
                Log.d(TAG, "schedule: this node is NOT targeted — cancelling alarm")
                cancel(context)
                return
            }

            if (!VibeSettingsRepository.isEnabled(context)) {
                Log.d(TAG, "schedule: vibrations are DISABLED — cancelling alarm")
                cancel(context)
                return
            }

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            val now = System.currentTimeMillis()
            val snoozeUntil = VibeSettingsRepository.snoozeUntilTimestamp(context)

            val nextTriggerTime: Long = if (now < snoozeUntil) {
                Log.d(TAG, "schedule: currently snoozed until $snoozeUntil — scheduling at snooze end")
                snoozeUntil
            } else {
                val minMs = VibeSettingsRepository.freqMinMinutes(context) * 60_000L
                val maxMs = VibeSettingsRepository.freqMaxMinutes(context) * 60_000L
                val randomMs = if (maxMs > minMs) Random.nextLong(minMs, maxMs + 1) else minMs
                Log.d(TAG, "schedule: minMs=$minMs, maxMs=$maxMs, randomMs=$randomMs")
                now + randomMs
            }

            val delaySeconds = (nextTriggerTime - now) / 1000
            Log.d(TAG, "schedule: nextTriggerTime=$nextTriggerTime (in ${delaySeconds}s)")

            val pendingIntent = buildPendingIntent(context)

            // Try exact alarm first; fall back to inexact if permission is missing
            if (canScheduleExactAlarms(alarmManager)) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextTriggerTime,
                    pendingIntent,
                )
                Log.d(TAG, "schedule: exact alarm SET successfully")
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextTriggerTime,
                    pendingIntent,
                )
                Log.w(TAG, "schedule: exact alarm permission denied — used inexact alarm instead")
            }
        } catch (e: Exception) {
            Log.e(TAG, "schedule: FAILED to schedule alarm", e)
        }
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(buildPendingIntent(context))
        Log.d(TAG, "cancel: alarm cancelled")
    }

    /** Returns true if the stored target node matches this device or is set to "all". */
    private suspend fun isThisNodeTargeted(context: Context): Boolean {
        val targetNodeId = VibeSettingsRepository.targetNodeId(context)
        if (targetNodeId == VibeConstants.VALUE_TARGET_NODE_ALL) {
            Log.d(TAG, "isThisNodeTargeted: target is ALL — returning true")
            return true
        }
        return try {
            val localNodeId = Wearable.getNodeClient(context).localNode.await().id
            val matched = localNodeId == targetNodeId
            Log.d(TAG, "isThisNodeTargeted: localNodeId=$localNodeId, targetNodeId=$targetNodeId, matched=$matched")
            matched
        } catch (e: Exception) {
            // If we can't determine the local node ID, default to true so the alarm
            // is still scheduled rather than silently dropped.
            Log.w(TAG, "isThisNodeTargeted: failed to get local node ID — defaulting to true", e)
            true
        }
    }

    /**
     * Checks whether the app can schedule exact alarms.
     * On API < 31 this is always true.  On API 31+ it requires user permission.
     */
    private fun canScheduleExactAlarms(alarmManager: AlarmManager): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
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
