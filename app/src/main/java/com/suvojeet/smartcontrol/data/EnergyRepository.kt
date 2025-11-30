package com.suvojeet.smartcontrol.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class DailyEnergyUsage(
    val date: String, // Format: YYYY-MM-DD
    val energyWh: Float
)


@Singleton
class EnergyRepository @Inject constructor(@ApplicationContext context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("energy_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val KEY_USAGE_DATA = "usage_data"
    private val KEY_BULB_USAGE = "bulb_usage_data"

    // Map of Date (YYYY-MM-DD) -> Energy (Wh) for total
    private var usageMap: MutableMap<String, Float> = mutableMapOf()
    
    // Map of BulbId -> (Date -> Energy) for per-bulb tracking
    private var bulbUsageMap: MutableMap<String, MutableMap<String, Float>> = mutableMapOf()

    init {
        loadData()
        loadBulbData()
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
    
    // Per-Bulb Tracking Methods
    private fun loadBulbData() {
        val json = prefs.getString(KEY_BULB_USAGE, null)
        if (json != null) {
            val type = object : TypeToken<MutableMap<String, MutableMap<String, Float>>>() {}.type
            bulbUsageMap = gson.fromJson(json, type)
        }
    }
    
    private fun saveBulbData() {
        val json = gson.toJson(bulbUsageMap)
        prefs.edit().putString(KEY_BULB_USAGE, json).apply()
    }
    
    fun addBulbUsage(bulbId: String, energyWh: Float) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        
        if (!bulbUsageMap.containsKey(bulbId)) {
            bulbUsageMap[bulbId] = mutableMapOf()
        }
        
        val current = bulbUsageMap[bulbId]!![today] ?: 0f
        bulbUsageMap[bulbId]!![today] = current + energyWh
        saveBulbData()
    }
    
    fun getBulbUsageToday(bulbId: String): Float {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        return bulbUsageMap[bulbId]?.get(today) ?: 0f
    }
    
    fun getAllBulbsUsageToday(): Map<String, Float> {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val result = mutableMapOf<String, Float>()
        
        bulbUsageMap.forEach { (bulbId, dateMap) ->
            result[bulbId] = dateMap[today] ?: 0f
        }
        
        return result
    }
    
    fun getBulbUsageHistory(bulbId: String, days: Int = 7): List<BulbEnergyUsage> {
        val result = mutableListOf<BulbEnergyUsage>()
        val calendar = java.util.Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        
        val bulbData = bulbUsageMap[bulbId] ?: return emptyList()
        
        for (i in 0 until days) {
            val dateStr = dateFormat.format(calendar.time)
            val energy = bulbData[dateStr] ?: 0f
            result.add(BulbEnergyUsage(bulbId, "", dateStr, energy, 0f))
            calendar.add(java.util.Calendar.DAY_OF_YEAR, -1)
        }
        
        return result.reversed()
    }
}
