package com.suvojeet.smartcontrol.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DailyEnergyUsage(
    val date: String, // Format: YYYY-MM-DD
    val energyWh: Float
)

class EnergyRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("energy_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val KEY_USAGE_DATA = "usage_data"

    // Map of Date (YYYY-MM-DD) -> Energy (Wh)
    private var usageMap: MutableMap<String, Float> = mutableMapOf()

    init {
        loadData()
    }

    private fun loadData() {
        val json = prefs.getString(KEY_USAGE_DATA, null)
        if (json != null) {
            val type = object : TypeToken<MutableMap<String, Float>>() {}.type
            usageMap = gson.fromJson(json, type)
        }
    }

    private fun saveData() {
        val json = gson.toJson(usageMap)
        prefs.edit().putString(KEY_USAGE_DATA, json).apply()
    }

    fun addUsage(energyWh: Float) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val current = usageMap[today] ?: 0f
        usageMap[today] = current + energyWh
        saveData()
    }

    fun getDailyUsage(days: Int = 7): List<DailyEnergyUsage> {
        val result = mutableListOf<DailyEnergyUsage>()
        val calendar = java.util.Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        // Get last N days including today
        for (i in 0 until days) {
            val dateStr = dateFormat.format(calendar.time)
            val energy = usageMap[dateStr] ?: 0f
            result.add(DailyEnergyUsage(dateStr, energy))
            calendar.add(java.util.Calendar.DAY_OF_YEAR, -1)
        }
        
        return result.reversed() // Return chronological order
    }
    
    fun getTotalUsageToday(): Float {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        return usageMap[today] ?: 0f
    }
}
