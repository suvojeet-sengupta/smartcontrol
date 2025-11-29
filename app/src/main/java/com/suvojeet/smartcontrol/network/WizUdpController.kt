package com.suvojeet.smartcontrol.network

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

object WizUdpController {
    private const val WIZ_PORT = 38899

    suspend fun sendCommand(ip: String, params: Map<String, Any>) {
        withContext(Dispatchers.IO) {
            try {
                // Construct JSON manually to avoid extra dependencies if possible, 
                // but since we have Gson, let's use a simple string builder for flexibility
                val paramsJson = params.entries.joinToString(",") { (key, value) ->
                    val jsonValue = if (value is String) "\"$value\"" else value
                    "\"$key\":$jsonValue"
                }
                
                val json = """{"method":"setPilot","params":{$paramsJson}}"""

                val socket = DatagramSocket()
                val address = InetAddress.getByName(ip)
                val buffer = json.toByteArray()

                val packet = DatagramPacket(buffer, buffer.size, address, WIZ_PORT)
                socket.send(packet)
                socket.close()

                Log.d("WizController", "Sent to $ip: $json")
            } catch (e: Exception) {
                Log.e("WizController", "Error sending command to $ip: ${e.message}")
            }
        }
    }

    suspend fun getBulbStatus(ip: String): Map<String, Any>? {
        return withContext(Dispatchers.IO) {
            try {
                val socket = DatagramSocket()
                socket.soTimeout = 2000 // 2 second timeout
                val address = InetAddress.getByName(ip)
                val json = """{"method":"getPilot","params":{}}"""
                val buffer = json.toByteArray()
                val packet = DatagramPacket(buffer, buffer.size, address, WIZ_PORT)
                socket.send(packet)

                val receiveBuffer = ByteArray(1024)
                val receivePacket = DatagramPacket(receiveBuffer, receiveBuffer.size)
                socket.receive(receivePacket)
                socket.close()

                val response = String(receivePacket.data, 0, receivePacket.length)
                Log.d("WizController", "Received from $ip: $response")
                
                val gson = Gson()
                val type = object : TypeToken<Map<String, Any>>() {}.type
                val map: Map<String, Any> = gson.fromJson(response, type)
                
                if (map.containsKey("result")) {
                    map["result"] as? Map<String, Any>
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.e("WizController", "Error getting status from $ip: ${e.message}")
                null
            }
        }
    }
}