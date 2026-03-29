package com.example.vild.wear

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Handles the [AlarmManager] broadcast: acquires a short WakeLock,
 * fires a single vibration, then reschedules the next alarm.
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
                vibrate(context)
                VibeScheduler.schedule(context)
            } finally {
                if (wakeLock.isHeld) wakeLock.release()
                pendingResult.finish()
            }
        }
    }

    private fun vibrate(context: Context) {
        val intensity = VibeSettingsRepository.vibrationIntensity(context)
        val effect = VibrationEffect.createOneShot(500L, intensity)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator.vibrate(effect)
        }
    }
}
