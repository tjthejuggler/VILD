package com.example.vild.ipc

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.vild.data.BedtimePromptNotifier
import com.example.vild.data.NightVibeScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private const val TAG = "BedtimeActionReceiver"

/**
 * Handles the **Goodnight** action on the bedtime prompt notification:
 * stamps the bedtime anchor (start of tonight's schedule), dismisses the
 * prompt, and re-arms the vibe chain relative to the new anchor.
 */
class BedtimeActionReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != BedtimePromptNotifier.ACTION_GOODNIGHT) return

        Log.d(TAG, "Goodnight tapped — anchoring tonight's schedule")
        val pendingResult = goAsync()
        val appContext = context.applicationContext

        scope.launch {
            try {
                NightVibeScheduler.confirmBedtime(appContext)
                // Dismiss the prompt — bedtime is confirmed.
                (appContext.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager)
                    .cancel(BedtimePromptNotifier.NOTIFICATION_ID)
            } catch (e: Exception) {
                Log.e(TAG, "Goodnight failed: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
