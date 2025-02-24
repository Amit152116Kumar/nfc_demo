package com.example.nfc_demo

import org.junit.Assert.assertEquals
import org.junit.Test
import java.security.SecureRandom

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun generateAESKey() {
        val key = ByteArray(16) // 16-byte AES key
        SecureRandom().nextBytes(key)
        println(key.toHexString())
    }

    @Test
    fun commandApdu() {
        val cmdApdu =
            DefaultApdu.ins(Instruction.SelectApplication).data(AID.MASTER_FILE.value).build().toByteArray()
        println(cmdApdu.toHexString())
    }

}