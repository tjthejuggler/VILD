package com.example.vild.wear

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.vild.shared.VibeConstants
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

/**
 * Schedules the next vibration alarm using [AlarmManager.setExactAndAllowWhileIdle]
 * so it fires even during Doze mode.
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
        if (!isThisNodeTargeted(context)) {
            cancel(context)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (!VibeSettingsRepository.isEnabled(context)) {
            cancel(context)
            return
        }

        val now = System.currentTimeMillis()
        val snoozeUntil = VibeSettingsRepository.snoozeUntilTimestamp(context)

        val nextTriggerTime: Long = if (now < snoozeUntil) {
            snoozeUntil
        } else {
            val minMs = VibeSettingsRepository.freqMinMinutes(context) * 60_000L
            val maxMs = VibeSettingsRepository.freqMaxMinutes(context) * 60_000L
            val randomMs = if (maxMs > minMs) Random.nextLong(minMs, maxMs + 1) else minMs
            now + randomMs
        }

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            nextTriggerTime,
            buildPendingIntent(context),
        )
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(buildPendingIntent(context))
    }

    /** Returns true if the stored target node matches this device or is set to "all". */
    private suspend fun isThisNodeTargeted(context: Context): Boolean {
        val targetNodeId = VibeSettingsRepository.targetNodeId(context)
        if (targetNodeId == VibeConstants.VALUE_TARGET_NODE_ALL) return true
        val localNodeId = Wearable.getNodeClient(context).localNode.await().id
        return localNodeId == targetNodeId
    }

    private fun buildPendingIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, VibeReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
}
