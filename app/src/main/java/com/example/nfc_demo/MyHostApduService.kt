package com.example.nfc_demo

import android.content.Intent
import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.util.Log
import java.util.Arrays

class MyHostApduService: HostApduService() {
    private  val TAG: String = javaClass.simpleName
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG,"activity started")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG,"service started")
        return super.onStartCommand(intent, flags, startId)
    }

    override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray {
        if (commandApdu == null || commandApdu.size < 4) {
            return buildResponse(null, 0x6F.toByte(), 0x00.toByte()) // Status: Wrong length
        }

        // Parse APDU components
        val cla = commandApdu[0]
        val ins = commandApdu[1]
        val p1 = commandApdu[2]
        val p2 = commandApdu[3]
        var data: ByteArray? = null

        if (commandApdu.size > 5) {
            val lc = commandApdu[4].toInt()
            data = Arrays.copyOfRange(commandApdu, 5, 5 + lc)
        }
        Log.d(TAG, "processCommand Apdu ins : $ins")
        Log.d(TAG, "processCommand Apdu p1, p2: $p1, $p2")
        Log.d(TAG, "processCommand Apdu data: $data")

        return when (ins) {
            0x10.toByte() -> handleAuthenticate(data)
            0x20.toByte() -> handleReadData(p1, p2)
            0x30.toByte() -> handleWriteData(data)
            else -> buildResponse(
                null,
                0x6D.toByte(),
                0x00.toByte()
            ) // Status: Instruction not supported
        }
    }

    private fun handleAuthenticate(data: ByteArray?): ByteArray {
        // Perform authentication logic (e.g., check token)
        val isAuthenticated: Boolean = authenticateToken(data)
        return if (isAuthenticated) {
            buildResponse("AuthSuccess".toByteArray(), 0x90.toByte(), 0x00.toByte()) // Success
        } else {
            buildResponse(null, 0x63.toByte(), 0x00.toByte()) // Auth failed
        }
    }

    private fun handleReadData(p1: Byte, p2: Byte): ByteArray {
        // Fetch data based on parameters p1/p2
        val responseData: ByteArray = fetchData(p1, p2)
        return buildResponse(responseData, 0x90.toByte(), 0x00.toByte()) // Success
    }

    private fun handleWriteData(data: ByteArray?): ByteArray {
        // Process and store the data
        val success: Boolean = writeData(data)
        return if (success
        ) buildResponse(null, 0x90.toByte(), 0x00.toByte()) // Success
        else buildResponse(null, 0x65.toByte(), 0x00.toByte()) // Write error
    }

    private fun buildResponse(data: ByteArray?, sw1: Byte, sw2: Byte): ByteArray {
        if (data == null) {
            return byteArrayOf(sw1, sw2)
        }
        val response = ByteArray(data.size + 2)
        System.arraycopy(data, 0, response, 0, data.size)
        response[data.size] = sw1
        response[data.size + 1] = sw2
        return response
    }

    private fun authenticateToken(data: ByteArray?): Boolean {
        return true
    }

    private fun writeData(data: ByteArray?):Boolean{
        return true
    }
    private fun fetchData(p1: Byte,p2: Byte):ByteArray{
        return "Amit Kumar".toByteArray()
    }

    override fun onDeactivated(reason: Int) {
        Log.d(TAG,"service deactivated")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG,"Activity destroyed")
    }
}