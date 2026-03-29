package com.example.vild.wear

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Centralised vibration logic for the watch.
 *
 * Reads pattern type, duration, intensity, and repeat count from
 * [VibeSettingsRepository] and builds the appropriate [VibrationEffect].
 * Used by both [VibeReceiver] (scheduled reminders) and
 * [VibeDataListenerService] (immediate vibrate-now command).
 */
object VibrationHelper {

    /**
     * Fires a vibration using the current settings stored in [VibeSettingsRepository].
     */
    fun vibrate(context: Context) {
        val intensity = VibeSettingsRepository.vibrationIntensity(context)
        val durationMs = VibeSettingsRepository.vibrationDurationMs(context)
        val patternType = VibeSettingsRepository.vibrationPatternType(context)
        val repeatCount = VibeSettingsRepository.vibrationRepeatCount(context)

        val effect = buildEffect(patternType, durationMs, intensity, repeatCount)
        getVibrator(context).vibrate(effect)
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun buildEffect(
        patternType: String,
        durationMs: Long,
        intensity: Int,
        repeatCount: Int,
    ): VibrationEffect {
        val gap = (durationMs / 2).coerceAtLeast(50L)

        return when (patternType) {
            "double" -> {
                // Two pulses separated by a gap, repeated [repeatCount] times
                val timings = buildPulseTimings(pulses = 2, durationMs = durationMs, gap = gap, repeatCount = repeatCount)
                val amplitudes = buildPulseAmplitudes(pulses = 2, intensity = intensity, repeatCount = repeatCount)
                VibrationEffect.createWaveform(timings, amplitudes, -1)
            }
            "triple" -> {
                val timings = buildPulseTimings(pulses = 3, durationMs = durationMs, gap = gap, repeatCount = repeatCount)
                val amplitudes = buildPulseAmplitudes(pulses = 3, intensity = intensity, repeatCount = repeatCount)
                VibrationEffect.createWaveform(timings, amplitudes, -1)
            }
            "ramp" -> {
                // Waveform that ramps up intensity across 4 steps
                val steps = 4
                val stepDuration = durationMs / steps
                val timings = LongArray(steps * repeatCount) { stepDuration }
                val amplitudes = IntArray(steps * repeatCount) { index ->
                    val step = index % steps
                    ((intensity * (step + 1)) / steps).coerceIn(1, 255)
                }
                VibrationEffect.createWaveform(timings, amplitudes, -1)
            }
            else -> {
                // "single" — one shot, repeated [repeatCount] times via waveform
                if (repeatCount <= 1) {
                    VibrationEffect.createOneShot(durationMs, intensity)
                } else {
                    val timings = buildPulseTimings(pulses = 1, durationMs = durationMs, gap = gap, repeatCount = repeatCount)
                    val amplitudes = buildPulseAmplitudes(pulses = 1, intensity = intensity, repeatCount = repeatCount)
                    VibrationEffect.createWaveform(timings, amplitudes, -1)
                }
            }
        }
    }

    /**
     * Builds a flat timings array for [pulses] pulses per repetition, repeated [repeatCount] times.
     * Pattern per repetition: [on, off, on, off, …, on, gap-after-last-set]
     */
    private fun buildPulseTimings(pulses: Int, durationMs: Long, gap: Long, repeatCount: Int): LongArray {
        // Each repetition: pulses × (on + off), last off replaced by a longer gap
        val perRep = mutableListOf<Long>()
        repeat(pulses) { i ->
            perRep.add(durationMs)
            perRep.add(if (i < pulses - 1) gap else gap * 2)
        }
        val result = LongArray(perRep.size * repeatCount)
        repeat(repeatCount) { r -> perRep.forEachIndexed { i, v -> result[r * perRep.size + i] = v } }
        return result
    }

    /**
     * Builds a flat amplitudes array matching [buildPulseTimings]:
     * on-slots get [intensity], off-slots get 0.
     */
    private fun buildPulseAmplitudes(pulses: Int, intensity: Int, repeatCount: Int): IntArray {
        val perRep = mutableListOf<Int>()
        repeat(pulses) {
            perRep.add(intensity)
            perRep.add(0)
        }
        val result = IntArray(perRep.size * repeatCount)
        repeat(repeatCount) { r -> perRep.forEachIndexed { i, v -> result[r * perRep.size + i] = v } }
        return result
    }

    private fun getVibrator(context: Context): Vibrator =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
}
