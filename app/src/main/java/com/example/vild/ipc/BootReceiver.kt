package com.example.vild.ipc

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.vild.data.DailyTriggerScheduler

private const val TAG = "BootReceiver"

/**
 * Reschedules the daily reality check alarm after a device reboot.
 * Registered in the manifest for [Intent.ACTION_BOOT_COMPLETED].
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        Log.d(TAG, "Boot completed — rescheduling daily trigger alarm")
        DailyTriggerScheduler.schedule(context.applicationContext)
    }
}
