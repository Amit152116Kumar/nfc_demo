package com.example.nfc_demo.access_management

import android.app.PendingIntent
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.nfc_demo.R
import com.example.nfc_demo.databinding.ActivityAccessVerificationBinding
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AccessVerificationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAccessVerificationBinding
    private var nfcAdapter: NfcAdapter? = null
    private var pendingIntent: PendingIntent? = null

    private var requiredDepartmentCode: Int = 0
    private var currentLocationCode: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAccessVerificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        binding.accessSetupMode.setOnClickListener {
            val intent = Intent(this, AccessSetupActivity::class.java)
            startActivity(intent)
        }
        setupNfc()
        setupSpinners()
    }

    private fun setupNfc() {
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        if (nfcAdapter == null) {
            Snackbar.make(binding.root, "NFC is not available on this device", Snackbar.LENGTH_LONG)
                .show()
            return
        }

        if (!nfcAdapter!!.isEnabled) {
            Snackbar.make(
                binding.root,
                "Please enable NFC to use this feature",
                Snackbar.LENGTH_LONG
            ).show()
        }

        // Create a PendingIntent to handle NFC intents
        val intent = Intent(this, javaClass).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_MUTABLE)
    }

    private fun setupSpinners() {
        // Department Spinner
        val departmentAdapter = ArrayAdapter(
            this,
            R.layout.spinner_item,
            Department.DEPARTMENTS.map { it.name }
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.requiredDepartmentSpinner.adapter = departmentAdapter

        // Location Spinner
        val locationAdapter = ArrayAdapter(
            this,
            R.layout.spinner_item,
            BaseLocation.LOCATIONS.map { it.name }
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.currentLocationSpinner.adapter = locationAdapter

        // Set listeners to update the codes when selection changes
        binding.requiredDepartmentSpinner.setOnItemSelectedListener { position ->
            requiredDepartmentCode = Department.DEPARTMENTS[position].code
        }

        binding.currentLocationSpinner.setOnItemSelectedListener { position ->
            currentLocationCode = BaseLocation.LOCATIONS[position].code
        }

        // Initialize with first values
        requiredDepartmentCode = Department.DEPARTMENTS[0].code
        currentLocationCode = BaseLocation.LOCATIONS[0].code
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter?.let {
            it.enableForegroundDispatch(this, pendingIntent, null, null)
        }
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        if (NfcAdapter.ACTION_TAG_DISCOVERED == intent.action ||
            NfcAdapter.ACTION_TECH_DISCOVERED == intent.action ||
            NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action
        ) {

            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            tag?.let {
                processNfcTag(it, intent)
            }
        }
    }

    private fun processNfcTag(tag: Tag, intent: Intent) {
        try {
            // Get raw data from the tag
            val rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
            if (rawMessages != null) {
                val ndefMessages = rawMessages.map { it as NdefMessage }
                if (ndefMessages.isNotEmpty()) {
                    val payload = ndefMessages[0].records[0].payload

                    // Try to parse as employee card
                    if (payload.size >= 22) { // Expected size of our data
                        val employeeCard = parseEmployeeCard(payload)
                        verifyAccess(employeeCard)
                    } else {
                        showInvalidCardError()
                    }
                } else {
                    showInvalidCardError()
                }
            } else {
                showInvalidCardError()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            showInvalidCardError()
        }
    }

    private fun parseEmployeeCard(data: ByteArray): EmployeeCard {
        // Company Code (4 bytes)
        var companyCode = 0
        for (i in 0..3) {
            companyCode = (companyCode shl 8) or (data[i].toInt() and 0xFF)
        }

        // Employee Code (10 bytes) - ASCII encoding
        val employeeCode = data.copyOfRange(4, 14).toString(Charsets.US_ASCII).trim()

        // Sequence Code (1 byte)
        val sequenceCode = data[14].toInt() and 0xFF

        // Department Code (2 bytes)
        val departmentCode = ((data[15].toInt() and 0xFF) shl 8) or (data[16].toInt() and 0xFF)

        // Base Location Code (1 byte)
        val baseLocationCode = data[17].toInt() and 0xFF

        // Access Code (4 bytes)
        val locationCode = data[18].toInt() and 0xFF
        val permissionByte2 = data[19].toInt() and 0xFF
        val permissionByte3 = data[20].toInt() and 0xFF

        // Extract permissions from bytes 2 and 3
        val permissions = mutableSetOf<Int>()

        // Process byte 2 (first 8 permissions)
        for (bit in 0..7) {
            if ((permissionByte2 and (1 shl bit)) != 0) {
                permissions.add(0x0001 + bit) // Department codes start from 0x0001
            }
        }

        // Process byte 3 (next 4 permissions, bits 0-3)
        for (bit in 0..3) {
            if ((permissionByte3 and (1 shl bit)) != 0) {
                permissions.add(0x0009 + bit) // Continue from 0x0009
            }
        }

        val accessCode = AccessCode(locationCode, permissions)

        return EmployeeCard(
            companyCode = companyCode,
            employeeCode = employeeCode,
            sequenceCode = sequenceCode,
            departmentCode = departmentCode,
            baseLocationCode = baseLocationCode,
            accessCode = accessCode
        )
    }

    private fun verifyAccess(card: EmployeeCard) {
        // Check if the employee has access to the required department at the current location
        val hasAccess = card.accessCode.toByteArray().let { bytes ->
            // First check if the location code matches
            val locationMatches = bytes[0].toInt() and 0xFF == currentLocationCode

            // Then check if has the required department permission
            var hasDepartmentPermission = false

            // Department permissions are spread across bytes 1 and 2
            when {
                requiredDepartmentCode <= 0x0008 -> {
                    // First byte, bits 0-7
                    val bit = requiredDepartmentCode - 0x0001
                    hasDepartmentPermission = (bytes[1].toInt() and (1 shl bit)) != 0
                    bit
                }

                requiredDepartmentCode <= 0x000C -> {
                    // Second byte, bits 0-3
                    val bit = requiredDepartmentCode - 0x0009
                    hasDepartmentPermission = (bytes[2].toInt() and (1 shl bit)) != 0
                    bit + 8
                }

                else -> -1
            }

            locationMatches && hasDepartmentPermission
        }

        if (hasAccess) {
            showAccessGranted(card)
        } else {
            showAccessDenied(card)
        }
    }

    private fun showAccessGranted(card: EmployeeCard) {
        val view = LayoutInflater.from(this).inflate(R.layout.layout_access_granted, null)

        // Find employee details
        val companyName =
            Company.COMPANIES.find { it.code == card.companyCode }?.name ?: "Unknown Company"
        val departmentName = Department.DEPARTMENTS.find { it.code == card.departmentCode }?.name
            ?: "Unknown Department"

        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        // Set text views
        view.findViewById<TextView>(R.id.employeeDetailsTextView).text =
            "${card.employeeCode}\n$companyName\n$departmentName"
        view.findViewById<TextView>(R.id.timestampTextView).text = timestamp

        // Show the result
        binding.accessResultContainer.removeAllViews()
        binding.accessResultContainer.addView(view)
        binding.accessResultContainer.visibility = View.VISIBLE
        binding.instructionsTextView.visibility = View.GONE
        binding.nfcImageView.visibility = View.GONE
    }

    private fun showAccessDenied(card: EmployeeCard) {
        val view = LayoutInflater.from(this).inflate(R.layout.layout_access_denied, null)

        // Find employee details
        val companyName =
            Company.COMPANIES.find { it.code == card.companyCode }?.name ?: "Unknown Company"
        val departmentName = Department.DEPARTMENTS.find { it.code == card.departmentCode }?.name
            ?: "Unknown Department"

        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        // Set text views
        view.findViewById<TextView>(R.id.employeeDetailsTextView).text =
            "${card.employeeCode}\n$companyName\n$departmentName"

        val requiredDeptName =
            Department.DEPARTMENTS.find { it.code == requiredDepartmentCode }?.name ?: "Unknown"
        view.findViewById<TextView>(R.id.reasonTextView).text =
            "No permission for $requiredDeptName at this location"

        view.findViewById<TextView>(R.id.timestampTextView).text = timestamp

        // Show the result
        binding.accessResultContainer.removeAllViews()
        binding.accessResultContainer.addView(view)
        binding.accessResultContainer.visibility = View.VISIBLE
        binding.instructionsTextView.visibility = View.GONE
        binding.nfcImageView.visibility = View.GONE
    }

    private fun showInvalidCardError() {
        Snackbar.make(binding.root, "Invalid or incompatible NFC card", Snackbar.LENGTH_LONG).show()
    }
}
