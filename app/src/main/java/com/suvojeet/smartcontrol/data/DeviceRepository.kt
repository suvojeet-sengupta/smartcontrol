package com.suvojeet.smartcontrol.data

import android.content.Context
import android.content.SharedPreferences
import com.suvojeet.smartcontrol.WizBulb
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class DeviceRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("wiz_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val KEY_DEVICES = "saved_devices"

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
}