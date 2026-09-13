package com.example.vild.ipc

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.vild.data.AppSettingsRepository
import com.example.vild.data.MorningPromptNotifier
import com.example.vild.data.NightVibeScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val TAG = "MorningPromptReceiver"

/**
 * Fired by [NightVibeScheduler] at the night-end time (internal default
 * 08:00). Posts the "Good morning" prompt **only while last night's cycle is
 * still live** — i.e. night vibes are enabled and no wake-up signal (Tail
 * habit, read/done tap, Good morning button) has cleared the bedtime anchor
 * yet. If the user is already awake the prompt is skipped silently.
 */
class MorningPromptReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != NightVibeScheduler.ACTION_MORNING_PROMPT) return

        Log.d(TAG, "Morning prompt alarm fired")
        val pendingResult = goAsync()
        val appContext = context.applicationContext

        scope.launch {
            try {
                val settings = AppSettingsRepository(appContext).settingsFlow.first()
                when {
                    !settings.isEnabled ->
                        Log.d(TAG, "Night vibes disabled — skipping morning prompt")
                    settings.bedtimeAnchorMs <= 0L ->
                        Log.d(TAG, "No live bedtime anchor — user already signalled awake; skipping")
                    else -> {
                        Log.d(TAG, "Night cycle still live — posting morning prompt")
                        MorningPromptNotifier.show(appContext)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Morning prompt failed: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
