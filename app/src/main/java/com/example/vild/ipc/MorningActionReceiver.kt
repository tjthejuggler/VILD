package com.example.vild.ipc

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.vild.data.DayModeSwitcher
import com.example.vild.data.MorningPromptNotifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private const val TAG = "MorningActionReceiver"

/**
 * Handles the **Good morning** action button on the morning prompt: runs the
 * shared wake-up sequence ([DayModeSwitcher.forceDayMode]) — anchor cleared,
 * vibe chain paused until the next Goodnight, night → day switch — and
 * dismisses the prompt itself.
 */
class MorningActionReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != MorningPromptNotifier.ACTION_GOOD_MORNING) return

        Log.d(TAG, "Good morning tapped — running wake-up sequence")
        val pendingResult = goAsync()
        val appContext = context.applicationContext

        scope.launch {
            try {
                // forceDayMode also cancels the morning prompt itself.
                DayModeSwitcher.forceDayMode(appContext)
            } catch (e: Exception) {
                Log.e(TAG, "Good morning failed: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
