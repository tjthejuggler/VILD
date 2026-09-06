package com.example.vild.ipc

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.vild.data.AppSettingsRepository
import com.example.vild.data.NightVibeLogRepository
import com.example.vild.data.NightVibeNotifier
import com.example.vild.data.NightVibeScheduler
import com.example.vild.data.NightWindow
import java.time.LocalDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val TAG = "NightVibeReceiver"

/**
 * Fired by [NightVibeScheduler] at each planned night-vibe time.
 *
 * If the current moment is inside the REM phase (after the gap), the app is
 * in night mode, the vibration reminder is not snoozed, and the feature is
 * enabled: posts the transient night notification (mirrored + vibrated by the
 * paired watch) and records the send time. Always re-arms itself with a
 * freshly computed time. The night ends when the app leaves night mode —
 * typically via the Tail "auto switch to Day on habit" integration.
 */
class NightVibeReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != NightVibeScheduler.ACTION_NIGHT_VIBE) return

        Log.d(TAG, "Night vibe alarm fired")
        val pendingResult = goAsync()
        val appContext = context.applicationContext

        scope.launch {
            try {
                val settingsRepo = AppSettingsRepository(appContext)
                val settings = settingsRepo.settingsFlow.first()
                val now = LocalDateTime.now()

                if (!settings.isEnabled) {
                    Log.d(TAG, "Night vibes disabled — disarming")
                    NightVibeScheduler.cancel(appContext)
                } else {
                    val inNightMode = settingsRepo.activeModeFlow.first() == "night"
                    val window = NightWindow.resolve(now, settings)
                    val snoozed = settings.snoozeUntilTimestamp > System.currentTimeMillis()
                    if (window.isInRemPhase(now) && inNightMode && !snoozed) {
                        Log.d(TAG, "In REM phase — posting night vibe")
                        NightVibeNotifier.show(appContext)
                        NightVibeLogRepository(appContext).record(System.currentTimeMillis())
                    } else {
                        Log.d(TAG, "Skipped (gap/day-mode/snoozed) — re-arming only")
                    }
                    NightVibeScheduler.scheduleNext(appContext)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Night vibe failed: ${e.message}", e)
                runCatching { NightVibeScheduler.scheduleNext(appContext) }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
