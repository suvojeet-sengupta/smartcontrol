package com.suvojeet.smartcontrol

sealed class DiscoveryState {
    object Idle : DiscoveryState()
    object Scanning : DiscoveryState()
    object Success : DiscoveryState()
    object NoDevicesFound : DiscoveryState()
    data class Error(val message: String) : DiscoveryState()
}
