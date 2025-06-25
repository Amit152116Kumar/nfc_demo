package com.example.nfc_demo.access_management

import android.content.Intent
import android.nfc.NfcAdapter
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.nfc_demo.R
import com.example.nfc_demo.databinding.ActivityAccessSetupBinding
import com.google.android.material.snackbar.Snackbar

class AccessSetupActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAccessSetupBinding
    private lateinit var nfcUtils: NfcUtils
    private var nfcWriteDialog: AlertDialog? = null

    private var currentEmployeeCard: EmployeeCard? = null
    private val selectedPermissions = mutableSetOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAccessSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        nfcUtils = NfcUtils(this)
        // Configure FlexboxLayout
        // Configure FlexboxLayout with reduced spacing
        binding.selectedPermissionsContainer.apply {
            flexWrap = com.google.android.flexbox.FlexWrap.WRAP
            flexDirection = com.google.android.flexbox.FlexDirection.ROW
            justifyContent = com.google.android.flexbox.JustifyContent.FLEX_START
            alignItems = com.google.android.flexbox.AlignItems.CENTER
            // Use proper methods to control spacing
            alignContent = com.google.android.flexbox.AlignContent.FLEX_START
            alignItems = com.google.android.flexbox.AlignItems.FLEX_START
        }

        setupSpinners()
        setupButtons()
        checkNfcAvailability()
    }

    private fun setupSpinners() {
        // Company Spinner with custom layout
        val companyAdapter = ArrayAdapter(
            this,
            R.layout.spinner_item,
            Company.COMPANIES.map { it.name }
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.companySpinner.adapter = companyAdapter

        // Department Spinner
        val departmentAdapter = ArrayAdapter(
            this,
            R.layout.spinner_item,
            Department.DEPARTMENTS.map { it.name }
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.departmentSpinner.adapter = departmentAdapter

        // Base Location Spinner
        val locationAdapter = ArrayAdapter(
            this,
            R.layout.spinner_item,
            BaseLocation.LOCATIONS.map { it.name }
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.baseLocationSpinner.adapter = locationAdapter
    }

    private fun setupButtons() {
        binding.generateButton.setOnClickListener {
            if (validateInputs()) {
                generateEmployeeCard()
            }
        }

        binding.writeToNfcButton.setOnClickListener {
            if (currentEmployeeCard != null) {
                if (nfcUtils.isNfcEnabled()) {
                    Snackbar.make(
                        binding.root,
                        "Please tap NFC tag to write data",
                        Snackbar.LENGTH_LONG
                    ).show()
                } else {
                    Snackbar.make(
                        binding.root,
                        "NFC is disabled. Please enable NFC in your settings",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            } else {
                Snackbar.make(
                    binding.root,
                    "Please generate access code first",
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }

        binding.selectPermissionsButton.setOnClickListener {
            showPermissionSelectionDialog()
        }

        binding.writeToNfcButton.setOnClickListener {
            if (currentEmployeeCard != null) {
                if (nfcUtils.isNfcEnabled()) {
                    showNfcWriteDialog()
                } else {
                    Snackbar.make(
                        binding.root,
                        "NFC is disabled. Please enable NFC in your settings",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            } else {
                Snackbar.make(
                    binding.root,
                    "Please generate access code first",
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun validateInputs(): Boolean {
        val employeeCode = binding.employeeCodeEditText.text.toString()
        if (employeeCode.isBlank()) {
            showError("Please enter employee code")
            binding.employeeCodeEditText.requestFocus()
            return false
        }

        val sequenceCodeStr = binding.sequenceCodeEditText.text.toString()
        if (sequenceCodeStr.isBlank()) {
            showError("Please enter sequence code")
            binding.sequenceCodeEditText.requestFocus()
            return false
        }

        return true
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    private fun showPermissionSelectionDialog() {
        val departmentNames = Department.DEPARTMENTS.map { it.name }.toTypedArray()
        val departmentCodes = Department.DEPARTMENTS.map { it.code }.toIntArray()

        // Create boolean array of currently selected items
        val selectedItems = BooleanArray(departmentNames.size) { position ->
            selectedPermissions.contains(departmentCodes[position])
        }

        AlertDialog.Builder(this)
            .setTitle("Select Access Permissions")
            .setMultiChoiceItems(departmentNames, selectedItems) { _, position, isChecked ->
                val departmentCode = departmentCodes[position]
                if (isChecked) {
                    selectedPermissions.add(departmentCode)
                } else {
                    selectedPermissions.remove(departmentCode)
                }
            }
            .setPositiveButton("OK") { _, _ ->
                updateSelectedPermissionsText()
            }
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Clear All") { _, _ ->
                selectedPermissions.clear()
                updateSelectedPermissionsText()
            }
            .show()
            .also { dialog ->
                // Add Select All button
                dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnLongClickListener {
                    // Select all permissions
                    Department.DEPARTMENTS.forEach {
                        selectedPermissions.add(it.code)
                    }
                    dialog.dismiss()
                    updateSelectedPermissionsText()
                    true
                }
            }
    }

    private fun updateSelectedPermissionsText() {
        val selectedNames = Department.DEPARTMENTS
            .filter { selectedPermissions.contains(it.code) }

        binding.selectedPermissionsContainer.removeAllViews()

        if (selectedNames.isEmpty()) {
            binding.noPermissionsSelectedText.visibility = View.VISIBLE
            binding.selectedPermissionsContainer.visibility = View.GONE
        } else {
            binding.noPermissionsSelectedText.visibility = View.GONE
            binding.selectedPermissionsContainer.visibility = View.VISIBLE

            // Create layout params with minimal vertical margins
            val layoutParams = com.google.android.flexbox.FlexboxLayout.LayoutParams(
                com.google.android.flexbox.FlexboxLayout.LayoutParams.WRAP_CONTENT,
                com.google.android.flexbox.FlexboxLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                // Keep horizontal margins but reduce vertical margins to zero
                setMargins(4, 0, 4, 0)
            }

            for (department in selectedNames) {
                val chip = layoutInflater.inflate(
                    R.layout.item_permission_chip,
                    binding.selectedPermissionsContainer,
                    false
                ) as com.google.android.material.chip.Chip

                chip.text = department.name
                chip.setOnCloseIconClickListener {
                    selectedPermissions.remove(department.code)
                    updateSelectedPermissionsText()
                }

                // Apply custom layout params with minimal vertical margins
                chip.layoutParams = layoutParams

                binding.selectedPermissionsContainer.addView(chip)
            }
        }
    }

    private fun checkNfcAvailability() {
        if (!nfcUtils.isNfcAvailable()) {
            Snackbar.make(
                binding.root,
                "NFC is not available on this device",
                Snackbar.LENGTH_LONG
            ).show()
            binding.writeToNfcButton.isEnabled = false
        }
    }

    private fun generateEmployeeCard() {
        try {
            // Get selected company
            val companyPosition = binding.companySpinner.selectedItemPosition
            val company = Company.COMPANIES[companyPosition]

            // Get selected department
            val departmentPosition = binding.departmentSpinner.selectedItemPosition
            val department = Department.DEPARTMENTS[departmentPosition]

            // Get selected location
            val locationPosition = binding.baseLocationSpinner.selectedItemPosition
            val location = BaseLocation.LOCATIONS[locationPosition]

            val employeeCode = binding.employeeCodeEditText.text.toString()
            val sequenceCode = binding.sequenceCodeEditText.text.toString().toInt()

            // Create access code
            val accessCode = AccessCode(location.code, selectedPermissions)

            // Create employee card
            currentEmployeeCard = EmployeeCard(
                companyCode = company.code,
                employeeCode = employeeCode,
                sequenceCode = sequenceCode,
                departmentCode = department.code,
                baseLocationCode = location.code,
                accessCode = accessCode
            )

            // Display result
            binding.resultCard.visibility = View.VISIBLE

            // Format result text
            val hexData = currentEmployeeCard!!.toNfcData().joinToString("") {
                String.format("%02X", it)
            }
            binding.resultTextView.text = """
                Company: ${company.name} (${String.format("0x%08X", company.code)})
                Employee Code: $employeeCode
                Sequence Code: $sequenceCode
                Department: ${department.name} (${String.format("0x%04X", department.code)})
                Base Location: ${location.name} (${String.format("0x%02X", location.code)})
                Access Code: ${accessCode.toHexString()}
                
                NFC Data (Hex): $hexData
            """.trimIndent()

        } catch (e: Exception) {
            showError("Error generating card data: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun showNfcWriteDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_nfc_write, null)
        val alertDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        alertDialog.show()

        // Store the dialog reference to dismiss it when NFC writing is complete
        val dialogTag = "nfc_write_dialog"
        supportFragmentManager.findFragmentByTag(dialogTag)?.let {
            if (it is androidx.fragment.app.DialogFragment) {
                it.dismissAllowingStateLoss()
            }
        }

        // Store the dialog in a field so we can dismiss it when NFC write completes
        this.nfcWriteDialog = alertDialog
    }

    override fun onResume() {
        super.onResume()
        nfcUtils.enableForegroundDispatch()
    }

    override fun onPause() {
        super.onPause()
        nfcUtils.disableForegroundDispatch()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (NfcAdapter.ACTION_TAG_DISCOVERED == intent.action) {
            val tag = intent.getParcelableExtra<android.nfc.Tag>(NfcAdapter.EXTRA_TAG)
            tag?.let {
                currentEmployeeCard?.let { card ->
                    // Dismiss the NFC write dialog
                    nfcWriteDialog?.dismiss()
                    nfcWriteDialog = null

                    val success = nfcUtils.writeEmployeeCardToTag(it, card)
                    if (success) {
                        Snackbar.make(
                            binding.root,
                            "Successfully wrote data to NFC tag",
                            Snackbar.LENGTH_LONG
                        ).show()
                    } else {
                        Snackbar.make(
                            binding.root,
                            "Failed to write data to NFC tag",
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }
}
