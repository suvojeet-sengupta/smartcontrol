package com.suvojeet.smartcontrol.data

import com.suvojeet.smartcontrol.WizBulb
import com.suvojeet.smartcontrol.BulbGroup
import kotlinx.coroutines.flow.Flow

interface DeviceRepository {
    fun getBulbs(): Flow<Resource<List<WizBulb>>>
    fun getGroups(): Flow<List<BulbGroup>>
    
    suspend fun addBulb(bulb: WizBulb)
    suspend fun deleteBulb(id: String)
    suspend fun updateBulb(bulb: WizBulb)
    suspend fun saveDevices(devices: List<WizBulb>)
    
    suspend fun addGroup(group: BulbGroup)
    suspend fun deleteGroup(id: String)
    suspend fun updateGroup(group: BulbGroup)
    suspend fun saveGroups(groups: List<BulbGroup>)
}