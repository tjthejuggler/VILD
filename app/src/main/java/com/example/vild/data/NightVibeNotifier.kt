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

/**
 * Builds and posts the night-vibe notification.
 *
 * The notification is a transient pulse: it reaches the paired wearable
 * (e.g. a Garmin watch), which mirrors it and vibrates, then disappears on
 * its own — nothing accumulates in the morning. The send time is recorded
 * separately by [NightVibeLogRepository].
 */
object NightVibeNotifier {

    const val CHANNEL_ID = "vild_night_vibe"
    const val NOTIFICATION_ID = 3001

    /** How long the notification stays on the phone before self-dismissing. */
    private const val AUTO_DISMISS_MS = 60_000L

    /** Creates the notification channel (idempotent — safe to call multiple times). */
    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(CHANNEL_ID) != null) return

            val channel = NotificationChannel(
                CHANNEL_ID,
                "Night Vibes",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "REM-aimed night pulses — forwarded to your watch"
                setShowBadge(false)
            }
            manager.createNotificationChannel(channel)
        }
    }

    /** Posts the transient night-vibe notification. */
    fun show(context: Context) {
        ensureChannel(context)

        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.vild_icon)
            .setContentTitle("☾ Dream check")
            .setContentText("Are you dreaming? Look at your hands.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(contentIntent)
            // Transient: gone in a minute, nothing to see in the morning.
            .setAutoCancel(true)
            .setTimeoutAfter(AUTO_DISMISS_MS)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, builder.build())
    }
}
