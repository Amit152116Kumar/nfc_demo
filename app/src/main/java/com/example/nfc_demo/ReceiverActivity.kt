package com.example.nfc_demo

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.NfcA
import android.nfc.tech.NfcF
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import com.example.nfc_demo.Utils.authenticateMasterKey
import com.example.nfc_demo.Utils.masterKey
import com.example.nfc_demo.Utils.readBalance
import com.example.nfc_demo.Utils.readClientInfo
import com.example.nfc_demo.databinding.ActivityReceiverBinding
import java.io.IOException
import java.nio.ByteBuffer


class ReceiverActivity : AppCompatActivity(), NfcAdapter.ReaderCallback {

    private var nfcAdapter: NfcAdapter? = null;
    private lateinit var binding: ActivityReceiverBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReceiverBinding.inflate(layoutInflater)
        setContentView(binding.root)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter == null) {
            Toast.makeText(this, "NFC is not available on this device", Toast.LENGTH_SHORT).show();
            finish();
            return
        }
        if (!nfcAdapter!!.isEnabled) {
            Toast.makeText(this, "Enable NFC to Receive", Toast.LENGTH_SHORT).show();
            finish()
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE
        )
        val filters = arrayOf(
            IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED),
            IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED),
            IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)
        )
        val techList = arrayOf(
            arrayOf(
                NfcF::class.java.name,
                NfcA::class.java.name,
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
            val tag = IntentCompat.getParcelableExtra(intent, NfcAdapter.EXTRA_TAG, Tag::class.java)
            readTagData(tag!!)
        }
    }

    private fun readTagData(tag: Tag) {
        val isoDep = IsoDep.get(tag) ?: return

        try {
            isoDep.connect()
            authenticateMasterKey(isoDep, masterKey)
            binding.balance.text = readBalance(isoDep).toString()
//            binding.transactionLogs.text = readTransactions(isoDep)
            binding.clientInfo.text = readClientInfo(isoDep)


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