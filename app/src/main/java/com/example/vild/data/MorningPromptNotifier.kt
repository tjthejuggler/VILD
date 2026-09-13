package com.example.vild.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.vild.MainActivity
import com.example.vild.R
import com.example.vild.ipc.MorningActionReceiver

/**
 * Builds and posts the daily "Good morning" prompt.
 *
 * Fired at the night-end time — but only while last night's cycle is still
 * live (no wake-up signal received yet). Its **Good morning** action runs the
 * shared wake-up sequence ([DayModeSwitcher.forceDayMode]): anchor cleared,
 * vibe chain paused, night → day switch. The notification self-dismisses
 * after a few hours.
 */
object MorningPromptNotifier {

    const val CHANNEL_ID = "vild_morning_prompt"
    const val NOTIFICATION_ID = 3003
    const val ACTION_GOOD_MORNING = "com.example.vild.ACTION_GOOD_MORNING"

    /** How long the prompt lingers before self-dismissing. */
    private const val AUTO_DISMISS_MS = 4 * 60 * 60_000L

    /** Creates the notification channel (idempotent — safe to call multiple times). */
    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(CHANNEL_ID) != null) return

            val channel = NotificationChannel(
                CHANNEL_ID,
                "Morning Prompt",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Daily \"Good morning\" — tap to tell VILD you are awake"
                setShowBadge(false)
            }
            manager.createNotificationChannel(channel)
        }
    }

    /** Posts the morning prompt with the Good-morning action button. */
    fun show(context: Context) {
        ensureChannel(context)

        val contentIntent = PendingIntent.getActivity(
            context,
            3,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val goodMorningIntent = PendingIntent.getBroadcast(
            context,
            4,
            Intent(context, MorningActionReceiver::class.java).apply { action = ACTION_GOOD_MORNING },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.vild_icon)
            .setContentTitle("☀ Good morning")
            .setContentText("Tap Good morning — tonight's dream vibes will wait for your next Goodnight.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(contentIntent)
            .addAction(0, "☀ Good morning", goodMorningIntent)
            .setAutoCancel(true)
            .setTimeoutAfter(AUTO_DISMISS_MS)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, builder.build())
    }

    /** Cancels a live morning prompt (e.g. on any wake-up signal). */
    fun cancel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(NOTIFICATION_ID)
    }
}
