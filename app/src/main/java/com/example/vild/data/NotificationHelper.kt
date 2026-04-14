package com.example.vild.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.vild.R

/**
 * Helper for creating and posting the daily reality check notification.
 */
object NotificationHelper {

    const val CHANNEL_ID = "vild_reality_check"
    const val NOTIFICATION_ID = 2001

    /** Creates the notification channel (idempotent — safe to call multiple times). */
    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(CHANNEL_ID) != null) return

            val channel = NotificationChannel(
                CHANNEL_ID,
                "Daily Reality Check",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Morning reality check trigger notification"
                setShowBadge(true)
            }
            manager.createNotificationChannel(channel)
        }
    }

    /** Shows a notification with the given trigger text. */
    fun showNotification(context: Context, triggerText: String) {
        ensureChannel(context)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.vild_icon)
            .setContentTitle("Today's Reality Check")
            .setContentText(triggerText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(triggerText))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }
}
