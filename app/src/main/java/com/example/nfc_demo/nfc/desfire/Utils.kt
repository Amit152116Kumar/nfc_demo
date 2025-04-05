package com.example.nfc_demo.nfc.desfire

import android.nfc.tech.IsoDep
import android.util.Log
import java.nio.ByteBuffer

object Utils {


    val masterKey =
        byteArrayOf(79, 68, -98, 29, -77, -35, -66, 15, 89, -19, 13, -106, 125, -99, -32, 23);
    val defaultKey = ByteArray(16) { 0x00 }

    fun formatMifareCard(isoDep: IsoDep) {
        try {
            // Format the card (0xFC = FormatPICC command)
            val apdu = byteArrayOf(0x90.toByte(), 0xFC.toByte(), 0x00, 0x00, 0x00)
            val response = isoDep.transceive(apdu)
            if (response.last() == 0x00.toByte()) {
                Log.d("NFC", "Card formatted successfully!")
            } else {
                Log.e("NFC", "Failed to format the card. Response: ${response.toHexString()}")
            }
        } catch (e: Exception) {
            Log.e("NFC", "Error formatting card: ${e.message}")
        }
    }


    fun selectMasterApp(isoDep: IsoDep) {
        val cmdApdu =
            DefaultApdu.ins(Instruction.SelectApplication).data(AID.MASTER_FILE.value).build()

        val response = cmdApdu.sendAPDU(isoDep)
        if (response.status == Status.Success) {
            Log.d("NFC", "Master application selected!")
        } else {
            Log.e("NFC", "Failed to select master application.")
        }
    }

    fun getApplications(isoDep: IsoDep) {
        try {
            val getApplicationsCmd = byteArrayOf(
                0x90.toByte(), 0xCA.toByte(), // Get Applications
                0x00, 0x00, 0x00 // No parameters
            )

            val response = isoDep.transceive(getApplicationsCmd)

            Log.d("NFC", "get application response : ${response.toHexString()}")
            if (response.isNotEmpty() && response.size > 2) {
                val statusBytes = response.copyOfRange(response.size - 2, response.size)
                if (statusBytes.contentEquals(byteArrayOf(0x91.toByte(), 0x00.toByte()))) {
                    val aidList = response.copyOf(response.size - 2) // Remove status bytes

                    for (i in aidList.indices step 3) {
                        val aid = aidList.sliceArray(i until i + 3)
                        val application = aid.joinToString("") { "%02X".format(it) }
                        Log.d("NFC", "Application idx $i : $application")
                    }

                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun createWalletApp(isoDep: IsoDep) {
        val apdu = byteArrayOf(
            0x90.toByte(),
            0xCA.toByte(),
            0x00,
            0x00,
            0x05,
            0x12,
            0x34,
            0x56, // Unique AID (example: 0x123456)
            0x0F, // Key Settings (AES encryption)
            0x00  // No additional options
        )
        val response = isoDep.transceive(apdu)
        Log.d("NFC", "Create Wallet App Response: ${response.toHexString()}")
    }


    fun createClientInfoFile(isoDep: IsoDep) {
        val command = byteArrayOf(
            0x90.toByte(), 0xCD.toByte(), 0x00, 0x00, 0x07, 0x01,  // File ID = 0x01
            0x00,  // Standard Data File
            0x00,  // Plain communication
            0x0E, 0x0E, 0x0E, 0x00,  // Access rights (Read, Write, Change)
            0x00, 0x20  // File size = 32 bytes
        )
        val response = isoDep.transceive(command)
        Log.d("NFC", "Create Client Info File: ${response.toHexString()}")
    }

    fun createBalanceFile(isoDep: IsoDep) {
        val command = byteArrayOf(
            0x90.toByte(), 0xCC.toByte(), 0x00, 0x00, 0x09, 0x02,  // File ID = 0x02
            0x00,  // Plain communication
            0x0E, 0x0E, 0x0E, 0x00,  // Access rights
            0x00, 0x00, 0x00, 0x00,  // Lower Limit (0)
            0x00, 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte()   // Upper Limit (16777215)
        )
        val response = isoDep.transceive(command)
        Log.d("NFC", "Create Balance File: ${response.toHexString()}")
    }

    fun createTransactionHistoryFile(isoDep: IsoDep) {
        val command = byteArrayOf(
            0x90.toByte(), 0xC1.toByte(), 0x00, 0x00,  // Instruction: Create Cyclic File
            0x09,  // Length of following data
            0x03,  // File ID (Transaction History)
            0x00, 0x04,  // Record size (4 bytes per transaction)
            0x05,  // Max number of records (5 transactions)
            0x00, 0x00, 0x00, 0x00,  // Access Rights: Read & Write Permissions (Update as needed)
            0x00  // Expected response length
        )
        val response = isoDep.transceive(command)
        Log.d("NFC", "Create Transaction History File: ${response.toHexString()}")
    }

    fun writeClientInfo(isoDep: IsoDep, userId: String, name: String) {
        val userData = (userId + ":" + name).toByteArray()
        val apdu = byteArrayOf(
            0x90.toByte(), 0x3D.toByte(), 0x00, 0x00, userData.size.toByte()
        ) + userData

        val response = isoDep.transceive(apdu)
        Log.d("NFC", "Write Client Info Response: ${response.toHexString()}")
    }


    fun writeBalance(isoDep: IsoDep, balance: Int) {
        val balanceBytes = ByteBuffer.allocate(4).putInt(balance).array()
        val apdu = byteArrayOf(
            0x90.toByte(), 0xC2.toByte(), 0x00, 0x00, 0x04
        ) + balanceBytes

        val response = isoDep.transceive(apdu)
        Log.d("NFC", "Write Balance Response: ${response.toHexString()}")
    }


    fun changeMasterKey(isoDep: IsoDep, oldKey: ByteArray, newKey: ByteArray) {
        try {
            val apdu = byteArrayOf(
                0x90.toByte(), 0xC4.toByte(), 0x00, 0x00, 0x11
            ) + oldKey + newKey + byteArrayOf(0x00)

            val response = isoDep.transceive(apdu)
            if (response.last() == 0x00.toByte()) {
                Log.d("NFC", "Master Key changed successfully!")
            } else {
                Log.e("NFC", "Failed to change Master Key. Response: ${response.toHexString()}")
            }
        } catch (e: Exception) {
            Log.e("NFC", "Error changing Master Key: ${e.message}")
        }
    }


    fun setPermissions(isoDep: IsoDep) {
        val apdu = byteArrayOf(
            0x90.toByte(), 0x5F.toByte(), // Set permissions command
            0x00, 0x00, 0x03, // Balance file (requires authentication)
            0x01 // Only authenticated users can write
        )
        val response = isoDep.transceive(apdu)
        Log.d("NFC", "Set Permissions Response: ${response.toHexString()}")
    }

    fun deductBalance(isoDep: IsoDep, amount: Int) {
        val amountBytes = ByteBuffer.allocate(4).putInt(amount).array()
        val apdu = byteArrayOf(
            0x90.toByte(), 0xC0.toByte(), 0x00, 0x00, 0x04
        ) + amountBytes

        val response = isoDep.transceive(apdu)
        Log.d("NFC", "Deduct Balance Response: ${response.toHexString()}")
    }


    fun logTransaction(isoDep: IsoDep, transaction: String) {
        val txData = transaction.toByteArray()
        val apdu = byteArrayOf(
            0x90.toByte(), 0x3D.toByte(), // Write to cyclic file
            0x00, 0x00, txData.size.toByte()
        ) + txData
        val response = isoDep.transceive(apdu)
        Log.d("NFC", "Transaction Log Response: ${response.toHexString()}")
    }


    fun readBalance(isoDep: IsoDep): Int {
        try {
            val apdu = byteArrayOf(
                0x90.toByte(), 0x6C.toByte(), // Get Value (Read Balance)
                0x02, 0x00, 0x04 // File ID (0x02 assumed), No Le
            )

            val response = isoDep.transceive(apdu)

            if (response.size < 4) {
                Log.e("NFC", "Read Balance Failed! Response: ${response.toHexString()}")
                return -1
            }

            return ByteBuffer.wrap(response.copyOf(4)).int
        } catch (e: Exception) {
            Log.e("NFC", "Error Reading Balance: ${e.message}")
            return -1
        }
    }

    fun readTransactions(isoDep: IsoDep, fileID: Byte, length: Byte): String {
        try {
            val apdu = byteArrayOf(
                0x90.toByte(), 0xBD.toByte(), // Read Data command
                fileID, 0x00, length // File ID, No offset, Read length bytes
            )

            val response = isoDep.transceive(apdu)

            if (response.isEmpty() || response.last() != 0x00.toByte()) {
                Log.e("NFC", "Read Transactions Failed! Response: ${response.toHexString()}")
                return "Error Reading Transactions"
            }

            return String(
                response.copyOf(response.size - 2), Charsets.UTF_8
            ) // Exclude status bytes
        } catch (e: Exception) {
            Log.e("NFC", "Error Reading Transactions: ${e.message}")
            return "Read Error"
        }
    }


    fun readClientInfo(isoDep: IsoDep): String {
        try {
            val apdu = byteArrayOf(
                0x90.toByte(), 0xBD.toByte(), // Read Data command
                0x00, 0x00, 0x20.toByte(),    // Offset 0x00, Read 32 bytes
                0x00                          // Le (expected length)
            )

            val response = isoDep.transceive(apdu)

            // Check response status (last 2 bytes should be 0x91 0x00 for success)
            if (response.size < 2 || response[response.size - 2] != 0x91.toByte() || response[response.size - 1] != 0x00.toByte()) {
                Log.e("NFC", "Read Client Info Failed! Response: ${response.toHexString()}")
                return "Error Reading Client Info"
            }

            // Convert response data to string (excluding status bytes)
            val clientInfo = String(response.copyOf(response.size - 2), Charsets.UTF_8)

            Log.d("NFC", "Client Info Read Successfully: $clientInfo")
            return clientInfo

        } catch (e: Exception) {
            Log.e("NFC", "Error Reading Client Info: ${e.message}")
            return "Read Error"
        }
    }

}