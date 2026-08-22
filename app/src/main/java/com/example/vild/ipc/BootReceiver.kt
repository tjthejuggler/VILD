package com.example.vild.ipc

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.vild.data.DailyTriggerScheduler
import com.example.vild.data.NagScheduler
import com.example.vild.data.RealityCheckStatsRepository
import com.example.vild.data.todayEpochDay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val TAG = "BootReceiver"

/**
 * Reschedules the daily reality check alarm after a device reboot, and
 * re-arms the nag cycle if today's check is still unconfirmed.
 * Registered in the manifest for [Intent.ACTION_BOOT_COMPLETED].
 */
class BootReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        Log.d(TAG, "Boot completed — rescheduling alarms")
        DailyTriggerScheduler.schedule(context.applicationContext)

        val pendingResult = goAsync()
        val appContext = context.applicationContext

        scope.launch {
            try {
                val repo = RealityCheckStatsRepository(appContext)
                val today = repo.allLogsFlow.first().firstOrNull { it.epochDay == todayEpochDay() }
                if (today != null && !today.isComplete) {
                    Log.d(TAG, "Today's check still unconfirmed — re-arming nag")
                    NagScheduler.schedule(appContext)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Boot nag re-arm failed: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
