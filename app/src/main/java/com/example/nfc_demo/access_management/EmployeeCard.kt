package com.example.nfc_demo.access_management

data class EmployeeCard(
    val companyCode: Int,
    val employeeCode: String,
    val sequenceCode: Int,
    val departmentCode: Int,
    val baseLocationCode: Int,
    val accessCode: AccessCode
) {
    fun toNfcData(): ByteArray {
        // Calculate byte size: 4 + 10 + 1 + 2 + 1 + 4 = 22 bytes
        val result = ByteArray(22)

        // Company Code (4 bytes)
        for (i in 0..3) {
            result[i] = (companyCode shr (8 * (3 - i))).toByte()
        }

        // Employee Code (10 bytes) - ASCII encoding
        employeeCode.padEnd(10).toByteArray(Charsets.US_ASCII).copyInto(result, 4, 0, 10)

        // Sequence Code (1 byte)
        result[14] = sequenceCode.toByte()

        // Department Code (2 bytes)
        result[15] = (departmentCode shr 8).toByte()
        result[16] = departmentCode.toByte()

        // Base Location Code (1 byte)
        result[17] = baseLocationCode.toByte()

        // Access Code (4 bytes)
        accessCode.toByteArray().copyInto(result, 18)

        return result
    }
}
