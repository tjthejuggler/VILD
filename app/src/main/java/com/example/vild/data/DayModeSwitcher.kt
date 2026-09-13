package com.example.vild.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.first

/**
 * Shared "I'm awake" sequence: vibe-chain teardown plus the night → day mode
 * switch. Invoked from every wake-up signal so they all behave identically:
 *
 * - [com.example.vild.ipc.TailHabitSyncReceiver] — any manual habit increment
 *   in Tail (the first one each morning is the natural wake-up signal).
 * - [com.example.vild.ipc.MorningActionReceiver] — the "Good morning" button.
 * - [com.example.vild.ipc.RealityCheckActionReceiver] — the user tapped
 *   "I read it" / "I did it" on the reality check notification.
 * - [com.example.vild.MainViewModel.markToday] — the same taps inside the app.
 * - [com.example.vild.ipc.MorningActionReceiver] — the "Good morning" button.
 * - [com.example.vild.MainViewModel.toggleMode] stays UI-only because it also
 *   updates Compose state; the persisted side-effects are identical.
 */
object DayModeSwitcher {

    private const val TAG = "DayModeSwitcher"

    /**
     * Tears down the live night and forces day mode:
     *
     * 1. **Unconditionally** clears the bedtime anchor, cancels the vibe alarm
     *    and any live night-vibe / morning-prompt notification. The clear must
     *    happen even when the app is already in day mode — a stale anchor from
     *    last night is exactly what kept vibes firing after sunrise.
     * 2. When the app is actually in night mode: snapshots the night settings,
     *    switches to day, loads the day settings.
     * 3. Re-arms the bedtime-prompt / morning-prompt alarms; no vibe alarm
     *    until the next Goodnight.
     *
     * Idempotent — safe to call from multiple signals.
     */
    suspend fun forceDayMode(context: Context) {
        val appContext = context.applicationContext
        val repo = AppSettingsRepository(appContext)

        // 1. Tear down the live night — always, regardless of the active mode.
        NightVibeScheduler.pauseUntilNextNight(appContext)
        NightVibeNotifier.cancel(appContext)
        MorningPromptNotifier.cancel(appContext)

        // 2. Mode switch — only when actually in night mode.
        if (repo.activeModeFlow.first() == "night") {
            // Save current night settings before switching.
            repo.saveModeSettings("night", repo.settingsFlow.first())

            repo.setActiveMode("day")

            // Load day settings and persist them.
            val daySettings = repo.loadModeSettings("day")
            repo.save(daySettings)

            Log.i(TAG, "Switched from night → day mode")
        } else {
            Log.d(TAG, "Wake-up signal in day mode — night chain cleared")
        }

        // 3. Re-arm the prompts; the vibe chain stays off until the next Goodnight.
        NightVibeScheduler.scheduleNext(appContext)
    }
}
