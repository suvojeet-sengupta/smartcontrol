package com.suvojeet.smartcontrol.domain.usecase

import com.suvojeet.smartcontrol.data.DeviceRepository
import com.suvojeet.smartcontrol.network.BluetoothController
import com.suvojeet.smartcontrol.network.WizUdpController
import com.suvojeet.smartcontrol.ConnectionType
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ToggleBulbUseCase @Inject constructor(
    private val repository: DeviceRepository,
    private val bluetoothController: BluetoothController
) {
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
                         // Restore color/temp if needed, but simple toggle just power on
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
                val params = mapOf("state" to newState)
                WizUdpController.sendCommand(bulb.ipAddress, params)
            }
        }
    }
}
