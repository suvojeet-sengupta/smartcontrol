package com.suvojeet.smartcontrol.network

import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

/**
 * WiZ UDP Controller - Now with type-safe data classes! 🛡️
 * No more manual JSON string construction
 */
object WizUdpController {
    private const val TAG = "WizController"
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

                val socket = DatagramSocket()
                val address = InetAddress.getByName(ip)
                val buffer = json.toByteArray()

                val packet = DatagramPacket(buffer, buffer.size, address, WizConstants.WIZ_PORT)
                socket.send(packet)
                socket.close()

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
        return withContext(Dispatchers.IO) {
            try {
                val socket = DatagramSocket()
                socket.soTimeout = WizConstants.SOCKET_TIMEOUT
                val address = InetAddress.getByName(ip)
                
                // Use type-safe request builder
                val request = WizRequestBuilder.getPilotRequest()
                val json = gson.toJson(request)
                val buffer = json.toByteArray()
                val packet = DatagramPacket(buffer, buffer.size, address, WizConstants.WIZ_PORT)
                socket.send(packet)

                val receiveBuffer = ByteArray(1024)
                val receivePacket = DatagramPacket(receiveBuffer, receiveBuffer.size)
                socket.receive(receivePacket)
                socket.close()

                val response = String(receivePacket.data, 0, receivePacket.length)
                Log.d(TAG, "Received from $ip: $response")
                
                // Parse using WizResponse data class
                val wizResponse = gson.fromJson(response, WizResponse::class.java)
                
                // Convert back to map for compatibility with existing code
                val result = wizResponse.result
                return@withContext if (result != null) {
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
                    ).filterValues { it != null }
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting status from $ip: ${e.message}")
                return@withContext null
            }
        }
    }
}