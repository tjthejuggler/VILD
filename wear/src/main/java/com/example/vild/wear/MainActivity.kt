package com.example.vild.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text

/**
 * Launcher activity for the Wear OS module.
 *
 * Displays a persistent status screen so the OS does not kill the app immediately,
 * which is required for [VibeDataListenerService] to reliably receive Data Layer events.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VildWearApp()
        }
    }
}

@Composable
private fun VildWearApp() {
    val context = LocalContext.current
    MaterialTheme {
        Scaffold {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "VILD is active.\nConfigure settings from your phone.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.body2,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { VibrationHelper.vibrate(context) }) {
                    Text(text = "Test Vibration")
                }
            }
        }
    }
}
