package com.example.vild.ui.dream

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/**
 * Smoothed device tilt in both axes, normalized to roughly [-1, 1].
 * Used for subtle parallax: background layers shift by `tilt * depth`,
 * making the dream world feel suspended in space as the phone moves.
 */
data class TiltState(
    val x: Float = 0f,
    val y: Float = 0f,
)

/**
 * Registers an accelerometer listener for the lifetime of the call site and
 * returns a low-pass-filtered [TiltState]. Falls back to (0,0) on devices
 * without an accelerometer.
 */
@Composable
fun rememberTiltState(): TiltState {
    val context = LocalContext.current
    var tilt by remember { mutableStateOf(TiltState()) }

    DisposableEffect(context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (sensorManager == null || accelerometer == null) {
            onDispose { }
        } else {
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    // Normalize by gravity, clamp, then low-pass filter so the
                    // parallax glides like a dream instead of jittering.
                    val rawX = (event.values[0] / SensorManager.GRAVITY_EARTH).coerceIn(-1f, 1f)
                    val rawY = (event.values[1] / SensorManager.GRAVITY_EARTH).coerceIn(-1f, 1f)
                    tilt = TiltState(
                        x = tilt.x * 0.92f + rawX * 0.08f,
                        y = tilt.y * 0.92f + rawY * 0.08f,
                    )
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }
            sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
            onDispose {
                sensorManager.unregisterListener(listener)
            }
        }
    }

    return tilt
}
