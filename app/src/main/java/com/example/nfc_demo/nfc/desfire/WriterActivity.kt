package com.example.nfc_demo.nfc.desfire

import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import com.example.nfc_demo.databinding.ActivityWriterBinding


class WriterActivity : AppCompatActivity(), NfcAdapter.ReaderCallback {

    private lateinit var binding: ActivityWriterBinding
    private lateinit var nfcAdapter: NfcAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWriterBinding.inflate(layoutInflater)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

    }

    override fun onResume() {
        super.onResume()
        nfcAdapter.enableReaderMode(
            this,
            this,
            NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
            null
        )
    }


    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (NfcAdapter.ACTION_TECH_DISCOVERED == intent.action) {
            val tag = IntentCompat.getParcelableExtra(intent, NfcAdapter.EXTRA_TAG, Tag::class.java)
            tag?.let { processNfcTag(it) }
        }
    }

    override fun onTagDiscovered(tag: Tag?) {
        if (tag != null) {
            processNfcTag(tag)
        }
    }

    private fun processNfcTag(tag: Tag) {
        Log.d("NFC", "TAG Description: ${tag.techList}")
        val isoDep = IsoDep.get(tag)
        isoDep?.use {
            it.connect()
            Log.d("NFC", "Connected to MIFARE DESFire card")
            Log.d(
                "NFC",
                "timout: ${it.timeout}, max Transeive Length: ${it.maxTransceiveLength}, Historical Bytes: ${it.historicalBytes.toHexString()} "
            )
            Log.d("NFC", "Extened Length Supported: ${it.isExtendedLengthApduSupported}")
            // Perform operations here
            runOnUiThread {
                binding.infoTxt.text = "Writing to the Smart Card"
            }
            performAdminOperations(it)
//            selectMasterApp(it)
//            authenticateDesfire(it, defaultKey, false)
//            getApplications(it)
//            createWalletApp(it)
//            createClientInfoFile(it)
//            createBalanceFile(it)
//            createTransactionHistoryFile(it)
//            writeClientInfo(it, "J325UDSK7", "Amit Kumar")
//            writeBalance(it, 5000)
//            setPermissions(it)

//            Log.d("NFC", "oldKey -> $defaultKey , newKey -> $masterKey")
//            changeMasterKey(it, defaultKey, masterKey)
        }
    }


}