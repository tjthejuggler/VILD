package com.example.vild.ipc

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.vild.data.NagScheduler
import com.example.vild.data.NotificationHelper
import com.example.vild.data.RealityCheckDayLog
import com.example.vild.data.RealityCheckRepository
import com.example.vild.data.RealityCheckStatsRepository
import com.example.vild.data.TailIntegrationRepository
import com.example.vild.data.TailIntegrationRepository.Slot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val TAG = "TailHabitSyncReceiver"

/**
 * Reverse half of the Tail integration: when the user increments one of the
 * habits VILD has mapped — inside Tail itself (main UI, home-screen widget,
 * habit-ask notification answer or voice) — Tail broadcasts
 * ACTION_HABIT_INCREMENTED and this receiver applies the increment to VILD's
 * own day log:
 *
 *  - READ slot habit  → adds [TailIntegrationRepository.EXTRA_AMOUNT] "I read it" rounds
 *  - DONE slot habit  → adds [TailIntegrationRepository.EXTRA_AMOUNT] "I did it" rounds
 *
 * Loop safety: VILD tags its own outgoing increments with EXTRA_SOURCE; Tail
 * propagates that extra on the announcement broadcast, and echoes
 * (source == our own package) are ignored here — so a VILD tap never counts
 * twice and the two apps can never ping-pong increments back and forth.
 *
 * Works even when VILD's UI is not open because manifest-registered receivers
 * are woken by the system. Never sends anything back to Tail.
 */
class TailHabitSyncReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TailIntegrationRepository.ACTION_HABIT_INCREMENTED) return

        val habitName = intent.getStringExtra(TailIntegrationRepository.EXTRA_HABIT_NAME)
            ?.takeIf { it.isNotBlank() } ?: return
        val amount = intent.getIntExtra(TailIntegrationRepository.EXTRA_AMOUNT, 1).coerceAtLeast(0)
        val source = intent.getStringExtra(TailIntegrationRepository.EXTRA_SOURCE)
        val appContext = context.applicationContext

        // Echo suppression — this announcement is Tail confirming an increment
        // that VILD itself sent; VILD has already counted it locally.
        if (source == appContext.packageName) {
            Log.d(TAG, "Ignoring echo of own increment for '$habitName'")
            return
        }

        // amount 0 = max-1 no-op or minutes-only adjustment in Tail — not a count.
        if (amount < 1) {
            Log.d(TAG, "Ignoring zero-count increment announcement for '$habitName'")
            return
        }

        Log.d(TAG, "Tail incremented '$habitName' by $amount (source=${source ?: "tail-local"})")

        val pendingResult = goAsync()
        scope.launch {
            try {
                val tailRepo = TailIntegrationRepository(appContext)
                val readHabit = tailRepo.getHabitName(Slot.READ)
                val doneHabit = tailRepo.getHabitName(Slot.DONE)
                val isRead = readHabit.isNotBlank() && habitName == readHabit
                val isDone = doneHabit.isNotBlank() && habitName == doneHabit

                if (!isRead && !isDone) {
                    Log.d(TAG, "Habit '$habitName' is not mapped to a VILD slot — ignoring")
                    return@launch
                }

                val statsRepo = RealityCheckStatsRepository(appContext)
                // The day log must exist before it can be updated; create it with
                // a random trigger exactly like the app-open path does.
                val triggers = RealityCheckRepository(appContext).allTriggersFlow.first()
                statsRepo.ensureTodayLog(triggers)

                var log: RealityCheckDayLog? = null
                if (isRead) {
                    repeat(amount) {
                        statsRepo.markReadToday()?.let { log = it }
                    }
                    Log.i(TAG, "Added $amount read round(s) (via Tail '$habitName')")
                }
                if (isDone) {
                    repeat(amount) {
                        statsRepo.markDoneToday()?.let { log = it }
                    }
                    Log.i(TAG, "Added $amount done round(s) (via Tail '$habitName')")
                }

                // Keep the notification in step with the new state, mirroring
                // MainViewModel.markToday — stop the nagging once complete.
                log?.let {
                    NotificationHelper.showNotification(appContext, it)
                    if (it.isComplete) NagScheduler.cancel(appContext)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to apply Tail increment for '$habitName': ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
