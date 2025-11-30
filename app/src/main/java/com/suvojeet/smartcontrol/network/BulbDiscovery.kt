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
    val macAddress: String,
    val isBle: Boolean = false
)

object BulbDiscovery {
    private const val TAG = "BulbDiscovery"
    private val gson = Gson()

    /**
     * Discover WiZ bulbs on the network using UDP broadcast and Bluetooth
     */
    suspend fun discoverBulbs(context: Context): List<DiscoveredBulb> {
        return withContext(Dispatchers.IO) {
            val discoveredBulbs = mutableListOf<DiscoveredBulb>()
            
            try {
                // 1. Start Bluetooth Scan (Async)
                val bleController = BluetoothController(context)
                val bleBulbs = mutableListOf<DiscoveredBulb>()
                bleController.onDeviceFound = { bulb ->
                    synchronized(bleBulbs) {
                        bleBulbs.add(bulb)
                    }
                }
                bleController.startScan()

                // 2. Try broadcast discovery
                val broadcastResults = discoverViaBroadcast(context)
                discoveredBulbs.addAll(broadcastResults)
                
                // 3. If broadcast didn't find anything, fall back to IP scanning
                if (discoveredBulbs.isEmpty()) {
                    Log.d(TAG, "Broadcast found nothing, trying batched IP scan...")
                    val scanResults = discoverViaIpScan(context)
                    discoveredBulbs.addAll(scanResults)
                }
                
                // Wait a bit for BLE results if needed, or just add what we have
                // In a real app, we might stream results, but here we'll just wait a moment
                // Since startScan runs for 10s, we might not want to block that long here.
                // For now, we'll just add whatever BLE found so far.
                // Ideally, the UI should listen to a stream, but we are returning a List.
                // Let's rely on the fact that this function is called once.
                // A better approach for BLE is a callback-based ViewModel.
                // But to fit existing architecture, we'll just add what we found quickly.
                
                synchronized(bleBulbs) {
                    discoveredBulbs.addAll(bleBulbs)
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
        return withTimeoutOrNull(WizConstants.DISCOVERY_TIMEOUT) {
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
                    
                    // Send registration request using type-safe data class 🛡️
                    val request = WizRequestBuilder.registrationRequest()
                    val json = gson.toJson(request)
                    val sendData = json.toByteArray()
                    val sendPacket = DatagramPacket(
                        sendData, 
                        sendData.size, 
                        InetAddress.getByName(broadcastAddress), 
                        WizConstants.WIZ_PORT
                    )
                    
                    Log.d(TAG, "Sending broadcast to $broadcastAddress:${WizConstants.WIZ_PORT}")
                    socket.send(sendPacket)
                    
                    // Listen for responses
                    val startTime = System.currentTimeMillis()
                    while (System.currentTimeMillis() - startTime < WizConstants.BROADCAST_LISTEN_DURATION) {
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
                Log.d(TAG, "Scanning subnet: $subnet.* in batches of ${WizConstants.IP_SCAN_BATCH_SIZE}")
                
                // Scan IPs in batches to avoid network spam! 🚦
                // Instead of 254 simultaneous requests, we do 50 at a time
                (1..254).chunked(WizConstants.IP_SCAN_BATCH_SIZE).forEach { batch ->
                    val jobs = batch.map { i ->
                        async {
                            val testIp = "$subnet.$i"
                            probeBulbAtIp(testIp)
                        }
                    }
                    val results = jobs.awaitAll()
                    discoveredBulbs.addAll(results.filterNotNull())
                    Log.d(TAG, "Batch complete: found ${results.filterNotNull().size} bulbs")
                }
                
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
        return withTimeoutOrNull(WizConstants.PROBE_TIMEOUT) {
            try {
                val socket = DatagramSocket()
                socket.soTimeout = WizConstants.PROBE_TIMEOUT.toInt()
                
                // Use type-safe getPilot request 🛡️
                val request = WizRequestBuilder.getPilotRequest()
                val json = gson.toJson(request)
                val sendData = json.toByteArray()
                val address = InetAddress.getByName(ip)
                val sendPacket = DatagramPacket(sendData, sendData.size, address, WizConstants.WIZ_PORT)
                
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
