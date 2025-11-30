package com.suvojeet.smartcontrol

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeet.smartcontrol.data.DeviceRepository
import com.suvojeet.smartcontrol.data.EnergyRepository
import com.suvojeet.smartcontrol.data.BulbEnergyUsage
import com.suvojeet.smartcontrol.data.Resource
import com.suvojeet.smartcontrol.domain.usecase.GetBulbsUseCase
import com.suvojeet.smartcontrol.domain.usecase.RefreshBulbsUseCase
import com.suvojeet.smartcontrol.domain.usecase.ToggleBulbUseCase
import com.suvojeet.smartcontrol.domain.usecase.UpdateBulbUseCase
import com.suvojeet.smartcontrol.network.BulbDiscovery
import com.suvojeet.smartcontrol.network.DiscoveredBulb
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getBulbsUseCase: GetBulbsUseCase,
    private val toggleBulbUseCase: ToggleBulbUseCase,
    private val updateBulbUseCase: UpdateBulbUseCase,
    private val refreshBulbsUseCase: RefreshBulbsUseCase,
    private val repository: DeviceRepository,
    private val energyRepository: EnergyRepository,
    private val application: Application
) : ViewModel() {

    // State backed by Flow
    var bulbs by mutableStateOf<List<WizBulb>>(emptyList())
        private set

    var groups by mutableStateOf<List<BulbGroup>>(emptyList())
        private set

    // Discovery state
    var discoveryState by mutableStateOf<DiscoveryState>(DiscoveryState.Idle)
        private set
    
    var discoveredBulbs by mutableStateOf<List<DiscoveredBulb>>(emptyList())
        private set

    var energyUsageHistory by mutableStateOf<List<com.suvojeet.smartcontrol.data.DailyEnergyUsage>>(emptyList())
        private set
        
    var todayUsage by mutableStateOf(0f)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    init {
        // Collect Flows
        viewModelScope.launch {
            getBulbsUseCase().collectLatest { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        isLoading = true
                    }
                    is Resource.Success -> {
                        isLoading = false
                        bulbs = resource.data ?: emptyList()
                        errorMessage = null
                    }
                    is Resource.Error -> {
                        isLoading = false
                        errorMessage = resource.message
                        if (resource.data != null) {
                            bulbs = resource.data
                        }
                    }
                }
            }
        }
        
        viewModelScope.launch {
            repository.getGroups().collectLatest {
                groups = it
            }
        }

        loadEnergyData()
        startPolling()
    }
    
    private fun loadEnergyData() {
        energyUsageHistory = energyRepository.getDailyUsage()
        todayUsage = energyRepository.getTotalUsageToday()
    }

    private fun startPolling() {
        viewModelScope.launch {
            while (isActive) {
                try {
                    refreshBulbsUseCase()
                    // Update energy UI after refresh
                    todayUsage = energyRepository.getTotalUsageToday()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(3000) // Poll every 3 seconds
            }
        }
    }

    fun addBulb(name: String, ip: String) {
        viewModelScope.launch {
            val newBulb = WizBulb(
                id = UUID.randomUUID().toString(),
                name = name,
                ipAddress = ip
            )
            repository.addBulb(newBulb)
        }
    }

    fun deleteBulb(id: String) {
        viewModelScope.launch {
            repository.deleteBulb(id)
        }
    }

    fun deleteBulbs(ids: List<String>) {
        viewModelScope.launch {
            ids.forEach { repository.deleteBulb(it) }
        }
    }

    fun toggleBulb(id: String) {
        viewModelScope.launch {
            toggleBulbUseCase(id, bulbs)
            refreshBulbsUseCase.markUpdate(id)
        }
    }

    fun updateBrightness(id: String, value: Float) {
        viewModelScope.launch {
            val bulb = bulbs.find { it.id == id } ?: return@launch
            updateBulbUseCase.updateBrightness(bulb, value)
            refreshBulbsUseCase.markUpdate(id)
        }
    }

    fun updateColor(id: String, color: Color) {
        viewModelScope.launch {
            val bulb = bulbs.find { it.id == id } ?: return@launch
            updateBulbUseCase.updateColor(bulb, color)
            refreshBulbsUseCase.markUpdate(id)
        }
    }
    
    fun updateBulbWattage(id: String, wattage: Float) {
        viewModelScope.launch {
            val bulb = bulbs.find { it.id == id } ?: return@launch
            repository.updateBulb(bulb.copy(wattage = wattage))
        }
    }
    
    fun getBulbUsageHistory(bulbId: String): List<BulbEnergyUsage> {
        return energyRepository.getBulbUsageHistory(bulbId)
    }
    
    fun getBulbUsageToday(bulbId: String): Float {
        return energyRepository.getBulbUsageToday(bulbId)
    }

    fun updateTemperature(id: String, temp: Int) {
        viewModelScope.launch {
            val bulb = bulbs.find { it.id == id } ?: return@launch
            updateBulbUseCase.updateTemperature(bulb, temp)
            refreshBulbsUseCase.markUpdate(id)
        }
    }

    fun updateScene(id: String, scene: String?) {
        viewModelScope.launch {
            val bulb = bulbs.find { it.id == id } ?: return@launch
            val sceneId = sceneMap[scene] ?: 0
            if (sceneId > 0 && scene != null) {
                updateBulbUseCase.updateScene(bulb, sceneId, scene)
                refreshBulbsUseCase.markUpdate(id)
            }
        }
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

    // Group Management
    fun createGroup(name: String, bulbIds: List<String>) {
        viewModelScope.launch {
            val newGroup = BulbGroup(
                id = UUID.randomUUID().toString(),
                name = name,
                bulbIds = bulbIds
            )
            repository.addGroup(newGroup)
        }
    }

    fun deleteGroup(groupId: String) {
        viewModelScope.launch {
            repository.deleteGroup(groupId)
        }
    }

    // Group Control
    fun toggleGroup(groupId: String) {
        viewModelScope.launch {
            val group = groups.find { it.id == groupId } ?: return@launch
            val newState = !group.isOn
            
            // Update group state
            repository.updateGroup(group.copy(isOn = newState))

            // Update all bulbs in group
            group.bulbIds.forEach { bulbId ->
                toggleBulb(bulbId) // Re-use toggleBulb which uses UseCase
            }
        }
    }

    fun updateGroupBrightness(groupId: String, brightness: Float) {
        viewModelScope.launch {
            val group = groups.find { it.id == groupId } ?: return@launch
            repository.updateGroup(group.copy(brightness = brightness))

            group.bulbIds.forEach { bulbId ->
                updateBrightness(bulbId, brightness)
            }
        }
    }

    fun updateGroupColor(groupId: String, color: Color) {
        viewModelScope.launch {
            val group = groups.find { it.id == groupId } ?: return@launch
            // No color property on group usually, but if there was we'd update it.
            // Just update bulbs.
            group.bulbIds.forEach { bulbId ->
                updateColor(bulbId, color)
            }
        }
    }

    fun updateGroupTemperature(groupId: String, temp: Int) {
        viewModelScope.launch {
            val group = groups.find { it.id == groupId } ?: return@launch
            group.bulbIds.forEach { bulbId ->
                updateTemperature(bulbId, temp)
            }
        }
    }

    fun updateGroupScene(groupId: String, scene: String?) {
        viewModelScope.launch {
            val group = groups.find { it.id == groupId } ?: return@launch
            group.bulbIds.forEach { bulbId ->
                updateScene(bulbId, scene)
            }
        }
    }

    // Discovery functions
    fun startDiscovery() {
        viewModelScope.launch {
            try {
                discoveryState = DiscoveryState.Scanning
                discoveredBulbs = emptyList()
                
                // Using application context for discovery
                val found = BulbDiscovery.discoverBulbs(application)
                
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
        viewModelScope.launch {
            if (bulbs.none { it.id == discovered.macAddress }) {
                val newBulb = WizBulb(
                    id = discovered.macAddress,
                    name = discovered.name,
                    ipAddress = discovered.ipAddress,
                    macAddress = discovered.macAddress,
                    connectionType = if (discovered.isBle) ConnectionType.BLE else ConnectionType.WIFI
                )
                repository.addBulb(newBulb)
                
                // Remove from discovered list
                discoveredBulbs = discoveredBulbs.filter { it.macAddress != discovered.macAddress }
            }
        }
    }

    fun addAllDiscoveredBulbs() {
        viewModelScope.launch {
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
            
            uniqueNewBulbs.forEach { repository.addBulb(it) }
            
            // Clear discovered list
            discoveredBulbs = emptyList()
            discoveryState = DiscoveryState.Idle
        }
    }

    fun resetDiscovery() {
        discoveryState = DiscoveryState.Idle
        discoveredBulbs = emptyList()
    }
}