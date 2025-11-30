package com.suvojeet.smartcontrol.domain.usecase

import com.suvojeet.smartcontrol.data.DeviceRepository
import com.suvojeet.smartcontrol.network.BluetoothController
import com.suvojeet.smartcontrol.network.WizUdpController
import com.suvojeet.smartcontrol.ConnectionType
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ToggleBulbUseCase @Inject constructor(
    private val repository: DeviceRepository,
    private val bluetoothController: BluetoothController,
    private val wizUdpController: WizUdpController
) {
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

    suspend operator fun invoke(bulbId: String, currentBulbs: List<com.suvojeet.smartcontrol.WizBulb>) {
        val bulb = currentBulbs.find { it.id == bulbId } ?: return
        val newState = !bulb.isOn
        val updatedBulb = bulb.copy(isOn = newState)
        
        // Update local state
        repository.updateBulb(updatedBulb)
        
        if (bulb.connectionType == ConnectionType.BLE) {
             withContext(Dispatchers.Main) {
                 try {
                     bluetoothController.connect(bulb.macAddress)
                     delay(500)
                     if (newState) {
                         bluetoothController.setPower(true)
                         bluetoothController.setBrightness(bulb.brightness.toInt())
                         // Restore color/temp if needed
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
        } else {
            // Send command
            withContext(Dispatchers.IO) {
                val params = mutableMapOf<String, Any>("state" to newState)
                
                if (newState) {
                    // Restore state when turning ON
                    params["dimming"] = bulb.brightness.toInt().coerceIn(10, 100)
                    
                    if (bulb.sceneMode != null) {
                        val sceneId = sceneMap[bulb.sceneMode] ?: 0
                        if (sceneId > 0) {
                            params["sceneId"] = sceneId
                        }
                    } else if (bulb.temperature > 0 && bulb.colorInt == Color.White.toArgb()) {
                        params["temp"] = bulb.temperature
                    } else {
                        val color = bulb.getComposeColor()
                        params["r"] = (color.red * 255).toInt()
                        params["g"] = (color.green * 255).toInt()
                        params["b"] = (color.blue * 255).toInt()
                    }
                }
                
                wizUdpController.sendCommand(bulb.ipAddress, params)
            }
        }
    }
}
