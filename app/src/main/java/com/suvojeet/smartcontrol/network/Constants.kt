package com.suvojeet.smartcontrol.network

/**
 * Centralized constants for WiZ protocol
 * Single source of truth - no more duplicate definitions! 🎯
 */
object WizConstants {
    /** WiZ UDP port for all communication */
    const val WIZ_PORT = 38899
    
    /** Discovery timeout in milliseconds */
    const val DISCOVERY_TIMEOUT = 5000L
    
    /** Timeout for individual bulb probe */
    const val PROBE_TIMEOUT = 500L
    
    /** Socket receive timeout */
    const val SOCKET_TIMEOUT = 2000
    
    /** Broadcast listen duration */
    const val BROADCAST_LISTEN_DURATION = 3000L
    
    /** IP scan batch size - processes IPs in chunks to avoid network spam */
    const val IP_SCAN_BATCH_SIZE = 50
}
