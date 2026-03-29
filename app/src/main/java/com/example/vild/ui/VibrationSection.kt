package com.example.vild.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.vild.MainViewModel
import com.example.vild.data.VibeSettings

private val PATTERN_OPTIONS = listOf("single", "double", "triple", "ramp")

/**
 * Vibration settings section: intensity, duration, pattern type, repeat count,
 * and the "Vibrate Now" test button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VibrationSection(settings: VibeSettings, vm: MainViewModel) {
    var patternExpanded by remember { mutableStateOf(false) }

    // ── Intensity ────────────────────────────────────────────────────────────
    Text(
        "Intensity: ${settings.vibrationIntensity}",
        style = MaterialTheme.typography.bodyMedium,
    )
    Slider(
        value = settings.vibrationIntensity.toFloat(),
        onValueChange = { vm.updateIntensity(it.toInt()) },
        valueRange = 1f..255f,
        steps = 253,
        modifier = Modifier.fillMaxWidth(),
    )

    // ── Duration ─────────────────────────────────────────────────────────────
    Text(
        "Duration: ${settings.vibrationDurationMs} ms",
        style = MaterialTheme.typography.bodyMedium,
    )
    Slider(
        value = settings.vibrationDurationMs.toFloat(),
        onValueChange = { vm.updateVibrationDurationMs(it.toLong()) },
        valueRange = 100f..4000f,
        steps = 77, // 50 ms steps: (4000-100)/50 - 1 = 77
        modifier = Modifier.fillMaxWidth(),
    )

    // ── Pattern type ─────────────────────────────────────────────────────────
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Pattern type", style = MaterialTheme.typography.bodyMedium)
        ExposedDropdownMenuBox(
            expanded = patternExpanded,
            onExpandedChange = { patternExpanded = !patternExpanded },
            modifier = Modifier.fillMaxWidth(),
        ) {
            OutlinedTextField(
                value = settings.vibrationPatternType,
                onValueChange = {},
                readOnly = true,
                label = { Text("Pattern") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(patternExpanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
            )
            ExposedDropdownMenu(
                expanded = patternExpanded,
                onDismissRequest = { patternExpanded = false },
            ) {
                PATTERN_OPTIONS.forEach { pattern ->
                    DropdownMenuItem(
                        text = { Text(pattern) },
                        onClick = {
                            vm.updateVibrationPatternType(pattern)
                            patternExpanded = false
                        },
                    )
                }
            }
        }
    }

    // ── Repeat count ─────────────────────────────────────────────────────────
    Text(
        "Repeat count: ${settings.vibrationRepeatCount}",
        style = MaterialTheme.typography.bodyMedium,
    )
    Slider(
        value = settings.vibrationRepeatCount.toFloat(),
        onValueChange = { vm.updateVibrationRepeatCount(it.toInt()) },
        valueRange = 1f..5f,
        steps = 3,
        modifier = Modifier.fillMaxWidth(),
    )

    // ── Vibrate Now button ───────────────────────────────────────────────────
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(onClick = { vm.vibrateNow() }) {
            Text("Vibrate Now")
        }
    }
}
