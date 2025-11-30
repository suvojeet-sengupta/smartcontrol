package com.suvojeet.smartcontrol

data class BulbGroup(
    val id: String,
    val name: String,
    val bulbIds: List<String>,
    val isOn: Boolean = false,
    val brightness: Float = 50f
)
