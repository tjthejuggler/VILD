package com.example.vild.wear

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "BootReceiver"

/**
 * Reschedules the vibration alarm after the device reboots.
 *
 * [AlarmManager] alarms are cleared on reboot, so without this receiver the
 * scheduled vibrations would stop permanently until the phone pushes new settings.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        Log.d(TAG, "onReceive: BOOT_COMPLETED — rescheduling vibration alarm")

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                VibeScheduler.schedule(context)
                Log.d(TAG, "onReceive: alarm rescheduled after boot")
            } catch (e: Exception) {
                Log.e(TAG, "onReceive: failed to reschedule alarm after boot", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
