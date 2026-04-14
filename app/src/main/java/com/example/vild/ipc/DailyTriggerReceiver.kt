package com.example.vild.ipc

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.vild.data.DailyTriggerScheduler
import com.example.vild.data.NotificationHelper
import com.example.vild.data.RealityCheckRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val TAG = "DailyTriggerReceiver"

/**
 * Fired at 8 AM each day by [DailyTriggerScheduler].
 * Picks a random reality check trigger from the user's list and shows a notification,
 * then reschedules the alarm for the next day.
 */
class DailyTriggerReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != DailyTriggerScheduler.ACTION_DAILY_TRIGGER &&
            intent.action != Intent.ACTION_BOOT_COMPLETED
        ) return

        Log.d(TAG, "Received alarm — picking random trigger")

        val pendingResult = goAsync()
        val appContext = context.applicationContext

        scope.launch {
            try {
                val repo = RealityCheckRepository(appContext)
                val triggers = repo.allTriggersFlow.first()

                if (triggers.isEmpty()) {
                    Log.d(TAG, "No triggers configured — skipping notification")
                } else {
                    val trigger = triggers.random()
                    Log.d(TAG, "Selected trigger: ${trigger.text}")
                    NotificationHelper.showNotification(appContext, trigger.text)
                }

                // Always reschedule for tomorrow
                DailyTriggerScheduler.schedule(appContext)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to show daily trigger: ${e.message}", e)
                // Still try to reschedule
                DailyTriggerScheduler.schedule(appContext)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
