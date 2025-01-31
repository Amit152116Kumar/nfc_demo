package com.example.nfc_demo

import android.content.Intent
import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class MyHostApduService : HostApduService() {
    private val TAG: String = javaClass.simpleName


    private var authDetails: String? = null


    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "activity started")

        val intent = Intent(this, SentActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "service started")

        return super.onStartCommand(intent, flags, startId)
    }

    override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray {
        Log.d(TAG, commandApdu.contentToString())
        if (commandApdu == null || commandApdu.size < 4) {
            return buildResponse(null, Status.Failure) // Status: Wrong length
        }

        val command = CommandApdu.fromByteArray(commandApdu)
        Log.d(TAG, command.getInstruction().name + "  " + String(command.getData()))

        return when (command.getInstruction()) {
            Instruction.Select -> handleSelectAid(command.getData())
            Instruction.Authenticate -> handleAuthenticate(command.getData())
            Instruction.Read -> handleReadData(command.getData())
            Instruction.Write -> handleWriteData(command.getData())
        }
    }

    private fun handleSelectAid(data: ByteArray): ByteArray {

        if (data.contentEquals(AID1)) {
            return buildResponse(null, Status.Success)
        }
        return buildResponse(null, Status.Failure)
    }

    private fun handleAuthenticate(data: ByteArray): ByteArray {
        // Perform authentication logic (e.g., check token)
        broadcastMsg("auth", String(data))
        val isAuthenticated: Boolean = authenticateToken(data)
        return if (isAuthenticated) {
            buildResponse("AuthSuccess".toByteArray(), Status.Success) // Success
        } else {
            buildResponse(null, Status.Failure) // Auth failed
        }
    }

    private fun handleReadData(data: ByteArray): ByteArray {
        // Fetch data based on parameters p1/p2
        if (ApduProto.accept && data.toString() == "AuthSuccess") {
            broadcastMsg("read", "Token Sent to \n $authDetails$")
            return buildResponse(
                "Tokens received to \n$authDetails".toByteArray(), Status.Success
            ) // Success
        }
        return buildResponse(null, Status.Failure)
    }

    private fun handleWriteData(data: ByteArray?): ByteArray {
        // Process and store the data
        val success: Boolean = writeData(data)
        return if (success) buildResponse(null, Status.Success) // Success
        else buildResponse(null, Status.Failure)// Write error
    }

    private fun buildResponse(data: ByteArray?, status: Status): ByteArray {
        return ResponseApdu(status, data).toByteArray()
    }

    private fun authenticateToken(data: ByteArray?): Boolean {
        if (data != null) {
            authDetails = String(data)
            return true
        }
        return false
    }

    private fun writeData(data: ByteArray?): Boolean {
        return true
    }

    override fun onDeactivated(reason: Int) {
        Log.d(TAG, "service deactivated")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Activity destroyed")
    }

    private fun broadcastMsg(key: String, value: String) {
        val intent = Intent()
        intent.setAction("HOST_APDU_SERVICE")
        intent.putExtra("key", value)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }


}