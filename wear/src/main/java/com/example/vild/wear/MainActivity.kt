package com.example.vild.wear

import android.app.Activity
import android.os.Bundle

/**
 * Minimal launcher activity for the Wear OS module.
 * Required so Android Studio recognises this as a runnable application module.
 * The actual functionality is driven by [VibeDataListenerService] and [VibeScheduler].
 */
class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // No UI — this app runs entirely in the background.
        finish()
    }
}
