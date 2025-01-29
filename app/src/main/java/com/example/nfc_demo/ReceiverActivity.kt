package com.example.nfc_demo

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NfcF
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.nfc_demo.databinding.ActivityReceiverBinding
import java.nio.ByteBuffer


class ReceiverActivity: AppCompatActivity(),NfcAdapter.ReaderCallback {

    private var nfcAdapter:NfcAdapter? = null;
    private lateinit var binding: ActivityReceiverBinding
    private val TAG = javaClass.simpleName

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

    }

    override fun onPause() {
        super.onPause()
        // Disable foreground dispatch when the activity is paused
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onResume() {
        super.onResume()

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

        val options = Bundle()

        // Work around for some broken Nfc firmware implementations that poll the card too fast
        options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250)

        nfcAdapter!!.enableForegroundDispatch(this, pendingIntent, filters, techList)
        nfcAdapter!!.enableReaderMode(
            this, this,
            (NfcAdapter.FLAG_READER_NFC_A or
                    NfcAdapter.FLAG_READER_NFC_B or
                    NfcAdapter.FLAG_READER_NFC_F or
                    NfcAdapter.FLAG_READER_NFC_V or
                    NfcAdapter.FLAG_READER_NFC_BARCODE or
                    NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS),
            options
        )

    }

    override fun onTagDiscovered(tag: Tag?) {
        Log.d(TAG, "TAG discovered")
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
        val buffer = ByteBuffer.allocate(5 + lc)
        buffer.put(cla).put(ins).put(p1).put(p2)
        if (data != null) buffer.put(lc.toByte()).put(data)
        return buffer.array()
    }


    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        if (NfcAdapter.ACTION_TAG_DISCOVERED == intent.action) {
            // Handle NFC tag
            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            Log.d(TAG, "new Intent discovered")
            readTagData(tag!!)
        }
    }

    private fun readTagData(tag: Tag) {
        Log.d(TAG, tag.toString())
        val nDef = Ndef.get(tag) ?: return



        // If we want to read
        // As we did not turn on the NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK
        // We can get the cached Ndef message the system read for us.
        val mNdefMessage: NdefMessage = nDef.cachedNdefMessage
        val record = mNdefMessage.records
        val ndefRecordsCount = record.size
        if (ndefRecordsCount > 0) {

            var ndefText = ""

            for( i in  0..ndefRecordsCount){

                val ndefTnf = record[i].tnf
                val ndefType = record[i].type
                val ndefPayload = record[i].payload
                if (ndefTnf == NdefRecord.TNF_WELL_KNOWN && ndefType.contentEquals(NdefRecord.RTD_TEXT)) {
                    ndefText = String(ndefPayload) + " \n"

                    // ndefText = ndefText + Utils.parseTextrecordPayload(ndefPayload) + " \n";
                    Log.d(TAG, "resultReceive ${ndefPayload.toString()}")
                    binding.receiverTxt.text = ndefPayload.toString()
                }
            }

        }



    }

}