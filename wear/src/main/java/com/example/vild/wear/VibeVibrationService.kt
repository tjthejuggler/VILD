package com.example.vild.wear

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log

private const val TAG = "VibeVibrationService"
private const val CHANNEL_ID = "vild_vibration_channel"
private const val NOTIFICATION_ID = 1

/**
 * A short-lived foreground service whose sole purpose is to call
 * [VibrationHelper.vibrate] from a Service context.
 *
 * On Wear OS, vibrations initiated from a [android.content.BroadcastReceiver]
 * context can be silently suppressed by the system when the device is in
 * Doze or the screen is off. Running the vibration from a foreground service
 * ensures it is always delivered.
 *
 * The service stops itself after a short delay (enough for the vibration
 * pattern to complete).
 */
class VibeVibrationService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: starting foreground + vibrating")

        // Must call startForeground within 5 seconds of startForegroundService()
        val notification = buildNotification()
        startForeground(NOTIFICATION_ID, notification)

        // Vibrate from the Service context
        try {
            VibrationHelper.vibrate(this)
            Log.d(TAG, "onStartCommand: vibration triggered successfully")
        } catch (e: Exception) {
            Log.e(TAG, "onStartCommand: vibration FAILED", e)
        }

        // Stop the service after the vibration pattern has had time to play.
        // The longest possible pattern is ~8 seconds (triple pattern, 4s duration,
        // repeated 3 times). 15 seconds gives plenty of margin.
        Handler(Looper.getMainLooper()).postDelayed({
            Log.d(TAG, "stopping self after vibration delay")
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }, 15_000L)

        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Vibration Reminders",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Used while delivering a vibration reminder"
            setShowBadge(false)
        }
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification =
        Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("VILD")
            .setContentText("Vibrating…")
            .setOngoing(true)
            .build()
}
