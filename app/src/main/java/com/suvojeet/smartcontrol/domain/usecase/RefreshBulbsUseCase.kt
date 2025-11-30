package com.suvojeet.smartcontrol.domain.usecase

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.suvojeet.smartcontrol.WizBulb
import com.suvojeet.smartcontrol.data.DeviceRepository
import com.suvojeet.smartcontrol.data.EnergyRepository
import com.suvojeet.smartcontrol.network.WizUdpController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject

class RefreshBulbsUseCase @Inject constructor(
    private val deviceRepository: DeviceRepository,
    private val energyRepository: EnergyRepository,
    private val wizUdpController: WizUdpController
) {
    // Map to track last update time for cooldown logic
    private val lastUpdateMap = mutableMapOf<String, Long>()
    
    private val sceneMap = mapOf(
        "ocean" to 1, "romance" to 2, "sunset" to 3, "party" to 4,
        "fireplace" to 5, "cozy" to 6, "forest" to 7, "pastel" to 8,
        "wakeup" to 9, "bedtime" to 10, "warmwhite" to 11, "daylight" to 12,
        "coolwhite" to 13, "nightlight" to 14, "focus" to 15, "relax" to 16,
        "truecolors" to 17, "tvtime" to 18, "plantgrowth" to 19, "spring" to 20,
        "summer" to 21, "fall" to 22, "deepdive" to 23, "jungle" to 24,
        "mojito" to 25, "club" to 26, "christmas" to 27, "halloween" to 28,
        "candlelight" to 29, "goldenwhite" to 30, "pulse" to 31, "steampunk" to 32,
        "diwali" to 33
    )

    suspend operator fun invoke() {
        val resource = deviceRepository.getBulbs().first()
        val bulbs = resource.data ?: return // If loading or error with no data, skip refresh
        var totalEnergyIncrement = 0f
        
        val updatedBulbs = bulbs.map { bulb ->
            // Calculate energy for this bulb for the 3s interval
            if (bulb.isOn) {
                val watts = bulb.wattage * (bulb.brightness / 100f)
                val hours = 3f / 3600f
                val energyWh = watts * hours
                totalEnergyIncrement += energyWh
                
                // Track per-bulb usage
                energyRepository.addBulbUsage(bulb.id, energyWh)
            }

            // Check cooldown
            val lastUpdate = lastUpdateMap[bulb.id] ?: 0L
            if (System.currentTimeMillis() - lastUpdate < 3000) {
                return@map bulb // Skip update if within cooldown
            }

            val status = wizUdpController.getBulbStatus(bulb.ipAddress)
            if (status != null) {
                val result = status["result"] as? Map<String, Any> ?: status
                
                val isOn = result["state"] as? Boolean ?: bulb.isOn
                val dimming = (result["dimming"] as? Number)?.toFloat() ?: bulb.brightness
                val sceneId = (result["sceneId"] as? Number)?.toInt() ?: 0
                val temp = (result["temp"] as? Number)?.toInt() ?: 0
                val r = (result["r"] as? Number)?.toInt() ?: 0
                val g = (result["g"] as? Number)?.toInt() ?: 0
                val b = (result["b"] as? Number)?.toInt() ?: 0

                var newSceneMode: String? = null
                var newColorInt = bulb.colorInt
                var newTemp = bulb.temperature

                if (sceneId > 0) {
                    newSceneMode = sceneMap.entries.find { it.value == sceneId }?.key
                } else if (temp > 0) {
                    newTemp = temp
                    newSceneMode = null
                    newColorInt = Color.White.toArgb()
                } else {
                    newSceneMode = null
                    if (r > 0 || g > 0 || b > 0) {
                            newColorInt = Color(r / 255f, g / 255f, b / 255f).toArgb()
                    }
                }

                bulb.copy(
                    isOn = isOn,
                    brightness = dimming,
                    sceneMode = newSceneMode,
                    temperature = newTemp,
                    colorInt = newColorInt,
                    isAvailable = true
                )
            } else {
                bulb.copy(isAvailable = false)
            }
        }
        
        // Save updated bulbs to repository
        // Only save if there are changes to avoid infinite loops if repo emits on save
        // But here we are just updating status.
        // Ideally we should have a method in repo to update statuses without triggering a full save if it's just transient,
        // but for now saveDevices is fine.
        if (updatedBulbs != bulbs) {
             deviceRepository.saveDevices(updatedBulbs)
        }
        
        // Update energy repository if any usage occurred
        if (totalEnergyIncrement > 0) {
            energyRepository.addUsage(totalEnergyIncrement)
        }
    }
    
    fun markUpdate(bulbId: String) {
        lastUpdateMap[bulbId] = System.currentTimeMillis()
    }
}
