package com.example.nfc_demo.access_management

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import java.io.IOException

class NfcUtils(private val activity: Activity) {
    private val nfcAdapter: NfcAdapter? by lazy { NfcAdapter.getDefaultAdapter(activity) }

    fun isNfcAvailable(): Boolean = nfcAdapter != null

    fun isNfcEnabled(): Boolean = nfcAdapter?.isEnabled == true

    fun enableForegroundDispatch() {
        if (isNfcEnabled()) {
            val intent =
                Intent(activity, activity.javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            val pendingIntent =
                PendingIntent.getActivity(activity, 0, intent, PendingIntent.FLAG_MUTABLE)
            val filters = arrayOf(IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED))
            val techLists =
                arrayOf(arrayOf(Ndef::class.java.name), arrayOf(NdefFormatable::class.java.name))
            nfcAdapter?.enableForegroundDispatch(activity, pendingIntent, filters, techLists)
        }
    }

    fun disableForegroundDispatch() {
        if (isNfcEnabled()) {
            nfcAdapter?.disableForegroundDispatch(activity)
        }
    }

    fun writeEmployeeCardToTag(tag: Tag, employeeCard: EmployeeCard): Boolean {
        val data = employeeCard.toNfcData()
        val ndefRecord = NdefRecord.createMime("application/com.example.accessmanager", data)
        val ndefMessage = NdefMessage(arrayOf(ndefRecord))

        try {
            val ndef = Ndef.get(tag)
            if (ndef != null) {
                ndef.connect()
                if (ndef.isWritable && ndef.maxSize >= ndefMessage.byteArrayLength) {
                    ndef.writeNdefMessage(ndefMessage)
                    return true
                }
            } else {
                val ndefFormatable = NdefFormatable.get(tag)
                ndefFormatable?.let {
                    it.connect()
                    it.format(ndefMessage)
                    return true
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return false
    }
}
