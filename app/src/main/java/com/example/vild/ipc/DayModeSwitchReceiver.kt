package com.example.vild.ipc

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.vild.data.AppSettingsRepository
import com.example.vild.data.WearSyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val TAG = "DayModeSwitchReceiver"

/**
 * Listens for Tail's `ACTION_HABIT_INCREMENTED` broadcast.
 *
 * When the user has enabled "Auto switch to Day mode on habit" in VILD settings
 * **and** VILD is currently in night mode, this receiver:
 * 1. Saves the current night-mode settings.
 * 2. Switches the active mode to "day".
 * 3. Loads the day-mode settings.
 * 4. Pushes the day-mode settings to the watch via [WearSyncManager].
 *
 * This works even when VILD's UI is not open because manifest-registered
 * receivers are woken by the system.
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
                val syncManager = WearSyncManager(appContext)

                // Check if the feature is enabled
                val enabled = repo.autoSwitchDayOnHabitFlow.first()
                if (!enabled) {
                    Log.d(TAG, "Auto-switch day on habit is disabled — ignoring")
                    return@launch
                }

                // Check if currently in night mode
                val currentMode = repo.activeModeFlow.first()
                if (currentMode != "night") {
                    Log.d(TAG, "Already in '$currentMode' mode — no switch needed")
                    return@launch
                }

                // Save current night settings before switching
                val currentSettings = repo.settingsFlow.first()
                repo.saveModeSettings("night", currentSettings)

                // Switch to day mode
                repo.setActiveMode("day")

                // Load day settings and push to watch
                val daySettings = repo.loadModeSettings("day")
                repo.save(daySettings)
                val success = syncManager.pushSettings(daySettings)

                Log.i(TAG, "Switched from night → day mode (push success=$success)")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to auto-switch to day mode: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
