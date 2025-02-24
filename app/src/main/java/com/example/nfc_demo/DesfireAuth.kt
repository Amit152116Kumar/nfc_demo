package com.example.nfc_demo

import android.nfc.tech.IsoDep
import android.util.Log
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Implements AuthenticateEV2First for DESFire EV2 cards
 * Provides secure messaging session for admin operations
 */
class DesfireEV2Authenticator(
    private val isoDep: IsoDep,
    private val masterKey: ByteArray = ByteArray(16) { 0x00 } // Default all-zeros key
) {
    companion object {
        // Key types
        const val KEY_TYPE_AES = 0x02.toByte()
        const val STATUS_MORE_DATA = 0xAF.toByte()
        const val STATUS_SUCCESS = 0x00.toByte()
    }

    // Session information
    private var sessionKey: ByteArray? = null
    private var ivSend: ByteArray? = null
    private var ivReceive: ByteArray? = null
    private var cmdCounter = 0

    /**
     * Authenticates with EV2First to establish secure messaging
     * @param keyNumber The key number to authenticate with (0 for master key)
     * @param keyType The key type (default AES)
     * @return true if authentication succeeded, false otherwise
     */
    fun authenticateEV2First(keyNumber: Byte = 0x00, keyType: Byte = KEY_TYPE_AES): Boolean {
        try {
            // Reset session state
            sessionKey = null
            ivSend = null
            ivReceive = null
            cmdCounter = 0

            // 1. Send AuthenticateEV2First command
            val authCommand = DefaultApdu.ins(Instruction.Authentication.EV2First)
                .data(byteArrayOf(keyNumber, keyType)).build()

            val resp1 = isoDep.transceive(authCommand.toByteArray())

            // 2. Check response status - should be 91AF plus challenge data
            if (!checkResponseStatus(resp1, STATUS_MORE_DATA)) {
                Log.d("NFC", "Authentication failed at first step: ${resp1.toHexString()}")
                return false
            }

            // 3. Extract the random challenge from the card (RndB)
            val rndB = resp1.copyOfRange(0, resp1.size - 2)

            // 4. Decrypt the challenge
            val decryptedRndB = decryptAes(rndB, masterKey)

            // 5. Generate our own random challenge (RndA)
            val rndA = generateRandom(16)

            // 6. Rotate RndB left by 1 byte
            val rotatedRndB = rotateLeft(decryptedRndB)

            // 7. Concatenate RndA + rotatedRndB
            val challengeResponse = rndA + rotatedRndB

            // 8. Encrypt the combined challenge
            val encryptedChallengeResponse = encryptAes(challengeResponse, masterKey)

            // 9. Send additional frame with our encrypted response
            val additionalFrame = DefaultApdu.ins(Instruction.Authentication.Step2Auth)
                .data(encryptedChallengeResponse).build()

            val resp2 = isoDep.transceive(additionalFrame.toByteArray())

            // 10. Check response - should be SW1SW2 = 0x9100 plus encrypted data
            if (!checkResponseStatus(resp2, STATUS_SUCCESS)) {
                Log.d("NFC", "Authentication failed at second step: ${resp2.toHexString()}")
                return false
            }

            // 11. Extract and decrypt the card's response (encrypted RndA')
            val encryptedRndA = resp2.copyOfRange(0, resp2.size - 2)
            val decryptedRndA = decryptAes(encryptedRndA, masterKey)

            // 12. Verify the card's response (should be RndA rotated left)
            val expectedRndA = rotateLeft(rndA)
            if (!decryptedRndA.contentEquals(expectedRndA)) {
                Log.d("NFC", "Card response verification failed")
                return false
            }

            // 13. Generate session keys
            setupSessionKeys(rndA, decryptedRndB)

            Log.d("NFC", "Authentication successful - Secure messaging established")
            return true

        } catch (e: Exception) {
            Log.d("NFC", "Authentication failed with exception: ${e.message}")
            e.printStackTrace()
            return false
        }
    }

    /**
     * Sets up session keys based on authentication exchange
     */
    private fun setupSessionKeys(rndA: ByteArray, rndB: ByteArray) {
        // Create session key material (concatenate parts of RndA and RndB)
        val sv1 = byteArrayOf(
            rndA[0], rndA[1], rndA[2], rndA[3],
            rndB[0], rndB[1], rndB[2], rndB[3],
            rndA[4], rndA[5], rndA[6], rndA[7],
            rndB[4], rndB[5], rndB[6], rndB[7]
        )

        val sv2 = byteArrayOf(
            rndA[8], rndA[9], rndA[10], rndA[11],
            rndB[8], rndB[9], rndB[10], rndB[11],
            rndA[12], rndA[13], rndA[14], rndA[15],
            rndB[12], rndB[13], rndB[14], rndB[15]
        )

        // Derive session encryption key
        val md = MessageDigest.getInstance("SHA-256")
        sessionKey = md.digest(sv1).copyOfRange(0, 16)

        // Initial IV values
        ivSend = sv2.copyOf()
        ivReceive = sv2.copyOf()
    }

    /**
     * Sends a command using secure messaging (after authentication)
     * @param command The plain command to send
     * @return The decrypted response
     */
    fun sendSecureCommand(command: ByteArray): ByteArray {
        if (sessionKey == null || ivSend == null || ivReceive == null) {
            throw IllegalStateException("Not authenticated - no secure session established")
        }

        // 1. Calculate MAC and encrypt command data
        val encryptedCommand = prepareSecureCommand(command)

        // 2. Send the secured command
        val encryptedResponse = isoDep.transceive(encryptedCommand)

        // 3. Verify and decrypt the response
        return decryptSecureResponse(encryptedResponse)
    }

    /**
     * Prepares a command with secure messaging
     */
    private fun prepareSecureCommand(command: ByteArray): ByteArray {
        // Implement secure messaging preparation
        // This would include:
        // - Adding command counter
        // - Calculating MAC
        // - Encrypting data

        // Simplified implementation - would need full cryptographic implementation
        // for actual secure messaging

        cmdCounter++
        return command  // Placeholder - real implementation needed
    }

    /**
     * Decrypts and verifies a secure response
     */
    private fun decryptSecureResponse(encryptedResponse: ByteArray): ByteArray {
        // Implement secure messaging response handling
        // This would include:
        // - Validating MAC
        // - Decrypting data

        // Simplified implementation - would need full cryptographic implementation
        return encryptedResponse  // Placeholder - real implementation needed
    }

    /**
     * Encrypt data using AES in CBC mode
     */
    private fun encryptAes(data: ByteArray, key: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/CBC/NoPadding")
        val keySpec = SecretKeySpec(key, "AES")
        val ivSpec = IvParameterSpec(ByteArray(16))

        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
        return cipher.doFinal(data)
    }

    /**
     * Decrypt data using AES in CBC mode
     */
    private fun decryptAes(data: ByteArray, key: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/CBC/NoPadding")
        val keySpec = SecretKeySpec(key, "AES")
        val ivSpec = IvParameterSpec(ByteArray(16))

        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
        return cipher.doFinal(data)
    }

    /**
     * Generate cryptographically secure random bytes
     */
    private fun generateRandom(size: Int): ByteArray {
        val random = ByteArray(size)
        SecureRandom().nextBytes(random)
        return random
    }

    /**
     * Rotate byte array left by one position
     */
    private fun rotateLeft(data: ByteArray): ByteArray {
        val result = ByteArray(data.size)
        System.arraycopy(data, 1, result, 0, data.size - 1)
        result[data.size - 1] = data[0]
        return result
    }

    /**
     * Check if response has expected status code
     */
    private fun checkResponseStatus(response: ByteArray, expectedStatus: Byte): Boolean {
        return response.size >= 2 &&
                response[response.size - 2] == 0x91.toByte() &&
                response[response.size - 1] == expectedStatus
    }
}

/**
 * Example usage for admin operations
 */
fun performAdminOperations(isoDep: IsoDep) {
    // Create authenticator
    val authenticator = DesfireEV2Authenticator(isoDep)

    // Select master application first
    val selectMasterAppCommand = byteArrayOf(
        0x90.toByte(), 0x5A, 0x00, 0x00, 0x03, 0x00, 0x00, 0x00, 0x00
    )
    val selectResponse = isoDep.transceive(selectMasterAppCommand)

    if (selectResponse[selectResponse.size - 2] != 0x91.toByte() ||
        selectResponse[selectResponse.size - 1] != 0x00.toByte()
    ) {
        Log.d("NFC", "Failed to select master application")
        return
    }

    // Authenticate with EV2First using master key (key 0)
    if (authenticator.authenticateEV2First(0x00)) {
        // Examples of admin operations using secure messaging

        // Get application IDs
        val getAppIdsCommand = byteArrayOf(0x90.toByte(), 0x6A, 0x00, 0x00, 0x00)
        val response = authenticator.sendSecureCommand(getAppIdsCommand)

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

        // Create new application
//        val createAppCommand = byteArrayOf(
//            0x90.toByte(), 0xCA.toByte(), 0x00, 0x00, 0x05, 0x01, 0x02, 0x03, 0x0F, 0x05, 0x00
//        )
//        val createAppResponse = authenticator.sendSecureCommand(createAppCommand)

        // Change master key
        // Implementation would depend on secure messaging format
    }
}