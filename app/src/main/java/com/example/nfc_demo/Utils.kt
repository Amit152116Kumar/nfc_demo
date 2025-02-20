package com.example.nfc_demo

import android.nfc.tech.IsoDep
import android.util.Log
import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

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
                Log.e("NFC", "Failed to format the card. Response: ${response.contentHashCode()}")
            }
        } catch (e: Exception) {
            Log.e("NFC", "Error formatting card: ${e.message}")
        }
    }


    fun selectMasterApp(isoDep: IsoDep) {
        val selectMasterApp = byteArrayOf(
            0x90.toByte(),
            0x5A.toByte(),
            0x00,
            0x00,
            0x03,
            0x00,
            0x00,
            0x00, // AID = 000000 (Master Application)
            0x00
        )

        val response = isoDep.transceive(selectMasterApp)
        if (response.last() == 0x00.toByte()) {
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

            Log.d("NFC", "get application response : ${response.contentHashCode()}")
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
        Log.d("NFC", "Create Wallet App Response: ${response.contentHashCode()}")
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
        Log.d("NFC", "Create Client Info File: ${response.contentHashCode()}")
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
        Log.d("NFC", "Create Balance File: ${response.contentHashCode()}")
    }

    fun createTransactionHistoryFile(isoDep: IsoDep) {
        val command = byteArrayOf(
            0x90.toByte(), 0xC1.toByte(), 0x00, 0x00,  // INS: Create Cyclic File
            0x09,  // Length of following data
            0x03,  // File ID (Transaction History)
            0x00, 0x04,  // Record size (4 bytes per transaction)
            0x05,  // Max number of records (5 transactions)
            0x00, 0x00, 0x00, 0x00,  // Access Rights: Read & Write Permissions (Update as needed)
            0x00  // Expected response length
        )
        val response = isoDep.transceive(command)
        Log.d("NFC", "Create Transaction History File: ${response.contentHashCode()}")
    }

    fun writeClientInfo(isoDep: IsoDep, userId: String, name: String) {
        val userData = (userId + ":" + name).toByteArray()
        val apdu = byteArrayOf(
            0x90.toByte(), 0x3D.toByte(), 0x00, 0x00, userData.size.toByte()
        ) + userData

        val response = isoDep.transceive(apdu)
        Log.d("NFC", "Write Client Info Response: ${response.contentHashCode()}")
    }


    fun writeBalance(isoDep: IsoDep, balance: Int) {
        val balanceBytes = ByteBuffer.allocate(4).putInt(balance).array()
        val apdu = byteArrayOf(
            0x90.toByte(), 0xC2.toByte(), 0x00, 0x00, 0x04
        ) + balanceBytes

        val response = isoDep.transceive(apdu)
        Log.d("NFC", "Write Balance Response: ${response.contentHashCode()}")
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
                Log.e("NFC", "Failed to change Master Key. Response: ${response.contentHashCode()}")
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
        Log.d("NFC", "Set Permissions Response: ${response.contentHashCode()}")
    }

    fun deductBalance(isoDep: IsoDep, amount: Int) {
        val amountBytes = ByteBuffer.allocate(4).putInt(amount).array()
        val apdu = byteArrayOf(
            0x90.toByte(), 0xC0.toByte(), 0x00, 0x00, 0x04
        ) + amountBytes

        val response = isoDep.transceive(apdu)
        Log.d("NFC", "Deduct Balance Response: ${response.contentHashCode()}")
    }


    fun logTransaction(isoDep: IsoDep, transaction: String) {
        val txData = transaction.toByteArray()
        val apdu = byteArrayOf(
            0x90.toByte(), 0x3D.toByte(), // Write to cyclic file
            0x00, 0x00, txData.size.toByte()
        ) + txData
        val response = isoDep.transceive(apdu)
        Log.d("NFC", "Transaction Log Response: ${response.contentHashCode()}")
    }

    fun authenticateDesfire(isoDep: IsoDep, key: ByteArray, useAES: Boolean = false): Boolean {
        try {
            // Step 1: Send authentication command (CLA 0x90)
            val authCmd: ByteArray = if (useAES) byteArrayOf(
                0x90.toByte(), 0xAA.toByte(), 0x00, 0x00, 0x00
            ) // AES (0xAA)
            else byteArrayOf(0x90.toByte(), 0x0A.toByte(), 0x00, 0x00, 0x00) // 2K3DES (0x0A)

            val response = isoDep.transceive(authCmd)

            if (response.isEmpty()) {
                Log.e("NFC", "No response from card!")
                return false
            }
            Log.d("NFC", "Response 1 -> ${response.contentHashCode()}")

            val rndB_encrypted = response.copyOfRange(0, response.size - 2) // Remove status bytes
            Log.d("NFC", "Encrypted RND_B: ${rndB_encrypted.contentHashCode()}")

            val rndB = decrypt(rndB_encrypted, key, useAES)
            Log.d("NFC", "Decrypted RND_B: ${rndB?.contentHashCode()}")

            if (rndB == null) return false

            // Rotate RND_B left by 1 byte
            val rotatedRndB = rndB.copyOfRange(1, rndB.size) + byteArrayOf(rndB[0])

            // Generate RND_A (random 8/16-byte number depending on cipher)
            val rndA = ByteArray(if (useAES) 16 else 8)
            SecureRandom().nextBytes(rndA)

            // Concatenate RND_A and rotated RND_B
            val rndA_rndB = rndA + rotatedRndB

            // Encrypt the challenge response
            val encryptedRndA_rndB = encrypt(rndA_rndB, key, useAES) ?: return false

            // Step 2: Send encrypted RND_A + rotated RND_B
            val apduStep2 = byteArrayOf(
                0x90.toByte(), 0xAF.toByte(), 0x00, 0x00, encryptedRndA_rndB.size.toByte()
            ) + encryptedRndA_rndB

            val response2 = isoDep.transceive(apduStep2)

            if (response2.isEmpty()) {
                Log.e("NFC", "Authentication Step 2 Failed!")
                return false
            }

            Log.d("NFC", "Response 2 -> ${response.contentHashCode()}")

            val rndA_encrypted = response2.copyOfRange(0, response2.size - 2) // Remove status bytes
            Log.d("NFC", "Encrypted RND_A: ${rndA_encrypted.contentHashCode()}")

            val decryptedRndA = decrypt(rndA_encrypted, key, useAES)
            Log.d("NFC", "Decrypted RND_A: ${decryptedRndA?.contentHashCode()}")

            // Verify RND_A by rotating left and checking
            if (decryptedRndA != null) {
                val rotatedRndA =
                    decryptedRndA.copyOfRange(1, decryptedRndA.size) + decryptedRndA[0]
                if (rotatedRndA contentEquals rndA) {
                    Log.d("NFC", "Authentication Successful! Secure session established.")
                    return true // Authentication successful
                } else {
                    Log.e("NFC", "Authentication Failed! RND_A verification failed.")
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isoDep.close()
        }
        return false
    }

    // Encryption function (DES or AES)
    private fun encrypt(data: ByteArray, key: ByteArray, useAES: Boolean): ByteArray? {
        return try {
            val cipher =
                if (useAES) Cipher.getInstance("AES/ECB/NoPadding") else Cipher.getInstance("DESede/ECB/NoPadding")
            val secretKey: SecretKey = SecretKeySpec(key, if (useAES) "AES" else "DESede")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            cipher.doFinal(data)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Decryption function (DES or AES)
    private fun decrypt(data: ByteArray, key: ByteArray, useAES: Boolean): ByteArray? {
        return try {
            val cipher =
                if (useAES) Cipher.getInstance("AES/ECB/NoPadding") else Cipher.getInstance("DESede/ECB/NoPadding")
            val secretKey: SecretKey = SecretKeySpec(key, if (useAES) "AES" else "DESede")
            cipher.init(Cipher.DECRYPT_MODE, secretKey)
            cipher.doFinal(data)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun authenticateMasterKey(isoDep: IsoDep, masterKey: ByteArray) {
        try {
            val keyNo: Byte = 0x00 // Master Key Number

            // Step 1: Send Authentication Request (0xAA for AES)
            val apdu = byteArrayOf(
                0x90.toByte(), 0xAA.toByte(), // AES Authentication Command
                keyNo, 0x00, 0x00 // Key Number, No Le
            )
            val response = isoDep.transceive(apdu)

            if (response.isEmpty()) {
                Log.e("NFC", "No response from card!")
                return
            }

            // Step 2: Decrypt Encrypted RND_B from Card
            val rndB_encrypted = response.copyOf(response.size - 2) // Remove status bytes
            Log.d("NFC", "Encrypted RND_B: ${rndB_encrypted.contentHashCode()}")

            val rndB = decryptAES(rndB_encrypted, masterKey)
            Log.d("NFC", "Decrypted RND_B: ${rndB.contentHashCode()}")

            // Step 3: Generate RND_A (16 random bytes)
            val rndA = ByteArray(16).apply { SecureRandom().nextBytes(this) }
            Log.d("NFC", "Generated RND_A: ${rndA.contentHashCode()}")

            // Step 4: Rotate RND_B Left (RND_B') and concatenate with RND_A
            val rndB_rotated = rotateLeft(rndB)
            val rndAB = rndA + rndB_rotated
            Log.d("NFC", "RND_A | RND_B': ${rndAB.contentHashCode()}")

            // Step 5: Encrypt RND_A | RND_B' using AES Master Key
            val rndAB_encrypted = encryptAES(rndAB, masterKey)

            // Step 6: Send back encrypted RND_A | RND_B'
            val apduStep2 =
                byteArrayOf(0x90.toByte(), 0xAF.toByte(), 0x00, 0x00, 0x20) + rndAB_encrypted
            val responseStep2 = isoDep.transceive(apduStep2)

            if (responseStep2.isEmpty()) {
                Log.e("NFC", "Authentication Step 2 Failed!")
                return
            }

            // Step 7: Verify the returned RND_A'
            val rndA_encrypted = responseStep2.copyOf(responseStep2.size - 2) // Remove status bytes
            val rndA_rotated = decryptAES(rndA_encrypted, masterKey)

            if (rndA_rotated.contentEquals(rotateLeft(rndA))) {
                Log.d("NFC", "Authentication Successful! Secure session established.")
            } else {
                Log.e("NFC", "Authentication Failed! RND_A verification failed.")
            }

        } catch (e: Exception) {
            Log.e("NFC", "Error during AES Master Key Authentication: ${e.message}")
        }
    }

    // Rotate bytes left by 1 position
    fun rotateLeft(input: ByteArray): ByteArray {
        return input.copyOfRange(1, input.size) + input[0]
    }

    // AES Decrypt (ECB Mode, No IV)
    fun decryptAES(data: ByteArray, key: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/ECB/NoPadding")
        val keySpec = SecretKeySpec(key, "AES")
        cipher.init(Cipher.DECRYPT_MODE, keySpec)
        return cipher.doFinal(data)
    }

    // AES Encrypt (ECB Mode, No IV)
    fun encryptAES(data: ByteArray, key: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/ECB/NoPadding")
        val keySpec = SecretKeySpec(key, "AES")
        cipher.init(Cipher.ENCRYPT_MODE, keySpec)
        return cipher.doFinal(data)
    }


    fun readBalance(isoDep: IsoDep): Int {
        try {
            val apdu = byteArrayOf(
                0x90.toByte(), 0x6C.toByte(), // Get Value (Read Balance)
                0x02, 0x00, 0x04 // File ID (0x02 assumed), No Le
            )

            val response = isoDep.transceive(apdu)

            if (response.size < 4) {
                Log.e("NFC", "Read Balance Failed! Response: ${response.contentHashCode()}")
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
                Log.e("NFC", "Read Transactions Failed! Response: ${response.contentHashCode()}")
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
                Log.e("NFC", "Read Client Info Failed! Response: ${response.contentHashCode()}")
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