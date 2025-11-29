package com.suvojeet.smartcontrol.network

import android.util.Log
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
}