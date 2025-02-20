package com.example.nfc_demo

import android.util.Log
import java.nio.ByteBuffer

class ResponseApdu(private val status: Status, private val data: ByteArray?) {

    fun getStatus(): Status {
        return this.status
    }

    fun getData(): ByteArray? {
        return this.data
    }


    fun toByteArray(): ByteArray {
        val buffer = ByteBuffer.allocate((data?.size ?: 0) + 2)
        if (data != null) {
            buffer.put(data)
        }
        buffer.put(status.sw1)
        buffer.put(status.sw2)
        return buffer.array()
    }

    companion object {
        fun fromByteArray(apdu: ByteArray): ResponseApdu {
            require(apdu.size >= 2) { "Invalid Response: Minimum length is 2 bytes" }
            Log.d(ResponseApdu::class.simpleName, apdu.contentHashCode().toString())
            val statusBytes = apdu.takeLast(2).toByteArray()
            // Extract the data (everything except the last two bytes)
            val data = apdu.copyOfRange(0, apdu.size - 2)

            val status = Status.fromBytes(statusBytes)

            return ResponseApdu(status, data)
        }

    }
}
