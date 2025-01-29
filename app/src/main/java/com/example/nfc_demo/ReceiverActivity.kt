package com.example.nfc_demo

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.NfcF
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.nfc_demo.databinding.ActivityReceiverBinding
import java.io.IOException
import java.nio.ByteBuffer
import java.util.Arrays


class ReceiverActivity: AppCompatActivity(),NfcAdapter.ReaderCallback {

    private var nfcAdapter:NfcAdapter? = null;
    private lateinit var binding: ActivityReceiverBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReceiverBinding.inflate(layoutInflater)
        setContentView(binding.root)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter==null) {
            Toast.makeText(this, "NFC is not available on this device", Toast.LENGTH_SHORT).show();
            finish();
            return
        }
        if(!nfcAdapter!!.isEnabled){
            Toast.makeText(this, "Enable NFC to Receive", Toast.LENGTH_SHORT).show();
            finish()
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this,javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE
        )
        val filters = arrayOf(
            IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED),
            IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED),
            IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)
        )
        val techList = arrayOf(
            arrayOf(
                NfcF::class.java.name
            )
        )

        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, filters, techList)

    }

    override fun onPause() {
        super.onPause()
        // Disable foreground dispatch when the activity is paused
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onTagDiscovered(tag: Tag?) {
        if (tag != null) {
            readTagData(tag)
        }
    }
    private fun buildCommandApdu(
        cla: Byte,
        ins: Byte,
        p1: Byte,
        p2: Byte,
        data: ByteArray?
    ): ByteArray {
        val lc = if ((data != null)) data.size else 0
        val buffer = ByteBuffer.allocate(4 + lc)
        buffer.put(cla).put(ins).put(p1).put(p2)
        if (data != null) buffer.put(lc.toByte()).put(data)
        return buffer.array()
    }


    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        if (NfcAdapter.ACTION_TAG_DISCOVERED == intent.action) {
            // Handle NFC tag
            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            readTagData(tag!!)
        }
    }

    private fun readTagData(tag: Tag) {
        val isoDep = IsoDep.get(tag) ?: return

        try {
            isoDep.connect()

            // Build APDU Command
            val commandApdu: ByteArray = buildCommandApdu(
                0x00.toByte(),
                0x10.toByte(),
                0x00.toByte(),
                0x00.toByte(),
                "Token123".toByteArray()
            )

            // Send APDU to HCE
            val responseApdu = isoDep.transceive(commandApdu)

            // Parse Response
            if (responseApdu.size >= 2) {
                val sw1 = responseApdu[responseApdu.size - 2]
                val sw2 = responseApdu[responseApdu.size - 1]
                val responseData = Arrays.copyOfRange(responseApdu, 0, responseApdu.size - 2)

                Log.d("APDU", "Response Data: " + String(responseData))
                Log.d("APDU", "Status: " + String.format("%02X%02X", sw1, sw2))
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                isoDep.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

    }

}