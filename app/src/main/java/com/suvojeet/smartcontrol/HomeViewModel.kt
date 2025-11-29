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

    private fun syncWithBulb(id: String) {
        val bulb = bulbs.find { it.id == id } ?: return
        
        viewModelScope.launch {
            val color = bulb.getComposeColor()
            val r = (color.red * 255).toInt()
            val g = (color.green * 255).toInt()
            val b = (color.blue * 255).toInt()

            WizUdpController.sendCommand(
                ip = bulb.ipAddress,
                state = bulb.isOn,
                brightness = bulb.brightness.toInt(),
                r = r, g = g, b = b
            )
        }
    }
}