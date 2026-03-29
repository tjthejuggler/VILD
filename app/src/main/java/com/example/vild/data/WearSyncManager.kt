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
 * - [sendVibrateNow] sends a fire-and-forget [VibeConstants.PATH_VIBRATE_NOW] message
 *   to a specific node via [com.google.android.gms.wearable.MessageClient].
 */
class WearSyncManager(private val context: Context) {

    /** Returns all currently connected (reachable) Wear OS nodes. */
    suspend fun getConnectedNodes(): List<Node> =
        try {
            Log.d(TAG, "getConnectedNodes: querying NodeClient…")
            val nodes = Wearable.getNodeClient(context).connectedNodes.await()
            if (nodes.isEmpty()) {
                Log.w(TAG, "getConnectedNodes: NO nodes found — is the watch paired, powered on, and nearby?")
            } else {
                nodes.forEach { node ->
                    Log.d(TAG, "getConnectedNodes: found node id=${node.id}, displayName=${node.displayName}, isNearby=${node.isNearby}")
                }
            }
            nodes
        } catch (e: Exception) {
            Log.e(TAG, "getConnectedNodes: FAILED to fetch connected nodes", e)
            emptyList()
        }

    /**
     * Pushes [settings] to the Data Layer path [VibeConstants.PATH_VIBE_SETTINGS].
     *
     * [setUrgent] is called so the item is delivered immediately rather than
     * being batched, which is important for real-time setting changes.
     *
     * @return `true` if the push succeeded, `false` if an exception was thrown.
     */
    suspend fun pushSettings(settings: VibeSettings): Boolean {
        return try {
            val request = PutDataMapRequest.create(VibeConstants.PATH_VIBE_SETTINGS).apply {
                dataMap.putBoolean(VibeConstants.KEY_IS_ENABLED, settings.isEnabled)
                dataMap.putInt(VibeConstants.KEY_FREQ_MIN_MINUTES, settings.freqMinMinutes)
                dataMap.putInt(VibeConstants.KEY_FREQ_MAX_MINUTES, settings.freqMaxMinutes)
                dataMap.putInt(VibeConstants.KEY_VIBRATION_INTENSITY, settings.vibrationIntensity)
                dataMap.putLong(VibeConstants.KEY_SNOOZE_UNTIL_TIMESTAMP, settings.snoozeUntilTimestamp)
                dataMap.putString(VibeConstants.KEY_TARGET_NODE_ID, settings.targetNodeId)
                dataMap.putLong(VibeConstants.KEY_VIBRATION_DURATION_MS, settings.vibrationDurationMs)
                dataMap.putString(VibeConstants.KEY_VIBRATION_PATTERN_TYPE, settings.vibrationPatternType)
                dataMap.putInt(VibeConstants.KEY_VIBRATION_REPEAT_COUNT, settings.vibrationRepeatCount)
            }.asPutDataRequest().setUrgent()

            Wearable.getDataClient(context).putDataItem(request).await()
            Log.d(TAG, "Settings pushed to Data Layer: $settings")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to push settings to Data Layer", e)
            false
        }
    }

    /**
     * Sends an immediate vibrate command to [nodeId] via [MessageClient].
     * The watch's [VibeDataListenerService] handles this in [onMessageReceived].
     */
    suspend fun sendVibrateNow(nodeId: String) {
        Log.d(TAG, "sendVibrateNow: STARTING — targetNodeId='$nodeId', path='${VibeConstants.PATH_VIBRATE_NOW}'")
        if (nodeId.isBlank()) {
            Log.e(TAG, "sendVibrateNow: ABORTED — nodeId is blank/empty! Check that a watch is selected.")
            return
        }
        try {
            val messageClient = Wearable.getMessageClient(context)
            Log.d(TAG, "sendVibrateNow: obtained MessageClient, calling sendMessage…")
            val requestId = messageClient
                .sendMessage(nodeId, VibeConstants.PATH_VIBRATE_NOW, null)
                .await()
            Log.d(TAG, "sendVibrateNow: SUCCESS — requestId=$requestId, nodeId='$nodeId'")
        } catch (e: Exception) {
            Log.e(TAG, "sendVibrateNow: FAILED — nodeId='$nodeId', exception: ${e.javaClass.simpleName}: ${e.message}", e)
        }
    }
}
