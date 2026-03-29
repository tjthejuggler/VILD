package com.example.vild.wear

import com.example.vild.shared.VibeConstants
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Listens for [VibeConstants.PATH_VIBE_SETTINGS] data changes pushed from the mobile app,
 * persists the new settings locally, and triggers [VibeScheduler].
 */
class VibeDataListenerService : WearableListenerService() {

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.forEach { event ->
            if (event.type == DataEvent.TYPE_CHANGED &&
                event.dataItem.uri.path == VibeConstants.PATH_VIBE_SETTINGS
            ) {
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap

                VibeSettingsRepository.save(
                    context = this,
                    isEnabled = dataMap.getBoolean(VibeConstants.KEY_IS_ENABLED, false),
                    freqMinMinutes = dataMap.getInt(VibeConstants.KEY_FREQ_MIN_MINUTES, 30),
                    freqMaxMinutes = dataMap.getInt(VibeConstants.KEY_FREQ_MAX_MINUTES, 60),
                    vibrationIntensity = dataMap.getInt(VibeConstants.KEY_VIBRATION_INTENSITY, 128),
                    snoozeUntilTimestamp = dataMap.getLong(VibeConstants.KEY_SNOOZE_UNTIL_TIMESTAMP, 0L),
                    targetNodeId = dataMap.getString(
                        VibeConstants.KEY_TARGET_NODE_ID,
                        VibeConstants.VALUE_TARGET_NODE_ALL,
                    ),
                )

                CoroutineScope(Dispatchers.IO).launch {
                    VibeScheduler.schedule(this@VibeDataListenerService)
                }
            }
        }
    }
}
