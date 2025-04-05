package com.example.nfc_demo.weigand


// Add this dependency to your build.gradle
// implementation 'com.github.mik3y:usb-serial-for-android:3.4.0'

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.nfc_demo.databinding.ActivityWeigandBinding
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.util.SerialInputOutputManager
import kotlinx.coroutines.Runnable
import okhttp3.internal.toImmutableList
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class UsbWeigandActivity : AppCompatActivity() {
    private val TAG = "UsbWeigandActivity"
    private lateinit var binding: ActivityWeigandBinding
    private var usbManager: UsbManager? = null
    private var usbConnection: UsbDeviceConnection? = null
    private var usbSerialPort: UsbSerialPort? = null
    private var serialIoManager: SerialInputOutputManager? = null

    private val ACTION_USB_PERMISSION = "com.example.nfc_demo.USB_PERMISSION"
    private val BAUD_RATE = 9600 // Adjust based on your converter PCB specifications

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val device: UsbDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
            } else {
                @Suppress("DEPRECATION") intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
            }
            when (intent.action) {
                ACTION_USB_PERMISSION -> {
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        device?.let {
                            updateStatus("Permission granted for device: ${it.deviceName}")
                            connectToSerialDevice(device)
                        }
                    } else {
                        updateStatus("Permission denied for device")
                    }
                }

                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    device?.let {
                        updateStatus("USB device attached: ${it.deviceName}")
                        findSerialDevice()
                    }
                }

                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    device?.let {
                        updateStatus("USB device detached: ${it.deviceName}")
                        closeSerialPort()
                    }
                }
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWeigandBinding.inflate(layoutInflater)
        setContentView(binding.root)

        usbManager = getSystemService(Context.USB_SERVICE) as UsbManager

        // Register for USB permission and connection events
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(usbReceiver, filter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(usbReceiver, filter)
        }

        // Initialize UI
        binding.scanButton.setOnClickListener {
            findSerialDevice()
        }

        binding.sendButton.setOnClickListener {
            val facilityCode = binding.facilityCodeInput.text.toString()
            val cardNumber = binding.cardNumberInput.text.toString()

            if (validateInput(facilityCode, cardNumber)) {
                sendWiegandData(facilityCode, cardNumber)
            } else {
                updateStatus("Invalid input. Facility code and card number must be 12 characters or less.")
            }
        }

        // Initial device scan
        findSerialDevice()
    }

    private fun validateInput(facilityCode: String, cardNumber: String): Boolean {
        return facilityCode.length <= 12 && cardNumber.length <= 12
    }

    private fun findSerialDevice() {
        updateStatus("Scanning for USB serial devices...")

        // Find all available USB devices that can be used as serial ports
        val availableDrivers =
            UsbSerialProber.getDefaultProber().findAllDrivers(usbManager).toImmutableList()

        if (availableDrivers.isEmpty()) {
            updateStatus("No USB serial devices found.")
            val devices = usbManager?.deviceList
            if (!devices.isNullOrEmpty()) {
                updateStatus("Available devices:")
                for (device in devices.values) {
                    updateStatus("- ${device.deviceName}")
                }
            } else {
                updateStatus("No USB devices found.")
            }
            return
        }

        updateStatus("Found ${availableDrivers.size} USB serial device(s):")

        // For each device, show details and request permission
        for (driver in availableDrivers) {
            val device = driver.device
            val vendorId = String.format("0x%04X", device.vendorId)
            val productId = String.format("0x%04X", device.productId)

            updateStatus("Device: ${device.deviceName}")
            updateStatus("- Vendor ID: $vendorId, Product ID: $productId")
            updateStatus("- Port count: ${driver.ports.size}")

            // Request permission to use this device
            val permissionIntent = PendingIntent.getBroadcast(
                this,
                0,
                Intent(ACTION_USB_PERMISSION),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PendingIntent.FLAG_MUTABLE
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }
            )

            if (usbManager?.hasPermission(device) == true) {
                updateStatus("Already have permission for this device")
                connectToSerialDevice(device)
            } else {
                updateStatus("Requesting permission for this device")
                usbManager?.requestPermission(device, permissionIntent)
            }
        }
    }

    private fun connectToSerialDevice(device: UsbDevice) {
        try {
            // Find appropriate driver for this device
            val driver = UsbSerialProber.getDefaultProber().probeDevice(device)

            if (driver == null) {
                updateStatus("No suitable driver found for this device")
                return
            }

            // Open a connection to the first available port
            usbConnection = usbManager?.openDevice(device)

            if (usbConnection == null) {
                updateStatus("Failed to open connection")
                return
            }

            usbSerialPort = driver.ports[0] // Most devices have just one port (port 0)

            try {
                usbSerialPort?.open(usbConnection)
                usbSerialPort?.setParameters(
                    BAUD_RATE,
                    UsbSerialPort.DATABITS_8,
                    UsbSerialPort.STOPBITS_1,
                    UsbSerialPort.PARITY_NONE
                )

                updateStatus("Serial connection established with ${device.deviceName}")
                binding.connectionStatus.text = "Connected"
                binding.connectionStatus.setTextColor(
                    resources.getColor(
                        android.R.color.holo_green_dark, null
                    )
                )

                // Start the I/O manager to handle incoming data
                startSerialIoManager()

            } catch (e: Exception) {
                updateStatus("Error setting up serial port: ${e.message}")
                closeSerialPort()
            }

        } catch (e: Exception) {
            updateStatus("Error connecting to device: ${e.message}")
        }
    }

    private fun startSerialIoManager() {
        usbSerialPort?.let { port ->
            serialIoManager =
                SerialInputOutputManager(port, object : SerialInputOutputManager.Listener {
                    override fun onNewData(data: ByteArray) {
                        runOnUiThread {
                            updateStatus("Received: ${data.joinToString(", ") { it.toString(16) }}")
                        }
                    }

                    override fun onRunError(e: Exception) {
                        runOnUiThread {
                            updateStatus("Error: ${e.message}")
                        }
                    }
                })
            val runnable: Runnable = Runnable {
                try {
                    serialIoManager?.run {
                        start()
                        updateStatus("I/O manager started")
                    } ?: run {
                        updateStatus("I/O manager is null")
                    }
                } catch (e: Exception) {
                    updateStatus("Error in I/O manager: ${e.message}")
                }
            }
            Executors.newSingleThreadExecutor().submit(runnable)
        }
    }

    private fun sendWiegandData(facilityCode: String, cardNumber: String) {
        if (usbSerialPort == null || !usbSerialPort!!.isOpen) {
            updateStatus("Serial port not open")
            return
        }

        try {
            // Format data according to your converter PCB's requirements
            // This is an example - adjust according to your specific protocol
            val formattedData = formatWiegandData(facilityCode, cardNumber)

            // Send the data

            updateStatus("Data sending: FC=$facilityCode, Card#=$cardNumber")
            usbSerialPort?.write(formattedData, 1000)
            updateStatus("Data sent")

        } catch (e: Exception) {
            updateStatus("Error sending data: ${e.message}")
        }
    }

    private fun formatWiegandData(facilityCode: String, cardNumber: String): ByteArray {
        // This is a placeholder for your specific formatting logic
        // You'll need to format the data according to your converter PCB's specifications

        // Example: Simple comma-separated values followed by newline
        val dataString = "$facilityCode,$cardNumber\n"
        return dataString.toByteArray()
    }

    private fun closeSerialPort() {
        serialIoManager?.stop()
        serialIoManager = null

        try {
            usbSerialPort?.close()
        } catch (ignored: Exception) {
        }

        usbSerialPort = null

        try {
            usbConnection?.close()
        } catch (ignored: Exception) {
        }

        usbConnection = null

        binding.connectionStatus.text = "Disconnected"
        binding.connectionStatus.setTextColor(
            resources.getColor(
                android.R.color.holo_red_dark, null
            )
        )

        updateStatus("Connection closed")
    }

    private fun updateStatus(message: String) {
        Log.d(TAG, message)
        runOnUiThread {
            val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            val timestamp = sdf.format(Date())
            val logMessage = "$timestamp: $message\n"


            // Append the message to the log TextView
            binding.statusText.append(logMessage)

            // Auto-scroll to bottom
            val scrollView = binding.statusScrollView
            scrollView.post { scrollView.fullScroll(android.view.View.FOCUS_DOWN) }
        }
    }

    override fun onDestroy() {
        closeSerialPort()
        unregisterReceiver(usbReceiver)
        super.onDestroy()
    }
}