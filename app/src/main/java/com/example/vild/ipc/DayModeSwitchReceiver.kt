package com.example.vild.ipc

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.vild.data.AppSettingsRepository
import com.example.vild.data.DayModeSwitcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val TAG = "DayModeSwitchReceiver"

/**
 * Listens for Tail's `ACTION_HABIT_INCREMENTED` broadcast.
 *
 * When the user has enabled "Auto switch to Day mode on habit" in VILD settings,
 * the habit increment is treated as a wake-up signal and the shared
 * [DayModeSwitcher] sequence runs: night settings are snapshotted, day mode is
 * activated, and the night-vibe chain is paused until the next Goodnight.
 *
 * This works even when VILD's UI is not open because manifest-registered
 * receivers are woken by the system. If the broadcast is missed, tapping
 * "I read it" on the daily dream trigger ([RealityCheckActionReceiver])
 * triggers the same switch as a fallback.
 */
class DayModeSwitchReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "com.example.tail.ACTION_HABIT_INCREMENTED") return

        Log.d(TAG, "Received ACTION_HABIT_INCREMENTED broadcast")

        val pendingResult = goAsync()
        val appContext = context.applicationContext

        scope.launch {
            try {
                val repo = AppSettingsRepository(appContext)

                val enabled = repo.autoSwitchDayOnHabitFlow.first()
                if (!enabled) {
                    Log.d(TAG, "Auto-switch day on habit is disabled — ignoring")
                    return@launch
                }

                DayModeSwitcher.forceDayMode(appContext)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to auto-switch to day mode: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
