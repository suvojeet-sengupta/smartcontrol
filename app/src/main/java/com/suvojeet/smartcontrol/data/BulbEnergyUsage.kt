package com.suvojeet.smartcontrol.data

data class BulbEnergyUsage(
    val bulbId: String,
    val bulbName: String,
    val date: String, // Format: yyyy-MM-dd
    val energyWh: Float,
    val wattage: Float
)
