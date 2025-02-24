package com.example.nfc_demo

import android.nfc.TagLostException
import android.nfc.tech.IsoDep
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.security.InvalidKeyException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.util.Arrays
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

class DESFireAuth {
    private val cipher: Cipher? = null
    private var sessionKey: SecretKey? = null
    private val transactionIdentifier: ByteArray? = null
    private var cmdCounter = 0

    /**
     * Performs non-first EV2 authentication with the DESFire card.
     * This is used for subsequent authentications after the first one is established.
     *
     * @param apdu The APDU communication channel with the card
     * @param keyNo The key number to authenticate with
     * @return true if authentication succeeded, false otherwise
     * @throws CardException if communication with the card fails
     * @throws NoSuchAlgorithmException if required crypto algorithm isn't available
     * @throws InvalidKeyException if the key is invalid
     */
    @Throws(TagLostException::class, NoSuchAlgorithmException::class, InvalidKeyException::class)
    fun authenticateEV2NonFirst(apdu: IsoDep, keyNo: Byte): Boolean {
        check(!(sessionKey == null || transactionIdentifier == null)) { "First authentication not performed" }


        // Increment command counter
        cmdCounter++


        // Step 1: Send authentication command with key number
        val command = byteArrayOf(0x90.toByte(), 0x77.toByte(), keyNo, 0x00, 0x00)
        var response = transmit(apdu, command)

        if (response.size < 17 || response[0] != STATUS_OK) {
            return false
        }


        // Extract card challenge and update TI
        val cardChallenge = Arrays.copyOfRange(response, 1, 17)


        // Step 2: Generate host challenge
        val hostChallenge = generateRandomBytes(16)


        // Step 3: Update transaction identifier
        updateTransactionIdentifier()


        // Step 4: Calculate session key for this authentication
        val sessionKeyBytes = calculateSessionKey(transactionIdentifier, cmdCounter)
        val tempSessionKey: SecretKey = SecretKeySpec(sessionKeyBytes, "AES")


        // Step 5: Encrypt challenges
        cipher!!.init(Cipher.ENCRYPT_MODE, tempSessionKey)
        val cardChallengeEnc = cipher.doFinal(cardChallenge)
        val hostChallengeEnc = cipher.doFinal(hostChallenge)


        // Step 6: Prepare and send second message
        val secondMsg = ByteArray(33)
        System.arraycopy(cardChallengeEnc, 0, secondMsg, 0, 16)
        System.arraycopy(hostChallengeEnc, 0, secondMsg, 16, 16)
        secondMsg[32] = 0x00 // Le

        response = transmit(apdu, concat(byteArrayOf(CMD_ADDITIONAL_FRAME), secondMsg))

        if (response.size < 17 || response[0] != STATUS_OK) {
            return false
        }


        // Step 7: Verify card response (encrypted host challenge)
        val expectedHostChallengeEnc = ByteArray(16)
        System.arraycopy(response, 1, expectedHostChallengeEnc, 0, 16)

        val expectedHostChallenge = cipher.doFinal(expectedHostChallengeEnc)


        // Verify the challenge response
        if (!hostChallenge.contentEquals(expectedHostChallenge)) {
            return false
        }


        // Authentication successful - Update session key
        this.sessionKey = tempSessionKey
        return true
    }

    // Helper methods
    @Throws(TagLostException::class)
    private fun transmit(apdu: IsoDep, command: ByteArray): ByteArray {
        var response = apdu.transceive(command)
        if (response[0] == CMD_MORE_DATA) {
            val baos = ByteArrayOutputStream()
            baos.write(response, 1, response.size - 1)

            val getMoreCommand = byteArrayOf(CMD_ADDITIONAL_FRAME, 0x00)
            while (true) {
                response = apdu.transceive(getMoreCommand)
                if (response[0] != CMD_MORE_DATA) {
                    baos.write(response, 1, response.size - 1)
                    response = baos.toByteArray()
                    break
                }
                baos.write(response, 1, response.size - 1)
            }
        }
        return response
    }

    @Throws(NoSuchAlgorithmException::class)
    private fun generateRandomBytes(length: Int): ByteArray {
        val random = SecureRandom.getInstanceStrong()
        val bytes = ByteArray(length)
        random.nextBytes(bytes)
        return bytes
    }

    private fun updateTransactionIdentifier() {
        // Increment last byte of TI, with overflow
        for (i in transactionIdentifier!!.indices.reversed()) {
            if ((transactionIdentifier[i].toInt() and 0xFF) == 0xFF) {
                transactionIdentifier[i] = 0
            } else {
                transactionIdentifier[i]++
                break
            }
        }
    }

    private fun calculateSessionKey(ti: ByteArray, counter: Int): ByteArray {
        // Implementation depends on specific EV2 protocol details
        // This is a placeholder - real implementation would use CMAC with master key
        try {
            val sha = MessageDigest.getInstance("SHA-256")
            val buffer = ByteBuffer.allocate(ti.size + 4)
            buffer.put(ti)
            buffer.putInt(counter)
            return sha.digest(buffer.array()).copyOf(16) // AES-128 key
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException("SHA-256 not available", e)
        }
    }

    private fun concat(first: ByteArray, second: ByteArray): ByteArray {
        val result = ByteArray(first.size + second.size)
        System.arraycopy(first, 0, result, 0, first.size)
        System.arraycopy(second, 0, result, first.size, second.size)
        return result
    }

    companion object {
        private const val CMD_MORE_DATA = 0xAF.toByte()
        private const val CMD_ADDITIONAL_FRAME = 0xAF.toByte()
        private const val STATUS_OK = 0x00.toByte()
    }
}