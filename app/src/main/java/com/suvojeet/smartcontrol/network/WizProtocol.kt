package com.suvojeet.smartcontrol.network

import com.google.gson.annotations.SerializedName

/**
 * Type-safe data classes for WiZ protocol
 * No more hardcoded JSON strings! 🛡️
 */

/**
 * Base WiZ request structure
 */
data class WizRequest(
    val method: String,
    val params: Any
)

/**
 * Registration/Discovery parameters
 */
data class RegistrationParams(
    val phoneMac: String = "AAAAAAAAAAAA",
    val register: Boolean = false
)

/**
 * SetPilot parameters for controlling bulb state
 */
data class SetPilotParams(
    val state: Boolean? = null,
    val dimming: Int? = null,
    val temp: Int? = null,
    val r: Int? = null,
    val g: Int? = null,
    val b: Int? = null,
    val c: Int? = null,
    val w: Int? = null,
    val speed: Int? = null,
    val sceneId: Int? = null
)

/**
 * GetPilot parameters (usually empty)
 */
class GetPilotParams

/**
 * WiZ response structure
 */
data class WizResponse(
    val method: String? = null,
    val env: String? = null,
    val result: WizResult? = null,
    val error: WizError? = null
)

/**
 * Result object in WiZ response
 */
data class WizResult(
    val mac: String? = null,
    val rssi: Int? = null,
    val src: String? = null,
    val state: Boolean? = null,
    val sceneId: Int? = null,
    val temp: Int? = null,
    val dimming: Int? = null,
    val r: Int? = null,
    val g: Int? = null,
    val b: Int? = null,
    val c: Int? = null,
    val w: Int? = null,
    val speed: Int? = null
)

/**
 * Error object in WiZ response
 */
data class WizError(
    val code: Int,
    val message: String
)

/**
 * Helper object to create common WiZ requests
 */
object WizRequestBuilder {
    
    fun registrationRequest(): WizRequest {
        return WizRequest(
            method = "registration",
            params = RegistrationParams()
        )
    }
    
    fun getPilotRequest(): WizRequest {
        return WizRequest(
            method = "getPilot",
            params = GetPilotParams()
        )
    }
    
    fun setPilotRequest(params: SetPilotParams): WizRequest {
        return WizRequest(
            method = "setPilot",
            params = params
        )
    }
    
    fun turnOn(brightness: Int? = null, temp: Int? = null): WizRequest {
        return setPilotRequest(
            SetPilotParams(
                state = true,
                dimming = brightness,
                temp = temp
            )
        )
    }
    
    fun turnOff(): WizRequest {
        return setPilotRequest(
            SetPilotParams(state = false)
        )
    }
    
    fun setBrightness(brightness: Int): WizRequest {
        return setPilotRequest(
            SetPilotParams(dimming = brightness)
        )
    }
    
    fun setColor(r: Int, g: Int, b: Int): WizRequest {
        return setPilotRequest(
            SetPilotParams(r = r, g = g, b = b)
        )
    }
    
    fun setTemperature(temp: Int, brightness: Int? = null): WizRequest {
        return setPilotRequest(
            SetPilotParams(temp = temp, dimming = brightness)
        )
    }
    
    fun setScene(sceneId: Int): WizRequest {
        return setPilotRequest(
            SetPilotParams(sceneId = sceneId)
        )
    }
}
