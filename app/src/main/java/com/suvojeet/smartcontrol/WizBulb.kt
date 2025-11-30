package com.suvojeet.smartcontrol

import androidx.compose.ui.graphics.Color

data class WizBulb(
    val id: String,
    val name: String,
    val ipAddress: String,
    val isOn: Boolean = false,
    val brightness: Float = 50f,
    val colorInt: Int = Color.White.hashCode(),
    val temperature: Int = 4000, // Kelvin: 2700 (warm) to 6500 (cool)
    val sceneMode: String? = null, // null = manual, or scene name like "fireplace", "sunset", etc.
    val isAvailable: Boolean = true,
    val macAddress: String = "",
    val connectionType: ConnectionType = ConnectionType.WIFI,
    val wattage: Float = 9f // Default 9W, can be customized per bulb
) {
    fun getComposeColor(): Color {
        return Color(colorInt)
    }
}

enum class ConnectionType {
    WIFI, BLE
}