package com.example.vild.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.vild.ipc.DailyTriggerReceiver
import java.util.Calendar

/**
 * Schedules the daily 8 AM alarm that triggers the reality check notification.
 * Uses [AlarmManager.setAlarmClock] for Doze-exempt exact delivery.
 */
object DailyTriggerScheduler {

    private const val TAG = "DailyTriggerScheduler"
    const val ACTION_DAILY_TRIGGER = "com.example.vild.ACTION_DAILY_TRIGGER"

    /** Schedules the next 8 AM alarm. If 8 AM today has already passed, schedules for tomorrow. */
    fun schedule(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, DailyTriggerReceiver::class.java).apply {
            action = ACTION_DAILY_TRIGGER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If 8 AM today has already passed, schedule for tomorrow
        if (target.before(now)) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }

        val alarmClockInfo = AlarmManager.AlarmClockInfo(target.timeInMillis, pendingIntent)
        alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)

        Log.d(TAG, "Scheduled daily trigger alarm for: ${target.time}")
    }

    /** Cancels the daily trigger alarm. */
    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, DailyTriggerReceiver::class.java).apply {
            action = ACTION_DAILY_TRIGGER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
        pendingIntent?.let { alarmManager.cancel(it) }
        Log.d(TAG, "Cancelled daily trigger alarm")
    }
}
