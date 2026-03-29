package com.example.vild.wear

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Handles the [AlarmManager] broadcast: acquires a short WakeLock,
 * fires a vibration via [VibrationHelper], then reschedules the next alarm.
 *
 * Uses [goAsync] so the coroutine that checks the target node ID can
 * complete before the receiver is recycled.
 */
class VibeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "vild:VibeReceiverWakeLock",
        ).apply { acquire(3_000L) }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                VibrationHelper.vibrate(context)
                VibeScheduler.schedule(context)
            } finally {
                if (wakeLock.isHeld) wakeLock.release()
                pendingResult.finish()
            }
        }
    }
}
