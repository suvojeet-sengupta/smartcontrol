package com.suvojeet.smartcontrol.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

object WizUdpController {
    private const val WIZ_PORT = 38899

    // Ye function chupchap background mein magic packet bhejega
    suspend fun sendCommand(ip: String, state: Boolean, brightness: Int, r: Int, g: Int, b: Int) {
        withContext(Dispatchers.IO) {
            try {
                // Brightness range 10-100 safe hoti hai
                val safeBrightness = brightness.coerceIn(10, 100)
                
                val json = """
                    {"method":"setPilot","params":{"state":$state,"r":$r,"g":$g,"b":$b,"dimming":$safeBrightness}}
                """.trimIndent()

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