package com.example.vild.ipc

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.vild.data.NagScheduler
import com.example.vild.data.NotificationHelper
import com.example.vild.data.RealityCheckStatsRepository
import com.example.vild.data.todayEpochDay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val TAG = "NagReceiver"

/**
 * Fired by [NagScheduler] every 30 minutes while today's reality check is
 * still unconfirmed. Re-posts the (ongoing) notification so it keeps
 * bothering the user, then re-arms itself. If the check is complete — or the
 * day rolled over with no log — the nag cycle stops.
 */
class NagReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != NagScheduler.ACTION_NAG) return

        Log.d(TAG, "Nag fired — checking today's reality check state")

        val pendingResult = goAsync()
        val appContext = context.applicationContext

        scope.launch {
            try {
                val repo = RealityCheckStatsRepository(appContext)
                val today = repo.allLogsFlow.first().firstOrNull { it.epochDay == todayEpochDay() }

                val interval = when {
                    today == null -> null.also {
                        Log.d(TAG, "No log for today — nag cycle ends")
                    }
                    else -> NagScheduler.nextIntervalMs(today!!).also {
                        if (it == null) Log.d(TAG, "Daily goals met or day over — nag cycle ends")
                    }
                }

                if (interval == null || today == null) {
                    NagScheduler.cancel(appContext)
                } else {
                    Log.d(TAG, "Goals unmet — re-posting notification, next nag in ${interval / 60000} min")
                    NotificationHelper.showNotification(appContext, today)
                    NagScheduler.schedule(appContext, interval)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Nag failed: ${e.message}", e)
                NagScheduler.schedule(appContext)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
