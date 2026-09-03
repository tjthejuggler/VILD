package com.example.vild.ipc

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.vild.data.DailyTriggerScheduler
import com.example.vild.data.NagScheduler
import com.example.vild.data.NotificationHelper
import com.example.vild.data.RealityCheckRepository
import com.example.vild.data.RealityCheckStatsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val TAG = "DailyTriggerReceiver"

/**
 * Fired at 8 AM each day by [DailyTriggerScheduler].
 * Chooses (or reuses) today's reality check log, shows the notification,
 * arms the nag cycle, and reschedules the alarm for the next day.
 */
class DailyTriggerReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != DailyTriggerScheduler.ACTION_DAILY_TRIGGER &&
            intent.action != Intent.ACTION_BOOT_COMPLETED
        ) return

        Log.d(TAG, "Received alarm — preparing today's reality check")

        val pendingResult = goAsync()
        val appContext = context.applicationContext

        scope.launch {
            try {
                val triggerRepo = RealityCheckRepository(appContext)
                val statsRepo = RealityCheckStatsRepository(appContext)

                val triggers = triggerRepo.allTriggersFlow.first()
                val todayLog = statsRepo.ensureTodayLog(triggers)

                Log.d(TAG, "Today's reality check: ${todayLog.triggerText}")
                NotificationHelper.showNotification(appContext, todayLog)

                // Keep bothering (adaptively) until the daily goals are met.
                NagScheduler.nextIntervalMs(todayLog)?.let {
                    NagScheduler.schedule(appContext, it)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to show daily trigger: ${e.message}", e)
            } finally {
                // Always reschedule for tomorrow.
                DailyTriggerScheduler.schedule(appContext)
                pendingResult.finish()
            }
        }
    }
}
