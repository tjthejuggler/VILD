package com.example.vild.wear

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "VibeReceiver"

/**
 * Handles the [AlarmManager] broadcast: starts [VibeVibrationService] to
 * perform the vibration from a foreground-like service context (which is
 * not subject to Wear OS background vibration suppression), then
 * reschedules the next alarm.
 *
 * Uses [goAsync] so the coroutine that checks the target node ID can
 * complete before the receiver is recycled.
 */
class VibeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive: alarm fired — action=${intent.action}")
        val pendingResult = goAsync()
        val appContext = context.applicationContext

        val powerManager = appContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "vild:VibeReceiverWakeLock",
        ).apply { acquire(10_000L) } // 10s to match goAsync() window

        // Start the vibration service — vibrating from a Service context
        // is more reliable on Wear OS than from a BroadcastReceiver.
        try {
            Log.d(TAG, "onReceive: starting VibeVibrationService")
            val serviceIntent = Intent(appContext, VibeVibrationService::class.java)
            appContext.startForegroundService(serviceIntent)
            Log.d(TAG, "onReceive: VibeVibrationService started")
        } catch (e: Exception) {
            Log.e(TAG, "onReceive: failed to start VibeVibrationService, falling back to direct vibrate", e)
            try {
                VibrationHelper.vibrate(appContext)
            } catch (e2: Exception) {
                Log.e(TAG, "onReceive: direct vibrate also failed", e2)
            }
        }

        // Reschedule on IO thread (needs suspend for Play Services node check)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "onReceive: rescheduling next alarm")
                VibeScheduler.schedule(appContext)
                Log.d(TAG, "onReceive: reschedule complete")
            } catch (e: Exception) {
                Log.e(TAG, "onReceive: FAILED to reschedule", e)
            } finally {
                if (wakeLock.isHeld) wakeLock.release()
                pendingResult.finish()
            }
        }
    }
}
