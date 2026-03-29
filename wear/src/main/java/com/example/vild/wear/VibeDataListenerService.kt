package com.example.vild.wear

import android.util.Log
import com.example.vild.shared.VibeConstants
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "VibeDataListenerSvc"

/**
 * Listens for:
 * - [VibeConstants.PATH_VIBE_SETTINGS] data changes pushed from the mobile app,
 *   persists the new settings locally, and triggers [VibeScheduler].
 * - [VibeConstants.PATH_VIBRATE_NOW] messages for immediate one-shot vibration
 *   via [VibrationHelper].
 */
class VibeDataListenerService : WearableListenerService() {

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        Log.d(TAG, "onDataChanged: received ${dataEvents.count} event(s)")
        dataEvents.forEach { event ->
            Log.d(TAG, "onDataChanged: event type=${event.type}, path=${event.dataItem.uri.path}, uri=${event.dataItem.uri}")
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
                    vibrationDurationMs = dataMap.getLong(VibeConstants.KEY_VIBRATION_DURATION_MS, 500L),
                    vibrationPatternType = dataMap.getString(
                        VibeConstants.KEY_VIBRATION_PATTERN_TYPE,
                        "single",
                    ),
                    vibrationRepeatCount = dataMap.getInt(VibeConstants.KEY_VIBRATION_REPEAT_COUNT, 1),
                )

                CoroutineScope(Dispatchers.IO).launch {
                    VibeScheduler.schedule(this@VibeDataListenerService)
                }
            }
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        Log.d(TAG, "onMessageReceived: path='${messageEvent.path}', sourceNodeId='${messageEvent.sourceNodeId}', dataSize=${messageEvent.data?.size ?: 0}")
        if (messageEvent.path == VibeConstants.PATH_VIBRATE_NOW) {
            Log.d(TAG, "onMessageReceived: PATH MATCHED — calling VibrationHelper.vibrate()")
            VibrationHelper.vibrate(this)
            Log.d(TAG, "onMessageReceived: VibrationHelper.vibrate() returned")
        } else {
            Log.w(TAG, "onMessageReceived: UNKNOWN path '${messageEvent.path}', expected '${VibeConstants.PATH_VIBRATE_NOW}'")
        }
    }
}
