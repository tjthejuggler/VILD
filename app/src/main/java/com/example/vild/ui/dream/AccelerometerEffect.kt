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
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Smoothed device tilt in both axes, normalized to roughly [-1, 1], plus a
 * shake envelope in [0, 1]. Used for the dream background: tilt steers the
 * drifting starfield, shake shatters it.
 */
data class TiltState(
    val x: Float = 0f,
    val y: Float = 0f,
    val shake: Float = 0f,
)

/** How much of the previous shake envelope survives each sensor sample. */
private const val SHAKE_DECAY = 0.90f

/** How strongly a gravity-deviation spike drives the shake envelope. */
private const val SHAKE_GAIN = 2.5f

/**
 * Registers an accelerometer listener for the lifetime of the call site and
 * returns a low-pass-filtered [TiltState]. Falls back to zeroes on devices
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
                    val gx = event.values[0]
                    val gy = event.values[1]
                    val gz = event.values[2]

                    // Screen-space tilt, normalized by gravity, clamped, then
                    // low-pass filtered so the parallax glides like a dream.
                    // X is inverted: dipping the LEFT edge reads positive on
                    // the sensor, but downhill must be negative-x so the
                    // stars slide toward the low side (matching Y, which is
                    // already correct).
                    val rawX = (-gx / SensorManager.GRAVITY_EARTH).coerceIn(-1f, 1f)
                    val rawY = (gy / SensorManager.GRAVITY_EARTH).coerceIn(-1f, 1f)

                    // Shake = how far total acceleration strays from pure
                    // gravity. Pure tilt only rotates the gravity vector
                    // without changing its length, so gentle leans barely
                    // register — only real jolts (rapid shaking, bumps) push
                    // this up. Fast attack, slow decay.
                    val impulse = abs(
                        sqrt(gx * gx + gy * gy + gz * gz) - SensorManager.GRAVITY_EARTH,
                    ) / SensorManager.GRAVITY_EARTH
                    val envelope = max(
                        tilt.shake * SHAKE_DECAY,
                        (impulse * SHAKE_GAIN).coerceAtMost(1f),
                    )

                    tilt = TiltState(
                        x = tilt.x * 0.92f + rawX * 0.08f,
                        y = tilt.y * 0.92f + rawY * 0.08f,
                        shake = envelope,
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
