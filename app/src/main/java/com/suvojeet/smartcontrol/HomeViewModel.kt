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
import kotlinx.coroutines.launch
import java.util.UUID

// AndroidViewModel use kiya taaki Storage ke liye Context mil jaye
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DeviceRepository(application)
    
    var bulbs by mutableStateOf<List<WizBulb>>(emptyList())
        private set

    init {
        // App khulte hi purane saved bulbs load karo
        loadBulbs()
    }

    private fun loadBulbs() {
        bulbs = repository.getDevices()
    }

    fun addBulb(name: String, ip: String) {
        val newBulb = WizBulb(
            id = UUID.randomUUID().toString(),
            name = name,
            ipAddress = ip
        )
        val updatedList = bulbs + newBulb
        bulbs = updatedList
        repository.saveDevices(updatedList)
    }
    
    fun deleteBulb(id: String) {
        val updatedList = bulbs.filter { it.id != id }
        bulbs = updatedList
        repository.saveDevices(updatedList)
    }

    fun toggleBulb(id: String) {
        bulbs = bulbs.map { if (it.id == id) it.copy(isOn = !it.isOn) else it }
        syncWithBulb(id)
        repository.saveDevices(bulbs)
    }

    fun updateBrightness(id: String, value: Float) {
        bulbs = bulbs.map { if (it.id == id) it.copy(brightness = value) else it }
        syncWithBulb(id)
    }

    fun updateColor(id: String, color: Color) {
        bulbs = bulbs.map { if (it.id == id) it.copy(colorInt = color.toArgb()) else it }
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
        bulbs = bulbs.map { if (it.id == id) it.copy(temperature = temp, sceneMode = null) else it }
        syncWithBulb(id)
        repository.saveDevices(bulbs)
    }

    fun updateScene(id: String, scene: String?) {
        bulbs = bulbs.map { if (it.id == id) it.copy(sceneMode = scene) else it }
        syncWithBulb(id)
        repository.saveDevices(bulbs)
    }

    private fun syncWithBulb(id: String) {
        val bulb = bulbs.find { it.id == id } ?: return
        
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
}