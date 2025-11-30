package com.suvojeet.smartcontrol.domain.usecase

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.suvojeet.smartcontrol.WizBulb
import com.suvojeet.smartcontrol.data.DeviceRepository
import com.suvojeet.smartcontrol.network.WizUdpController
import com.suvojeet.smartcontrol.network.BluetoothController
import com.suvojeet.smartcontrol.ConnectionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import javax.inject.Inject

class UpdateBulbUseCase @Inject constructor(
    private val repository: DeviceRepository,
    private val bluetoothController: BluetoothController,
    private val wizUdpController: WizUdpController
) {
    suspend fun updateBrightness(bulb: WizBulb, brightness: Float) {
        val updatedBulb = bulb.copy(brightness = brightness)
        repository.updateBulb(updatedBulb)
        
        if (bulb.connectionType == ConnectionType.BLE) {
            withContext(Dispatchers.Main) {
                try {
                    bluetoothController.connect(bulb.macAddress)
                    delay(200)
                    bluetoothController.setBrightness(brightness.toInt())
                } catch (e: Exception) { e.printStackTrace() }
            }
        } else {
            withContext(Dispatchers.IO) {
                val params = mapOf(
                    "state" to true,
                    "dimming" to brightness.toInt().coerceIn(10, 100)
                )
                wizUdpController.sendCommand(bulb.ipAddress, params)
            }
        }
    }

    suspend fun updateColor(bulb: WizBulb, color: Color) {
        val updatedBulb = bulb.copy(
            colorInt = color.toArgb(),
            sceneMode = null
        )
        repository.updateBulb(updatedBulb)
        
        if (bulb.connectionType == ConnectionType.BLE) {
            withContext(Dispatchers.Main) {
                try {
                    bluetoothController.connect(bulb.macAddress)
                    delay(200)
                    bluetoothController.setColor(
                        (color.red * 255).toInt(),
                        (color.green * 255).toInt(),
                        (color.blue * 255).toInt()
                    )
                } catch (e: Exception) { e.printStackTrace() }
            }
        } else {
            withContext(Dispatchers.IO) {
                val params = mapOf(
                    "state" to true,
                    "r" to (color.red * 255).toInt(),
                    "g" to (color.green * 255).toInt(),
                    "b" to (color.blue * 255).toInt()
                )
                wizUdpController.sendCommand(bulb.ipAddress, params)
            }
        }
    }
    
    suspend fun updateTemperature(bulb: WizBulb, temp: Int) {
        val updatedBulb = bulb.copy(
            temperature = temp,
            sceneMode = null,
            colorInt = Color.White.toArgb()
        )
        repository.updateBulb(updatedBulb)
        
        if (bulb.connectionType == ConnectionType.BLE) {
            withContext(Dispatchers.Main) {
                try {
                    bluetoothController.connect(bulb.macAddress)
                    delay(200)
                    bluetoothController.setTemperature(temp)
                } catch (e: Exception) { e.printStackTrace() }
            }
        } else {
            withContext(Dispatchers.IO) {
                val params = mapOf(
                    "state" to true,
                    "temp" to temp
                )
                wizUdpController.sendCommand(bulb.ipAddress, params)
            }
        }
    }
    
    suspend fun updateScene(bulb: WizBulb, sceneId: Int, sceneName: String) {
        val updatedBulb = bulb.copy(sceneMode = sceneName)
        repository.updateBulb(updatedBulb)
        
        if (bulb.connectionType == ConnectionType.BLE) {
            // BLE scenes not yet implemented in controller
        } else {
            withContext(Dispatchers.IO) {
                val params = mapOf(
                    "state" to true,
                    "sceneId" to sceneId
                )
                wizUdpController.sendCommand(bulb.ipAddress, params)
            }
        }
    }
}
