package com.example.vild.data

import android.content.Context
import android.util.Log
import com.example.vild.shared.VibeConstants
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await

private const val TAG = "WearSyncManager"

/**
 * Handles all Wearable Data Layer communication from the phone side.
 *
 * - [getConnectedNodes] returns the list of currently reachable Wear OS nodes.
 * - [pushSettings] serialises a [VibeSettings] snapshot into a [PutDataMapRequest]
 *   and calls [com.google.android.gms.wearable.DataClient.putDataItem].
 *   The Data Layer automatically delivers the item to every paired node that is
 *   currently connected, and queues it for nodes that reconnect later — satisfying
 *   the "remember which watch doesn't have the latest settings" requirement.
 */
class WearSyncManager(private val context: Context) {

    /** Returns all currently connected (reachable) Wear OS nodes. */
    suspend fun getConnectedNodes(): List<Node> =
        try {
            Wearable.getNodeClient(context).connectedNodes.await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch connected nodes", e)
            emptyList()
        }

    /**
     * Pushes [settings] to the Data Layer path [VibeConstants.PATH_VIBE_SETTINGS].
     *
     * [setUrgent] is called so the item is delivered immediately rather than
     * being batched, which is important for real-time setting changes.
     */
    suspend fun pushSettings(settings: VibeSettings) {
        try {
            val request = PutDataMapRequest.create(VibeConstants.PATH_VIBE_SETTINGS).apply {
                dataMap.putBoolean(VibeConstants.KEY_IS_ENABLED, settings.isEnabled)
                dataMap.putInt(VibeConstants.KEY_FREQ_MIN_MINUTES, settings.freqMinMinutes)
                dataMap.putInt(VibeConstants.KEY_FREQ_MAX_MINUTES, settings.freqMaxMinutes)
                dataMap.putInt(VibeConstants.KEY_VIBRATION_INTENSITY, settings.vibrationIntensity)
                dataMap.putLong(VibeConstants.KEY_SNOOZE_UNTIL_TIMESTAMP, settings.snoozeUntilTimestamp)
                dataMap.putString(VibeConstants.KEY_TARGET_NODE_ID, settings.targetNodeId)
            }.asPutDataRequest().setUrgent()

            Wearable.getDataClient(context).putDataItem(request).await()
            Log.d(TAG, "Settings pushed to Data Layer: $settings")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to push settings to Data Layer", e)
        }
    }
}
