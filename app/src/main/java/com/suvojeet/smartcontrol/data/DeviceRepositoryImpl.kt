package com.suvojeet.smartcontrol.data

import android.content.Context
import android.content.SharedPreferences
import com.suvojeet.smartcontrol.WizBulb
import com.suvojeet.smartcontrol.BulbGroup
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : DeviceRepository {

    private val prefs: SharedPreferences = context.getSharedPreferences("wiz_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val KEY_DEVICES = "saved_devices"
    private val KEY_GROUPS = "saved_groups"

    private val _bulbs = MutableStateFlow<List<WizBulb>>(emptyList())
    private val _groups = MutableStateFlow<List<BulbGroup>>(emptyList())

    init {
        loadDevices()
        loadGroups()
    }

    private fun loadDevices() {
        val json = prefs.getString(KEY_DEVICES, null)
        if (json != null) {
            val type = object : TypeToken<List<WizBulb>>() {}.type
            _bulbs.value = gson.fromJson(json, type)
        }
    }

    private fun loadGroups() {
        val json = prefs.getString(KEY_GROUPS, null)
        if (json != null) {
            val type = object : TypeToken<List<BulbGroup>>() {}.type
            _groups.value = gson.fromJson(json, type)
        }
    }

    override fun getBulbs(): Flow<List<WizBulb>> = _bulbs.asStateFlow()

    override fun getGroups(): Flow<List<BulbGroup>> = _groups.asStateFlow()

    override suspend fun addBulb(bulb: WizBulb) {
        val current = _bulbs.value.toMutableList()
        current.add(bulb)
        saveDevices(current)
    }

    override suspend fun deleteBulb(id: String) {
        val current = _bulbs.value.filter { it.id != id }
        saveDevices(current)
    }

    override suspend fun updateBulb(bulb: WizBulb) {
        val current = _bulbs.value.map { if (it.id == bulb.id) bulb else it }
        saveDevices(current)
    }

    override suspend fun saveDevices(devices: List<WizBulb>) {
        _bulbs.value = devices
        val json = gson.toJson(devices)
        prefs.edit().putString(KEY_DEVICES, json).apply()
    }

    override suspend fun addGroup(group: BulbGroup) {
        val current = _groups.value.toMutableList()
        current.add(group)
        saveGroups(current)
    }

    override suspend fun deleteGroup(id: String) {
        val current = _groups.value.filter { it.id != id }
        saveGroups(current)
    }

    override suspend fun updateGroup(group: BulbGroup) {
        val current = _groups.value.map { if (it.id == group.id) group else it }
        saveGroups(current)
    }

    override suspend fun saveGroups(groups: List<BulbGroup>) {
        _groups.value = groups
        val json = gson.toJson(groups)
        prefs.edit().putString(KEY_GROUPS, json).apply()
    }
}