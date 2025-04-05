package com.example.nfc_demo.nfc

import android.util.Log
import com.example.nfc_demo.nfc.desfire.Status

class ResponseApdu(val status: Status, val data: ByteArray?) {

    companion object {
        fun fromByteArray(apdu: ByteArray): ResponseApdu {
            require(apdu.size >= 2) { "Invalid Response: Minimum length is 2 bytes" }
            val statusBytes = apdu.takeLast(2).toByteArray()
            // Extract the data (everything except the last two bytes)
            val data = apdu.copyOfRange(0, apdu.size - 2)

            val status = Status.fromBytes(statusBytes)
            Log.d(
                ResponseApdu::class.simpleName,
                "Received status: ${status::class.simpleName} (SW1: ${
                    status.sw1.toUByte().toString(16)
                }, SW2: ${status.sw2.toUByte().toString(16)})"
            )
            return ResponseApdu(status, data)
        }

    }
}
