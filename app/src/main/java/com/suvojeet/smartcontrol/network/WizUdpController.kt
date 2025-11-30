package com.suvojeet.smartcontrol.network

import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WiZ UDP Controller - Now with type-safe data classes and Connection Pooling! 🛡️
 */
@Singleton
class WizUdpController @Inject constructor(
    private val socketManager: WizSocketManager
) {
    companion object {
        private const val TAG = "WizController"
    }
    
    private val gson = Gson()

    /**
     * Send a command to a WiZ bulb using type-safe data classes
     */
    suspend fun sendCommand(ip: String, params: Map<String, Any>) {
        withContext(Dispatchers.IO) {
            try {
                // Convert map params to SetPilotParams for type safety
                val setPilotParams = SetPilotParams(
                    state = params["state"] as? Boolean,
                    dimming = (params["dimming"] as? Number)?.toInt(),
                    temp = (params["temp"] as? Number)?.toInt(),
                    r = (params["r"] as? Number)?.toInt(),
                    g = (params["g"] as? Number)?.toInt(),
                    b = (params["b"] as? Number)?.toInt(),
                    c = (params["c"] as? Number)?.toInt(),
                    w = (params["w"] as? Number)?.toInt(),
                    speed = (params["speed"] as? Number)?.toInt(),
                    sceneId = (params["sceneId"] as? Number)?.toInt()
                )
                
                val request = WizRequestBuilder.setPilotRequest(setPilotParams)
                val json = gson.toJson(request)
                val buffer = json.toByteArray()

                socketManager.sendPacket(buffer, ip, WizConstants.WIZ_PORT)

                Log.d(TAG, "Sent to $ip: $json")
            } catch (e: Exception) {
                Log.e(TAG, "Error sending command to $ip: ${e.message}")
            }
        }
    }

    /**
     * Get bulb status using type-safe data classes
     */
    suspend fun getBulbStatus(ip: String): Map<String, Any>? {
        return withContext<Map<String, Any>?>(Dispatchers.IO) {
            try {
                // Use type-safe request builder
                val request = WizRequestBuilder.getPilotRequest()
                val json = gson.toJson(request)
                val buffer = json.toByteArray()
                
                val receivePacket = socketManager.sendAndReceive(buffer, ip, WizConstants.WIZ_PORT)
                
                if (receivePacket != null) {
                    val response = String(receivePacket.data, 0, receivePacket.length)
                    Log.d(TAG, "Received from $ip: $response")
                    
                    // Parse using WizResponse data class
                    val wizResponse = gson.fromJson(response, WizResponse::class.java)
                    
                    // Convert back to map for compatibility with existing code
                    val result = wizResponse.result
                    if (result != null) {
                        @Suppress("UNCHECKED_CAST")
                        mapOf(
                            "mac" to result.mac,
                            "rssi" to result.rssi,
                            "state" to result.state,
                            "sceneId" to result.sceneId,
                            "temp" to result.temp,
                            "dimming" to result.dimming,
                            "r" to result.r,
                            "g" to result.g,
                            "b" to result.b,
                            "c" to result.c,
                            "w" to result.w,
                            "speed" to result.speed
                        ).filterValues { it != null } as Map<String, Any>
                    } else {
                        null
                    }
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting status from $ip: ${e.message}")
                null
            }
        }
    }
}