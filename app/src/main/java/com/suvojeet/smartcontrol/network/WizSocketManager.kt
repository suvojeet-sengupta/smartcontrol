package com.suvojeet.smartcontrol.network

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class WizSocketManager @Inject constructor() {

    private var socket: DatagramSocket? = null
    private val mutex = Mutex()

    private fun getSocket(): DatagramSocket {
        if (socket == null || socket?.isClosed == true) {
            socket = DatagramSocket()
            socket?.soTimeout = 3000 // Default timeout
        }
        return socket!!
    }

    suspend fun sendPacket(data: ByteArray, ip: String, port: Int) {
        mutex.withLock {
            try {
                val address = InetAddress.getByName(ip)
                val packet = DatagramPacket(data, data.size, address, port)
                getSocket().send(packet)
            } catch (e: Exception) {
                e.printStackTrace()
                closeSocket()
            }
        }
    }

    suspend fun sendAndReceive(data: ByteArray, ip: String, port: Int, timeout: Int = 3000): DatagramPacket? {
        mutex.withLock {
            return try {
                val socket = getSocket()
                socket.soTimeout = timeout
                
                val address = InetAddress.getByName(ip)
                val sendPacket = DatagramPacket(data, data.size, address, port)
                socket.send(sendPacket)
                
                val buffer = ByteArray(1024)
                val receivePacket = DatagramPacket(buffer, buffer.size)
                socket.receive(receivePacket)
                
                // Simple verification: check if response is from the target IP
                if (receivePacket.address.hostAddress == ip) {
                    receivePacket
                } else {
                    null
                }
            } catch (e: java.net.SocketTimeoutException) {
                null
            } catch (e: Exception) {
                e.printStackTrace()
                if (e !is java.net.SocketTimeoutException) {
                    closeSocket()
                }
                null
            }
        }
    }

    private fun closeSocket() {
        try {
            socket?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            socket = null
        }
    }
}
