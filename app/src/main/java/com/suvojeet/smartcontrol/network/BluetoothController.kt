package com.suvojeet.smartcontrol.network

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log

import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BluetoothController @Inject constructor(@ApplicationContext private val context: Context) {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val adapter: BluetoothAdapter? = bluetoothManager.adapter
    private var isScanning = false
    private val handler = Handler(Looper.getMainLooper())

    // Callback for discovered devices
    var onDeviceFound: ((DiscoveredBulb) -> Unit)? = null

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val scanRecord = result.scanRecord
            val name = device.name ?: scanRecord?.deviceName ?: "Unknown Device"
            val serviceUuids = scanRecord?.serviceUuids ?: emptyList()
            
            Log.d("BluetoothController", "Scanned: $name - ${device.address} - UUIDs: $serviceUuids")
            
            // Tuya devices use Service UUID 0xFD50
            val isTuyaDevice = serviceUuids.any { it.uuid.toString().contains("fd50", ignoreCase = true) }
            
            // Broadened filter to catch more potential smart lights
            // Wipro lights might advertise as "Wipro", "WiZ", "Smart Light", etc.
            // OR if they have the Tuya Service UUID
            if (name.contains("WiZ", ignoreCase = true) || 
                name.contains("Wipro", ignoreCase = true) ||
                name.contains("Smart", ignoreCase = true) ||
                name.contains("Light", ignoreCase = true) ||
                name.contains("Bulb", ignoreCase = true) ||
                name.contains("LED", ignoreCase = true) ||
                isTuyaDevice) {
                
                val discovered = DiscoveredBulb(
                    ipAddress = device.address, // Use MAC as IP for BLE devices temporarily
                    macAddress = device.address,
                    name = if (isTuyaDevice && name == "Unknown Device") "Wipro/Tuya Light" else name,
                    isBle = true
                )
                onDeviceFound?.invoke(discovered)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e("BluetoothController", "Scan failed: $errorCode")
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        if (adapter?.isEnabled == true && !isScanning) {
            isScanning = true
            adapter.bluetoothLeScanner?.startScan(scanCallback)
            
            // Stop scan after 10 seconds
            handler.postDelayed({
                stopScan()
            }, 10000)
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (isScanning && adapter?.isEnabled == true) {
            isScanning = false
            adapter.bluetoothLeScanner?.stopScan(scanCallback)
        }
    }

    private var bluetoothGatt: android.bluetooth.BluetoothGatt? = null
    private var activeDeviceAddress: String? = null

    private val gattCallback = object : android.bluetooth.BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: android.bluetooth.BluetoothGatt, status: Int, newState: Int) {
            if (newState == android.bluetooth.BluetoothProfile.STATE_CONNECTED) {
                Log.d("BluetoothController", "Connected to ${gatt.device.address}")
                gatt.discoverServices()
            } else if (newState == android.bluetooth.BluetoothProfile.STATE_DISCONNECTED) {
                Log.d("BluetoothController", "Disconnected from ${gatt.device.address}")
                if (activeDeviceAddress == gatt.device.address) {
                    bluetoothGatt = null
                    activeDeviceAddress = null
                }
            }
        }

        override fun onServicesDiscovered(gatt: android.bluetooth.BluetoothGatt, status: Int) {
            if (status == android.bluetooth.BluetoothGatt.GATT_SUCCESS) {
                Log.d("BluetoothController", "Services discovered")
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun connect(address: String) {
        if (activeDeviceAddress == address && bluetoothGatt != null) return
        
        disconnect() // Disconnect existing
        
        val device = adapter?.getRemoteDevice(address)
        bluetoothGatt = device?.connectGatt(context, false, gattCallback)
        activeDeviceAddress = address
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        activeDeviceAddress = null
    }

    // Placeholder UUIDs - these need to be replaced with actual WiZ/Wipro UUIDs
    // For now, these are generic/example UUIDs
    private val SERVICE_UUID = java.util.UUID.fromString("00001000-0000-1000-8000-00805f9b34fb") // Example
    private val CHAR_CONTROL_UUID = java.util.UUID.fromString("00001001-0000-1000-8000-00805f9b34fb") // Example

    @SuppressLint("MissingPermission")
    private fun writeCharacteristic(command: ByteArray) {
        val gatt = bluetoothGatt ?: return
        val service = gatt.getService(SERVICE_UUID) ?: return
        val characteristic = service.getCharacteristic(CHAR_CONTROL_UUID) ?: return
        
        characteristic.value = command
        gatt.writeCharacteristic(characteristic)
    }

    fun setPower(isOn: Boolean) {
        // Example command structure
        val command = if (isOn) byteArrayOf(0x01) else byteArrayOf(0x00)
        writeCharacteristic(command)
    }

    fun setBrightness(value: Int) {
        val command = byteArrayOf(0x02, value.toByte())
        writeCharacteristic(command)
    }

    fun setColor(r: Int, g: Int, b: Int) {
        val command = byteArrayOf(0x03, r.toByte(), g.toByte(), b.toByte())
        writeCharacteristic(command)
    }

    fun setTemperature(temp: Int) {
        // Map temp (e.g. 2700-6500) to byte
        val command = byteArrayOf(0x04, (temp / 100).toByte())
        writeCharacteristic(command)
    }
}
