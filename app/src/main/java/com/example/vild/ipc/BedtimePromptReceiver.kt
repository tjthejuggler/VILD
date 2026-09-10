package com.example.vild.ipc

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.vild.data.AppSettingsRepository
import com.example.vild.data.BedtimePromptNotifier
import com.example.vild.data.NightVibeScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val TAG = "BedtimePromptReceiver"

/**
 * Fired by [NightVibeScheduler] once a day at the configured prompt time.
 * Posts the "Going to sleep?" notification with a **Goodnight** action
 * ([BedtimePromptNotifier]); the whole night schedule is measured from the
 * moment the user taps it. Always re-arms the prompt for the next day.
 */
class BedtimePromptReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != NightVibeScheduler.ACTION_BEDTIME_PROMPT) return

        Log.d(TAG, "Bedtime prompt alarm fired")
        val pendingResult = goAsync()
        val appContext = context.applicationContext

        scope.launch {
            try {
                val settings = AppSettingsRepository(appContext).settingsFlow.first()
                if (settings.isEnabled) {
                    BedtimePromptNotifier.show(appContext)
                } else {
                    Log.d(TAG, "Night vibes disabled — prompt suppressed")
                }
                NightVibeScheduler.scheduleNext(appContext)
            } catch (e: Exception) {
                Log.e(TAG, "Bedtime prompt failed: ${e.message}", e)
                runCatching { NightVibeScheduler.scheduleNext(appContext) }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
