package com.example.nfc_demo

import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.nfc_demo.databinding.ActivityReceiverBinding


class ReceiverActivity : AppCompatActivity(), NfcAdapter.ReaderCallback {

    private var nfcAdapter: NfcAdapter? = null
    private lateinit var binding: ActivityReceiverBinding
    private val TAG = javaClass.simpleName

    private var authCode: String? = null
    private var tagId: ByteArray? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReceiverBinding.inflate(layoutInflater)
        setContentView(binding.root)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter == null) {
            Toast.makeText(this, "NFC is not available on this device", Toast.LENGTH_SHORT).show();
            finish();
            return
        } else if (!nfcAdapter!!.isEnabled) {
            Toast.makeText(this, "Enable NFC to Receive", Toast.LENGTH_SHORT).show();
            finish()
        }

    }

    override fun onResume() {
        super.onResume()

        val options = Bundle()

        options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250)
        nfcAdapter!!.enableReaderMode(
            this,
            this,
            (NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK),
            options
        )

    }

    override fun onTagDiscovered(tag: Tag?) {
        Log.d(TAG, "TAG discovered")
        val isoDep = IsoDep.get(tag)
        if (isoDep != null) {
            try {
                isoDep.connect()
                tagId = tag?.id
                isoDep.timeout = 5000
                Log.d(
                    TAG,
                    "tagId: ${tagId} isconnected: ${isoDep.isConnected} length: ${isoDep.maxTransceiveLength} timeout: ${isoDep.timeout}"
                )
                var commandApdu = ApduProto.selectCommand
                var responseApdu = sendApduCommand(isoDep, commandApdu.build())


                if (authCode == null) {

                    commandApdu =
                        ApduProto.authenticateCommandBuilder.setData("Name: Amit Kumar\nRTSP: 23095SDK23587SDKJH230987\n".toByteArray())
                    responseApdu = sendApduCommand(isoDep, commandApdu.build())
                    if (responseApdu.getData() != null) {
                        authCode = String(responseApdu.getData()!!)
                    } else {
                        authCode = null
                    }

                } else {
                    commandApdu = ApduProto.readCommandBuilder.setData(authCode!!.toByteArray())
                    responseApdu = sendApduCommand(isoDep, commandApdu.build())
                }


            } catch (e: Exception) {
                Log.e(TAG, e.message.toString())
                e.printStackTrace()
            } finally {
                isoDep.close()
            }
        }
    }


    private fun sendApduCommand(isoDep: IsoDep, commandApdu: CommandApdu): ResponseApdu {
        val responseBytes = isoDep.transceive(commandApdu.toByteArray())
        val responseApdu = ResponseApdu.fromByteArray(responseBytes)
        if (responseApdu.getStatus() == Status.Failure) {
            throw Exception("response failure for ${commandApdu.getInstruction().name}")
        }
        Log.d(
            TAG,
            "Command : ${commandApdu.getInstruction().name} \t Response Data: " + responseApdu.getData()
                .toString()
        )
        return responseApdu
    }


}