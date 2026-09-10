package com.example.vild.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.vild.MainActivity
import com.example.vild.R
import com.example.vild.ipc.BedtimeActionReceiver

/**
 * Builds and posts the nightly "Going to sleep?" prompt.
 *
 * The prompt carries a **Goodnight** action: tapping it stamps the bedtime
 * anchor that the entire night schedule (quiet gap + REM-targeted pulses) is
 * measured from. The notification also opens [MainActivity], where the main
 * screen offers the same Goodnight confirmation.
 */
object BedtimePromptNotifier {

    const val CHANNEL_ID = "vild_bedtime_prompt"
    const val NOTIFICATION_ID = 3002
    const val ACTION_GOODNIGHT = "com.example.vild.ACTION_GOODNIGHT"

    /** How long the prompt lingers before self-dismissing. */
    private const val AUTO_DISMISS_MS = 4 * 60 * 60_000L // stays up all evening

    /** Creates the notification channel (idempotent — safe to call multiple times). */
    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(CHANNEL_ID) != null) return

            val channel = NotificationChannel(
                CHANNEL_ID,
                "Bedtime Prompt",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Nightly \"Going to sleep?\" — tap Goodnight to start tonight's schedule"
                setShowBadge(false)
            }
            manager.createNotificationChannel(channel)
        }
    }

    /** Posts the bedtime prompt with the Goodnight action button. */
    fun show(context: Context) {
        ensureChannel(context)

        val contentIntent = PendingIntent.getActivity(
            context,
            1,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val goodnightIntent = PendingIntent.getBroadcast(
            context,
            2,
            Intent(context, BedtimeActionReceiver::class.java).apply { action = ACTION_GOODNIGHT },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.vild_icon)
            .setContentTitle("☾ Going to sleep?")
            .setContentText("Tap Goodnight and tonight's dream vibes will be timed from now.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(contentIntent)
            .addAction(0, "🌙 Goodnight", goodnightIntent)
            .setAutoCancel(true)
            .setTimeoutAfter(AUTO_DISMISS_MS)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, builder.build())
    }
}
