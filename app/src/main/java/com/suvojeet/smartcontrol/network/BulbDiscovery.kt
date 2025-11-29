package com.suvojeet.smartcontrol.network

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress

data class DiscoveredBulb(
    val name: String,
    val ipAddress: String,
    val macAddress: String
)

object BulbDiscovery {
    private const val WIZ_PORT = 38899
    private const val DISCOVERY_TIMEOUT = 5000L // 5 seconds
    private const val TAG = "BulbDiscovery"

    /**
     * Discover WiZ bulbs on the network using UDP broadcast
     */
    suspend fun discoverBulbs(context: Context): List<DiscoveredBulb> {
        return withContext(Dispatchers.IO) {
            val discoveredBulbs = mutableListOf<DiscoveredBulb>()
            
            try {
                // Try broadcast discovery first
                val broadcastResults = discoverViaBroadcast(context)
                discoveredBulbs.addAll(broadcastResults)
                
                // If broadcast didn't find anything, fall back to IP scanning
                if (discoveredBulbs.isEmpty()) {
                    Log.d(TAG, "Broadcast found nothing, trying IP scan...")
                    val scanResults = discoverViaIpScan(context)
                    discoveredBulbs.addAll(scanResults)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Discovery error: ${e.message}", e)
            }
            
            // Remove duplicates by MAC address
            discoveredBulbs.distinctBy { it.macAddress }
        }
    }

    /**
     * Discover bulbs using UDP broadcast
     */
    private suspend fun discoverViaBroadcast(context: Context): List<DiscoveredBulb> {
        return withTimeoutOrNull(DISCOVERY_TIMEOUT) {
            val discoveredBulbs = mutableListOf<DiscoveredBulb>()
            
            try {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val multicastLock = wifiManager.createMulticastLock("smartcontrol_discovery")
                multicastLock.acquire()
                
                try {
                    val socket = DatagramSocket()
                    socket.broadcast = true
                    socket.soTimeout = 3000
                    
                    // Get broadcast address
                    val broadcastAddress = getBroadcastAddress(wifiManager)
                    
                    // Send registration request (WiZ discovery command)
                    val json = """{"method":"registration","params":{"phoneMac":"AAAAAAAAAAAA","register":false}}"""
                    val sendData = json.toByteArray()
                    val sendPacket = DatagramPacket(
                        sendData, 
                        sendData.size, 
                        InetAddress.getByName(broadcastAddress), 
                        WIZ_PORT
                    )
                    
                    Log.d(TAG, "Sending broadcast to $broadcastAddress:$WIZ_PORT")
                    socket.send(sendPacket)
                    
                    // Listen for responses
                    val startTime = System.currentTimeMillis()
                    while (System.currentTimeMillis() - startTime < 3000) {
                        try {
                            val receiveData = ByteArray(1024)
                            val receivePacket = DatagramPacket(receiveData, receiveData.size)
                            socket.receive(receivePacket)
                            
                            val response = String(receivePacket.data, 0, receivePacket.length)
                            val ipAddress = receivePacket.address.hostAddress ?: continue
                            
                            Log.d(TAG, "Received response from $ipAddress: $response")
                            
                            // Parse response
                            val bulb = parseBulbResponse(response, ipAddress)
                            if (bulb != null) {
                                discoveredBulbs.add(bulb)
                                Log.d(TAG, "Discovered bulb: ${bulb.name} at $ipAddress")
                            }
                        } catch (e: Exception) {
                            // Timeout or parsing error, continue listening
                        }
                    }
                    
                    socket.close()
                } finally {
                    multicastLock.release()
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Broadcast discovery error: ${e.message}", e)
            }
            
            discoveredBulbs
        } ?: emptyList()
    }

    /**
     * Discover bulbs by scanning IP range
     */
    private suspend fun discoverViaIpScan(context: Context): List<DiscoveredBulb> {
        return withContext(Dispatchers.IO) {
            val discoveredBulbs = mutableListOf<DiscoveredBulb>()
            
            try {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val ipAddress = wifiManager.connectionInfo.ipAddress
                
                if (ipAddress == 0) {
                    Log.e(TAG, "Not connected to WiFi")
                    return@withContext emptyList()
                }
                
                // Convert IP to string
                val ipString = String.format(
                    "%d.%d.%d.%d",
                    ipAddress and 0xff,
                    ipAddress shr 8 and 0xff,
                    ipAddress shr 16 and 0xff,
                    ipAddress shr 24 and 0xff
                )
                
                // Get subnet (e.g., "192.168.1")
                val subnet = ipString.substringBeforeLast(".")
                Log.d(TAG, "Scanning subnet: $subnet.*")
                
                // Scan IPs concurrently in batches
                val jobs = (1..254).map { i ->
                    async {
                        val testIp = "$subnet.$i"
                        probeBulbAtIp(testIp)
                    }
                }
                
                val results = jobs.awaitAll()
                discoveredBulbs.addAll(results.filterNotNull())
                
            } catch (e: Exception) {
                Log.e(TAG, "IP scan error: ${e.message}", e)
            }
            
            discoveredBulbs
        }
    }

    /**
     * Probe a specific IP to check if it's a WiZ bulb
     */
    private suspend fun probeBulbAtIp(ip: String): DiscoveredBulb? {
        return withTimeoutOrNull(500L) {
            try {
                val socket = DatagramSocket()
                socket.soTimeout = 500
                
                val json = """{"method":"getPilot","params":{}}"""
                val sendData = json.toByteArray()
                val address = InetAddress.getByName(ip)
                val sendPacket = DatagramPacket(sendData, sendData.size, address, WIZ_PORT)
                
                socket.send(sendPacket)
                
                val receiveData = ByteArray(1024)
                val receivePacket = DatagramPacket(receiveData, receiveData.size)
                socket.receive(receivePacket)
                socket.close()
                
                val response = String(receivePacket.data, 0, receivePacket.length)
                parseBulbResponse(response, ip)
                
            } catch (e: Exception) {
                // Not a bulb or timeout
                null
            }
        }
    }

    /**
     * Parse JSON response from bulb
     */
    private fun parseBulbResponse(response: String, ipAddress: String): DiscoveredBulb? {
        return try {
            val gson = Gson()
            val type = object : TypeToken<Map<String, Any>>() {}.type
            val map: Map<String, Any> = gson.fromJson(response, type)
            
            // Extract MAC address from response
            val result = map["result"] as? Map<String, Any>
            val mac = result?.get("mac") as? String 
                ?: map["mac"] as? String 
                ?: "unknown_${ipAddress.replace(".", "_")}"
            
            // Generate a friendly name
            val lastOctet = ipAddress.substringAfterLast(".")
            val name = "WiZ Bulb ($lastOctet)"
            
            DiscoveredBulb(
                name = name,
                ipAddress = ipAddress,
                macAddress = mac
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing response: ${e.message}")
            null
        }
    }

    /**
     * Get broadcast address for current WiFi network
     */
    private fun getBroadcastAddress(wifiManager: WifiManager): String {
        val dhcp = wifiManager.dhcpInfo
        val broadcast = (dhcp.ipAddress and dhcp.netmask) or dhcp.netmask.inv()
        return String.format(
            "%d.%d.%d.%d",
            broadcast and 0xff,
            broadcast shr 8 and 0xff,
            broadcast shr 16 and 0xff,
            broadcast shr 24 and 0xff
        )
    }
}
