package com.example.vild.ipc

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.vild.data.AppSettingsRepository
import com.example.vild.data.NightVibeLogRepository
import com.example.vild.data.NightVibeNotifier
import com.example.vild.data.NightVibeScheduler
import com.example.vild.data.SleepLearning
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
 * A vibe fires only when a fresh bedtime anchor exists and the current moment
 * is past the quiet gap (i.e. the scheduler decided this is a REM-targeted
 * pulse). Posts the transient night notification (mirrored + vibrated by the
 * paired watch), records the send time, feeds accumulated user feedback to
 * [SleepLearning], and re-arms with a freshly computed time. The night ends at
 * the configured wall-clock cap or when the user signals wake-up (Day-mode
 * switch).
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
                val logRepo = NightVibeLogRepository(appContext)

                if (!settings.isEnabled) {
                    Log.d(TAG, "Night vibes disabled — disarming")
                    NightVibeScheduler.cancel(appContext)
                } else {
                    val snoozed = settings.snoozeUntilTimestamp > System.currentTimeMillis()
                    when {
                        snoozed -> Log.d(TAG, "Skipped (snoozed) — re-arming only")
                        settings.bedtimeAnchorMs <= 0L ->
                            Log.d(TAG, "Skipped (no bedtime anchor yet) — re-arming only")
                        else -> {
                            Log.d(TAG, "Bedtime anchored — posting night vibe")
                            NightVibeNotifier.show(appContext)
                            logRepo.record(System.currentTimeMillis())
                            runAdaptiveLearning(settingsRepo, logRepo, settings.remIntervalMinutes)
                        }
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

    /**
     * Batched auto-tuning of the sleep-cycle interval: applies the engine's
     * decision when enough feedback has accumulated, and persists it.
     */
    private suspend fun runAdaptiveLearning(
        settingsRepo: AppSettingsRepository,
        logRepo: NightVibeLogRepository,
        currentInterval: Int,
    ) {
        if (!settingsRepo.settingsFlow.first().adaptiveInterval) return
        val entries = logRepo.entriesFlow.first()
        val adjustment = SleepLearning.evaluate(entries, currentInterval) ?: return

        if (adjustment.applied) {
            settingsRepo.save(
                settingsRepo.settingsFlow.first()
                    .copy(remIntervalMinutes = adjustment.newInterval),
            )
        }
        Log.i(TAG, "Learning: ${adjustment.reason} (${adjustment.consumed.size} annotations)")
    }
}
