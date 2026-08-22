package com.example.vild.ipc

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.vild.data.NagScheduler
import com.example.vild.data.NotificationHelper
import com.example.vild.data.RealityCheckStatsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private const val TAG = "RealityCheckActionReceiver"

/**
 * Handles the action buttons on the reality check notification:
 * "✓ Read" and "✓ Done". Updates today's day log, refreshes or clears the
 * notification, and stops the nag cycle once the check is fully confirmed.
 */
class RealityCheckActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_MARK_READ = "com.example.vild.ACTION_MARK_READ"
        const val ACTION_MARK_DONE = "com.example.vild.ACTION_MARK_DONE"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != ACTION_MARK_READ && action != ACTION_MARK_DONE) return

        Log.d(TAG, "Notification action received: $action")

        val pendingResult = goAsync()
        val appContext = context.applicationContext

        scope.launch {
            try {
                val repo = RealityCheckStatsRepository(appContext)
                val log = if (action == ACTION_MARK_READ) repo.markReadToday() else repo.markDoneToday()

                if (log == null) {
                    Log.w(TAG, "No log for today — nothing to mark")
                } else if (log.isComplete) {
                    Log.d(TAG, "Check complete — clearing notification, disarming nag")
                    NotificationHelper.cancelNotification(appContext)
                    NagScheduler.cancel(appContext)
                } else {
                    // Still half-done: refresh the notification to reflect new state.
                    NotificationHelper.showNotification(appContext, log)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to mark: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
