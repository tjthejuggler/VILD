package com.example.vild.ipc

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.vild.data.NagScheduler
import com.example.vild.data.NotificationHelper
import com.example.vild.data.RealityCheckStatsRepository
import com.example.vild.data.TailIntegrationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private const val TAG = "RealityCheckActionReceiver"

/**
 * Handles the action buttons on the reality check notification:
 * "✓ Read" and "✓ Done" — both repeatable, every tap logs another round.
 *
 * Each successful action also increments the matching habit in the Tail app.
 * The notification itself is never cancelled: it lives all day as the home
 * of the repeatable "I read it" / "I did it" actions; only the nagging stops
 * once the day is complete.
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
                val tail = TailIntegrationRepository(appContext)
                val log = if (action == ACTION_MARK_READ) {
                    // Repeatable by design: every tap bumps readCount and sends
                    // another increment to Tail.
                    repo.markReadToday()?.also {
                        tail.sendHabitIncrement(TailIntegrationRepository.Slot.READ)
                    }
                } else {
                    // Repeatable by design: every tap bumps doneCount and sends
                    // another increment to Tail.
                    repo.markDoneToday()?.also {
                        tail.sendHabitIncrement(TailIntegrationRepository.Slot.DONE)
                    }
                }

                if (log == null) {
                    Log.d(TAG, "Nothing to mark (no log for today yet)")
                } else {
                    // Refresh the notification (it stays all day) and stop the
                    // nagging once the day is complete.
                    NotificationHelper.showNotification(appContext, log)
                    if (log.isComplete) {
                        Log.d(TAG, "Check complete — disarming nag (notification stays)")
                        NagScheduler.cancel(appContext)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to mark: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
