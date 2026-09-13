package com.example.vild.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.first

/**
 * Shared "I'm awake" sequence: the night → day mode switch plus vibe-chain
 * teardown. Invoked from every wake-up signal so they all behave identically:
 *
 * - [com.example.vild.ipc.DayModeSwitchReceiver] — Tail's habit broadcast.
 * - [com.example.vild.ipc.RealityCheckActionReceiver] — the user tapped
 *   "I read it" / "I did it" on today's dream trigger. The tap itself proves
 *   wakefulness, so it acts as a **fallback** when Tail's broadcast never
 *   arrives (broken sync, app update, missed alarm, …).
 * - [com.example.vild.MainViewModel.toggleMode] stays UI-only because it also
 *   updates Compose state; the persisted side-effects are identical.
 */
object DayModeSwitcher {

    private const val TAG = "DayModeSwitcher"

    /**
     * Forces day mode: snapshots the current settings under "night", switches
     * the active mode to "day", loads the day-mode settings, pauses the vibe
     * chain until the next Goodnight, and cancels any live night-vibe
     * notification. Idempotent — a no-op when already in day mode.
     */
    suspend fun forceDayMode(context: Context) {
        val appContext = context.applicationContext
        val repo = AppSettingsRepository(appContext)

        if (repo.activeModeFlow.first() != "night") {
            Log.d(TAG, "Already in day mode — nothing to do")
            return
        }

        // Save current night settings before switching.
        repo.saveModeSettings("night", repo.settingsFlow.first())

        // Switch to day mode.
        repo.setActiveMode("day")

        // Load day settings and persist them.
        val daySettings = repo.loadModeSettings("day")
        repo.save(daySettings)

        // Switching to day = wake-up: pause vibes until the next night starts.
        NightVibeScheduler.pauseUntilNextNight(appContext)
        NightVibeScheduler.scheduleNext(appContext)

        // Belt-and-braces: a vibe posted moments ago disappears immediately.
        NightVibeNotifier.cancel(appContext)

        Log.i(TAG, "Switched from night → day mode")
    }
}
