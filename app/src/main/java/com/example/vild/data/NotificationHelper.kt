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
import com.example.vild.ipc.RealityCheckActionReceiver
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Builds and posts the daily reality check notification.
 *
 * While the day's check is unconfirmed the notification is **ongoing** (it
 * cannot be swiped away) and carries direct "✓ Read" / "✓ Done" actions.
 * [NagReceiver] re-posts it every 30 minutes until both are confirmed —
 * the app keeps forcefully bothering the user, as designed.
 */
object NotificationHelper {

    const val CHANNEL_ID = "vild_reality_check"
    const val NOTIFICATION_ID = 2001

    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")

    /** Creates the notification channel (idempotent — safe to call multiple times). */
    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(CHANNEL_ID) != null) return

            val channel = NotificationChannel(
                CHANNEL_ID,
                "Daily Reality Check",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Morning reality check — insists until you read AND do it"
                setShowBadge(true)
            }
            manager.createNotificationChannel(channel)
        }
    }

    /** Removes the reality check notification (once the day's check is complete). */
    fun cancelNotification(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(NOTIFICATION_ID)
    }

    /**
     * Shows (or refreshes) the notification for [log].
     * Title/status reflect what is still missing; actions let the user confirm
     * directly from the notification.
     */
    fun showNotification(context: Context, log: RealityCheckDayLog) {
        ensureChannel(context)

        val read = log.readAt != null
        val done = log.doneAt != null

        val title = when {
            read && !done -> "Read ✓ — now DO the check"
            done && !read -> "Done ✓ — mark it as read"
            else -> "Today's Reality Check"
        }
        val statusLine = buildString {
            if (read) append("✓ read ${formatTime(log.readAt)}")
            if (read && !done) append("  ·  ")
            if (done) append("✓ done ${formatTime(log.doneAt)}")
            if (!read && !done) append("waiting for you…")
        }

        // Tap → open the app on the reality check screen.
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        fun actionIntent(action: String, requestCode: Int) = PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, RealityCheckActionReceiver::class.java).apply { this.action = action },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.vild_icon)
            .setContentTitle(title)
            .setContentText(log.triggerText)
            .setStyle(NotificationCompat.BigTextStyle().bigText("${log.triggerText}\n$statusLine"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentIntent)
            .setAutoCancel(false)
            // Ongoing = cannot be dismissed until the check is confirmed.
            .setOngoing(!(read && done))

        if (!read) {
            builder.addAction(0, "✓ I read it", actionIntent(RealityCheckActionReceiver.ACTION_MARK_READ, 1))
        }
        if (!done) {
            builder.addAction(0, "✓ I did it", actionIntent(RealityCheckActionReceiver.ACTION_MARK_DONE, 2))
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, builder.build())
    }

    private fun formatTime(epochMs: Long?): String =
        epochMs?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).format(timeFormat) } ?: ""
}
