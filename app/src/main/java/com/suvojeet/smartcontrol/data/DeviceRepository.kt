package com.suvojeet.smartcontrol.data

import android.content.Context
import android.content.SharedPreferences
import com.suvojeet.smartcontrol.WizBulb
import com.suvojeet.smartcontrol.BulbGroup
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class DeviceRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("wiz_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val KEY_DEVICES = "saved_devices"
    private val KEY_GROUPS = "saved_groups"

    // Load saved bulbs
    fun getDevices(): List<WizBulb> {
        val json = prefs.getString(KEY_DEVICES, null) ?: return emptyList()
        val type = object : TypeToken<List<WizBulb>>() {}.type
        return gson.fromJson(json, type)
    }

    // Save bulbs list
    fun saveDevices(devices: List<WizBulb>) {
        val json = gson.toJson(devices)
        prefs.edit().putString(KEY_DEVICES, json).apply()
    }

    // Load saved groups
    fun getGroups(): List<BulbGroup> {
        val json = prefs.getString(KEY_GROUPS, null) ?: return emptyList()
        val type = object : TypeToken<List<BulbGroup>>() {}.type
        return gson.fromJson(json, type)
    }

    // Save groups list
    fun saveGroups(groups: List<BulbGroup>) {
        val json = gson.toJson(groups)
        prefs.edit().putString(KEY_GROUPS, json).apply()
    }
}