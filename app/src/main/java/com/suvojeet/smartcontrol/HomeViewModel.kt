package com.suvojeet.smartcontrol

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeet.smartcontrol.data.DeviceRepository
import com.suvojeet.smartcontrol.network.WizUdpController
import com.suvojeet.smartcontrol.network.BulbDiscovery
import com.suvojeet.smartcontrol.network.DiscoveredBulb
import com.suvojeet.smartcontrol.network.BluetoothController
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.UUID

// AndroidViewModel use kiya taaki Storage ke liye Context mil jaye
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DeviceRepository(application)
    private val context = application.applicationContext
    private val bluetoothController = BluetoothController(context)
    
    var bulbs by mutableStateOf<List<WizBulb>>(emptyList())
        private set
        repository.saveDevices(updatedList)
    }

    fun deleteBulbs(ids: List<String>) {
        val updatedList = bulbs.filter { it.id !in ids }
        bulbs = updatedList
        repository.saveDevices(updatedList)
    }

    fun toggleBulb(id: String) {
        lastUpdateMap[id] = System.currentTimeMillis()
        bulbs = bulbs.map { if (it.id == id) it.copy(isOn = !it.isOn) else it }
        syncWithBulb(id)
        repository.saveDevices(bulbs)
    }

    fun updateBrightness(id: String, value: Float) {
        lastUpdateMap[id] = System.currentTimeMillis()
        bulbs = bulbs.map { if (it.id == id) it.copy(brightness = value) else it }
        syncWithBulb(id)
    }

    fun updateColor(id: String, color: Color) {
        lastUpdateMap[id] = System.currentTimeMillis()
        bulbs = bulbs.map { 
            if (it.id == id) {
                it.copy(
                    colorInt = color.toArgb(),
                    sceneMode = null // Reset scene mode so color takes effect
                ) 
            } else it 
        }
        syncWithBulb(id)
        repository.saveDevices(bulbs)
    }

    private val sceneMap = mapOf(
        "ocean" to 1, "romance" to 2, "sunset" to 3, "party" to 4,
        "fireplace" to 5, "cozy" to 6, "forest" to 7, "pastel" to 8,
        "wakeup" to 9, "bedtime" to 10, "warmwhite" to 11, "daylight" to 12,
        "coolwhite" to 13, "nightlight" to 14, "focus" to 15, "relax" to 16,
        "truecolors" to 17, "tvtime" to 18, "plantgrowth" to 19, "spring" to 20,
        "summer" to 21, "fall" to 22, "deepdive" to 23, "jungle" to 24,
        "mojito" to 25, "club" to 26, "christmas" to 27, "halloween" to 28,
        "candlelight" to 29, "goldenwhite" to 30, "pulse" to 31, "steampunk" to 32
    )

    fun updateTemperature(id: String, temp: Int) {
        lastUpdateMap[id] = System.currentTimeMillis()
        bulbs = bulbs.map { 
            if (it.id == id) {
                it.copy(
                    temperature = temp, 
                    sceneMode = null,
                    colorInt = Color.White.toArgb() // Reset color to ensure white mode is active
                ) 
            } else it 
        }
        syncWithBulb(id)
        repository.saveDevices(bulbs)
    }

    fun updateScene(id: String, scene: String?) {
        lastUpdateMap[id] = System.currentTimeMillis()
        bulbs = bulbs.map { if (it.id == id) it.copy(sceneMode = scene) else it }
        syncWithBulb(id)
        repository.saveDevices(bulbs)
    }

    private fun syncWithBulb(id: String) {
        val bulb = bulbs.find { it.id == id } ?: return
        
        if (bulb.connectionType == ConnectionType.BLE) {
            // BLE Control
            viewModelScope.launch {
                try {
                    bluetoothController.connect(bulb.macAddress, context)
                    // Small delay to ensure connection
                    delay(500)
                    
                    if (bulb.isOn) {
                        bluetoothController.setPower(true)
                        bluetoothController.setBrightness(bulb.brightness.toInt())
                        
                        if (bulb.temperature > 0 && bulb.colorInt == Color.White.toArgb()) {
                            bluetoothController.setTemperature(bulb.temperature)
                        } else {
                            val color = bulb.getComposeColor()
                            bluetoothController.setColor(
                                (color.red * 255).toInt(),
                                (color.green * 255).toInt(),
                                (color.blue * 255).toInt()
                            )
                        }
                    } else {
                        bluetoothController.setPower(false)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            return
        }
        
        viewModelScope.launch {
            val params = mutableMapOf<String, Any>(
                "state" to bulb.isOn,
                "dimming" to bulb.brightness.toInt().coerceIn(10, 100)
            )

            if (bulb.isOn) {
                if (bulb.sceneMode != null) {
                    // Scene mode
                    val sceneId = sceneMap[bulb.sceneMode] ?: 0
                    if (sceneId > 0) {
                        params["sceneId"] = sceneId
                    }
                } else if (bulb.temperature > 0 && bulb.colorInt == Color.White.hashCode()) {
                    // White mode (if color is default white or specifically set to white)
                    // Note: This logic assumes if color is white, we use temp. 
                    // Better might be to track "mode" explicitly, but this works for now.
                    params["temp"] = bulb.temperature
                } else {
                    // Color mode
                    val color = bulb.getComposeColor()
                    params["r"] = (color.red * 255).toInt()
                    params["g"] = (color.green * 255).toInt()
                    params["b"] = (color.blue * 255).toInt()
                }
            }

            WizUdpController.sendCommand(bulb.ipAddress, params)
        }
    }

    // Group Management
    fun createGroup(name: String, bulbIds: List<String>) {
        val newGroup = BulbGroup(
            id = UUID.randomUUID().toString(),
            name = name,
            bulbIds = bulbIds
        )
        val updatedGroups = groups + newGroup
        groups = updatedGroups
        repository.saveGroups(updatedGroups)
    }

    fun deleteGroup(groupId: String) {
        val updatedGroups = groups.filter { it.id != groupId }
        groups = updatedGroups
        repository.saveGroups(updatedGroups)
    }

    // Group Control
    fun toggleGroup(groupId: String) {
        val group = groups.find { it.id == groupId } ?: return
        val newState = !group.isOn
        
        // Update group state
        groups = groups.map { if (it.id == groupId) it.copy(isOn = newState) else it }
        repository.saveGroups(groups)

        // Update all bulbs in group
        group.bulbIds.forEach { bulbId ->
            // Update local state
            bulbs = bulbs.map { if (it.id == bulbId) it.copy(isOn = newState) else it }
            // Send command
            syncWithBulb(bulbId)
        }
        repository.saveDevices(bulbs)
    }

    fun updateGroupBrightness(groupId: String, brightness: Float) {
        val group = groups.find { it.id == groupId } ?: return
        
        groups = groups.map { if (it.id == groupId) it.copy(brightness = brightness) else it }
        repository.saveGroups(groups)

        group.bulbIds.forEach { bulbId ->
            updateBrightness(bulbId, brightness)
        }
    }

    fun updateGroupColor(groupId: String, color: Color) {
        val group = groups.find { it.id == groupId } ?: return
        group.bulbIds.forEach { bulbId ->
            updateColor(bulbId, color)
        }
    }

    fun updateGroupTemperature(groupId: String, temp: Int) {
        val group = groups.find { it.id == groupId } ?: return
        group.bulbIds.forEach { bulbId ->
            updateTemperature(bulbId, temp)
        }
    }

    fun updateGroupScene(groupId: String, scene: String?) {
        val group = groups.find { it.id == groupId } ?: return
        group.bulbIds.forEach { bulbId ->
            updateScene(bulbId, scene)
        }
    }

    // Discovery functions
    fun startDiscovery() {
        viewModelScope.launch {
            try {
                discoveryState = DiscoveryState.Scanning
                discoveredBulbs = emptyList()
                
                val found = BulbDiscovery.discoverBulbs(context)
                
                // Filter out already added bulbs
                val existingIps = bulbs.map { it.ipAddress }.toSet()
                val newBulbs = found.filter { it.ipAddress !in existingIps }
                
                discoveredBulbs = newBulbs
                discoveryState = if (newBulbs.isNotEmpty()) {
                    DiscoveryState.Success
                } else {
                    DiscoveryState.NoDevicesFound
                }
            } catch (e: Exception) {
                discoveryState = DiscoveryState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun addDiscoveredBulb(discovered: DiscoveredBulb) {
        if (bulbs.none { it.id == discovered.macAddress }) {
            val newBulb = WizBulb(
                id = discovered.macAddress,
                name = discovered.name,
                ipAddress = discovered.ipAddress,
                macAddress = discovered.macAddress,
                connectionType = if (discovered.isBle) ConnectionType.BLE else ConnectionType.WIFI
            )
            val updatedBulbs = bulbs + newBulb
            bulbs = updatedBulbs
            repository.saveDevices(updatedBulbs)
            
            // Remove from discovered list
            discoveredBulbs = discoveredBulbs.filter { it.macAddress != discovered.macAddress }
        }
    }

    fun addAllDiscoveredBulbs() {
        val newBulbs = discoveredBulbs.map { discovered ->
            WizBulb(
                id = discovered.macAddress,
                name = discovered.name,
                ipAddress = discovered.ipAddress,
                macAddress = discovered.macAddress,
                connectionType = if (discovered.isBle) ConnectionType.BLE else ConnectionType.WIFI
            )
        }
        // Filter out duplicates again just in case
        val uniqueNewBulbs = newBulbs.filter { new -> bulbs.none { it.id == new.id } }
        
        val updatedList = bulbs + uniqueNewBulbs
        bulbs = updatedList
        repository.saveDevices(updatedList)
        
        // Clear discovered list
        discoveredBulbs = emptyList()
        discoveryState = DiscoveryState.Idle
    }

    fun resetDiscovery() {
        discoveryState = DiscoveryState.Idle
        discoveredBulbs = emptyList()
    }
}

// Discovery state sealed class
sealed class DiscoveryState {
    object Idle : DiscoveryState()
    object Scanning : DiscoveryState()
    object Success : DiscoveryState()
    object NoDevicesFound : DiscoveryState()
    data class Error(val message: String) : DiscoveryState()
}