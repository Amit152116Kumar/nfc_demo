package com.example.nfc_demo.weigand

import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.nfc_demo.databinding.ActivityWeigandBinding
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class WifiWiegandActivity : AppCompatActivity() {

    private var wifiManager: WifiManager? = null
    private var socket: DatagramSocket? = null
    private var serverAddress: InetAddress? = null
    private var isConnected = false

    private lateinit var binding: ActivityWeigandBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWeigandBinding.inflate(layoutInflater)
        setContentView(binding.root)


        // Enable scrolling for status text
        binding.statusText.movementMethod = ScrollingMovementMethod()

        // Initialize WiFi manager
        wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager

        // Set button click listeners
        binding.scanButton.setOnClickListener { connectToESP32() }

        binding.sendButton.setOnClickListener { sendWiegandData() }

        // Update UI initially
        updateConnectionStatus(false)
    }

    override fun onDestroy() {
        super.onDestroy()
        closeSocket()
    }

    /**
     * Connects to the ESP32 WiFi network and establishes a UDP socket connection
     */
    private fun connectToESP32() {
        updateLog("Attempting to connect to ESP32...")


        // Start connection in a separate thread to avoid blocking UI
        Thread {
            try {
                // Connect to ESP32 WiFi network
                connectToWifi()


                // Create UDP socket
                if (socket == null || socket!!.isClosed) {
                    socket = DatagramSocket()
                    socket!!.soTimeout = SOCKET_TIMEOUT
                }


                // Send connection request to ESP32
                serverAddress =
                    InetAddress.getByName(ESP32_IP)
                sendConnectionRequest()
            } catch (e: Exception) {
                updateLog("Error: " + e.message)
                e.printStackTrace()
                runOnUiThread {
                    updateConnectionStatus(false)
                    Toast.makeText(
                        this@WifiWiegandActivity,
                        "Connection failed", Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }.start()
    }

    /**
     * Connects to the ESP32 WiFi network
     */
    private fun connectToWifi() {
        updateLog("Connecting to WiFi: $ESP32_SSID")


        // Configure WiFi connection
        val wifiNetworkSpecifier = WifiNetworkSpecifier.Builder()
            .setSsid(ESP32_SSID)
            .setWpa2Passphrase(ESP32_PASSWORD)
            .build()

        // Enable WiFi if it's not enabled
        if (!wifiManager!!.isWifiEnabled) {
//            val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
//            startActivity(intent)
//            updateLog("Prompting user to enable WiFi")
            wifiManager!!.setWifiEnabled(true)
        }

        val networkRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .setNetworkSpecifier(wifiNetworkSpecifier)
            .build()

        // Wait for connection to establish
        updateLog("Waiting for WiFi connection...")

        val connectivityManager =
            getSystemService(CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        connectivityManager.requestNetwork(
            networkRequest,
            object : android.net.ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: android.net.Network) {
                    super.onAvailable(network)
                    connectivityManager.bindProcessToNetwork(network)
                    updateLog("Connected to WiFi: $ESP32_SSID")

                }

                override fun onUnavailable() {
                    super.onUnavailable()
                    updateLog("Failed to connect to WiFi: $ESP32_SSID")
                }
            }
        )
    }

    /**
     * Sends a connection request to the ESP32
     */
    @Throws(JSONException::class, IOException::class)
    private fun sendConnectionRequest() {
        val requestObj = JSONObject()
        requestObj.put("message_type", "connection_request")
        val request = requestObj.toString()

        updateLog("Sending connection request to ESP32")


        // Send the request multiple times as per the protocol
        for (i in 0 until MESSAGE_REPEAT_COUNT) {
            sendUdpMessage(request)
            try {
                Thread.sleep(500) // Wait 500ms between retries
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }


        // Listen for response
        listenForConnectionResponse()
    }

    /**
     * Listens for a connection response from the ESP32
     */
    private fun listenForConnectionResponse() {
        try {
            val buffer = ByteArray(1024)
            val packet = DatagramPacket(buffer, buffer.size)

            updateLog("Waiting for connection response...")

            try {
                socket!!.receive(packet)
                val response = String(packet.data, 0, packet.length)
                updateLog("Received: $response")

                val responseObj = JSONObject(response)
                if (responseObj.getString("message_type") == "connection_response" && responseObj.getString(
                        "status"
                    ) == "connected"
                ) {
                    isConnected = true
                    runOnUiThread {
                        updateConnectionStatus(true)
                        Toast.makeText(
                            this@WifiWiegandActivity,
                            "Connected to ESP32", Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: SocketTimeoutException) {
                updateLog("Timeout waiting for response")
                runOnUiThread {
                    updateConnectionStatus(false)
                    Toast.makeText(
                        this@WifiWiegandActivity,
                        "Connection timed out", Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } catch (e: Exception) {
            updateLog("Error in connection response: " + e.message)
            e.printStackTrace()
        }
    }

    /**
     * Sends Wiegand data to the ESP32
     */
    private fun sendWiegandData() {
        if (!isConnected) {
            Toast.makeText(this, "Not connected to ESP32", Toast.LENGTH_SHORT).show()
            return
        }

        val facilityCode = binding.facilityCodeInput.text.toString().trim { it <= ' ' }
        val cardNumber = binding.cardNumberInput.text.toString().trim { it <= ' ' }

        if (facilityCode.isEmpty() || cardNumber.isEmpty()) {
            Toast.makeText(
                this, "Please enter both facility code and card number",
                Toast.LENGTH_SHORT
            ).show()
            return
        }


        // Start data transmission in a separate thread
        Thread {
            try {
                // Convert decimal to binary string (16-bit)
                val facilityCodeBinary = convertToBinary(facilityCode, 16)
                val cardNumberBinary = convertToBinary(cardNumber, 16)

                updateLog(
                    "Sending Wiegand data: FC=" + facilityCode +
                            " (" + facilityCodeBinary + "), CN=" + cardNumber +
                            " (" + cardNumberBinary + ")"
                )


                // Create JSON message
                val requestObj = JSONObject()
                requestObj.put("message_type", "wiegand_flash_request")
                requestObj.put("facility_code", facilityCodeBinary)
                requestObj.put("card_number", cardNumberBinary)
                val request = requestObj.toString()


                // Send the request multiple times
                for (i in 0 until MESSAGE_REPEAT_COUNT) {
                    sendUdpMessage(request)
                    try {
                        Thread.sleep(500) // Wait 500ms between retries
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                }


                // Listen for acknowledgment
                listenForWiegandResponse()
            } catch (e: Exception) {
                updateLog("Error sending Wiegand data: " + e.message)
                e.printStackTrace()
            }
        }.start()
    }

    /**
     * Converts a decimal string to a binary string with specified length
     */
    private fun convertToBinary(decimal: String, length: Int): String {
        try {
            val value = decimal.toInt()
            var binary = Integer.toBinaryString(value)


            // Pad with leading zeros if necessary
            while (binary.length < length) {
                binary = "0$binary"
            }


            // Truncate if too long
            if (binary.length > length) {
                binary = binary.substring(binary.length - length)
            }

            return binary
        } catch (e: NumberFormatException) {
            // If input is not a valid number, return all zeros
            val zeros = StringBuilder()
            var i = 0
            while (i < length) {
                zeros.append("0")
                i++
            }
            return zeros.toString()
        }
    }

    /**
     * Listens for a Wiegand response from the ESP32
     */
    private fun listenForWiegandResponse() {
        try {
            val buffer = ByteArray(1024)
            val packet = DatagramPacket(buffer, buffer.size)

            updateLog("Waiting for Wiegand response...")

            try {
                socket!!.receive(packet)
                val response = String(packet.data, 0, packet.length)
                updateLog("Received: $response")

                val responseObj = JSONObject(response)
                if (responseObj.getString("message_type") == "wiegand_flash_response" && responseObj.getString(
                        "status"
                    ) == "received"
                ) {
                    updateLog("ESP32 received data successfully")


                    // Wait for flash confirmation
                    listenForWiegandFlashedConfirmation()
                }
            } catch (e: SocketTimeoutException) {
                updateLog("Timeout waiting for Wiegand response")
                runOnUiThread {
                    Toast.makeText(
                        this@WifiWiegandActivity,
                        "Wiegand response timeout", Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } catch (e: Exception) {
            updateLog("Error in Wiegand response: " + e.message)
            e.printStackTrace()
        }
    }

    /**
     * Listens for a Wiegand flashed confirmation from the ESP32
     */
    private fun listenForWiegandFlashedConfirmation() {
        try {
            val buffer = ByteArray(1024)
            val packet = DatagramPacket(buffer, buffer.size)

            updateLog("Waiting for flash confirmation...")

            try {
                socket!!.receive(packet)
                val response = String(packet.data, 0, packet.length)
                updateLog("Received: $response")

                val responseObj = JSONObject(response)
                if (responseObj.getString("message_type") == "wiegand_flashed" && responseObj.getString(
                        "status"
                    ) == "flashed"
                ) {
                    updateLog("Wiegand data successfully flashed!")
                    runOnUiThread {
                        Toast.makeText(
                            this@WifiWiegandActivity,
                            "Wiegand data successfully sent", Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: SocketTimeoutException) {
                updateLog("Timeout waiting for flash confirmation")
                runOnUiThread {
                    Toast.makeText(
                        this@WifiWiegandActivity,
                        "Flash confirmation timeout", Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } catch (e: Exception) {
            updateLog("Error in flash confirmation: " + e.message)
            e.printStackTrace()
        }
    }

    /**
     * Sends a UDP message to the ESP32
     */
    @Throws(IOException::class)
    private fun sendUdpMessage(message: String) {
        val sendData = message.toByteArray()
        val sendPacket = DatagramPacket(
            sendData, sendData.size, serverAddress, ESP32_PORT
        )
        socket!!.send(sendPacket)
    }

    /**
     * Updates the connection status UI
     */
    private fun updateConnectionStatus(connected: Boolean) {
        isConnected = connected
        runOnUiThread {
            if (connected) {
                binding.connectionStatus.text = "Connected"
                binding.connectionStatus.setTextColor(
                    resources.getColor(
                        android.R.color.holo_green_dark, null
                    )
                )
                binding.sendButton.isEnabled = true
            } else {
                binding.connectionStatus.text = "Disconnected"
                binding.connectionStatus.setTextColor(
                    resources.getColor(
                        android.R.color.holo_red_dark, null
                    )
                )
                binding.sendButton.isEnabled = false
            }
        }
    }

    /**
     * Updates the log UI with a new message
     */
    private fun updateLog(message: String) {
        Log.d(TAG, message)

        runOnUiThread {
            val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            val timestamp = sdf.format(Date())
            val logMessage = "$timestamp: $message\n"


            // Append the message to the log TextView
            binding.statusText.append(logMessage)


//            // Scroll to the bottom
//            val scrollAmount = (binding.statusText.layout.getLineTop(binding.statusText.lineCount)
//                    - binding.statusText.height)
//            if (scrollAmount > 0) {
//                binding.statusText.scrollTo(0, scrollAmount)
//            } else {
//                binding.statusText.scrollTo(0, 0)
//            }
            // Auto-scroll to bottom
            val scrollView = binding.statusScrollView
            scrollView.post { scrollView.fullScroll(View.FOCUS_DOWN) }
        }
    }

    /**
     * Closes the socket connection
     */
    private fun closeSocket() {
        if (socket != null && !socket!!.isClosed) {
            socket!!.close()
        }
    }

    companion object {
        private const val TAG = "WifiWiegandActivity"
        private const val ESP32_SSID = "UID123xyz#"
        private const val ESP32_PASSWORD = "12345678"
        private const val ESP32_IP = "192.168.4.1"
        private const val ESP32_PORT = 5005
        private const val SOCKET_TIMEOUT = 5000 // 5 seconds
        private const val MESSAGE_REPEAT_COUNT = 3
    }
}