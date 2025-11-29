package com.suvojeet.smartcontrol

import androidx.compose.ui.graphics.Color

data class WizBulb(
    val id: String,
    val name: String,
    val ipAddress: String,
    val isOn: Boolean = false,
    val brightness: Float = 50f,
    val colorInt: Int = Color.White.hashCode()
) {
    fun getComposeColor(): Color {
        return Color(colorInt)
    }
}