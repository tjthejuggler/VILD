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

                when {
                    today == null -> {
                        Log.d(TAG, "No log for today — nag cycle ends")
                        NagScheduler.cancel(appContext)
                    }
                    today.isComplete -> {
                        Log.d(TAG, "Today's check is complete — nag cycle ends")
                        NagScheduler.cancel(appContext)
                    }
                    else -> {
                        Log.d(TAG, "Check still unconfirmed — re-posting notification")
                        NotificationHelper.showNotification(appContext, today)
                        NagScheduler.schedule(appContext)
                    }
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
