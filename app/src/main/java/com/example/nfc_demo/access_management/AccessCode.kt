package com.example.nfc_demo.access_management

data class AccessCode(
    val locationCode: Int, // Byte 1 - Location code
    val accessPermissions: Set<Int> = emptySet() // Department access permissions
) {
    fun toByteArray(): ByteArray {
        // Byte 1: Location Code
        val byte1 = locationCode.toByte()

        // Byte 2 and 3: Access permissions
        var byte2 = 0
        var byte3 = 0

        for (permission in accessPermissions) {
            when (permission) {
                // Byte 2 permissions
                0x0001 -> byte2 = byte2 or (1 shl 0)  // Admin 2 (Bit 1)
                0x0002 -> byte2 = byte2 or (1 shl 1)  // Admin all (Bit 2)
                0x0003 -> byte2 = byte2 or (1 shl 2)  // FRM (Bit 3)
                0x0004 -> byte2 = byte2 or (1 shl 3)  // FRM OSE (Bit 4)
                0x0005 -> byte2 = byte2 or (1 shl 4)  // General (Bit 5)
                0x0006 -> byte2 = byte2 or (1 shl 5)  // Infosec (Bit 6)
                0x0007 -> byte2 = byte2 or (1 shl 6)  // Infosec OSE (Bit 7)
                0x0008 -> byte2 = byte2 or (1 shl 7)  // IT (Bit 8)

                // Byte 3 permissions
                0x0009 -> byte3 = byte3 or (1 shl 0)  // IT only (Bit 1)
                0x000A -> byte3 = byte3 or (1 shl 1)  // Operations (Bit 2)
                0x000B -> byte3 = byte3 or (1 shl 2)  // Operations OSE (Bit 3)
                0x000C -> byte3 = byte3 or (1 shl 3)  // Service Provider (Bit 4)
            }
        }

        // Byte 4: Reserved for Future Use (RFU)
        val byte4 = 0

        return byteArrayOf(byte1, byte2.toByte(), byte3.toByte(), byte4.toByte())
    }

    fun toHexString(): String {
        return toByteArray().joinToString("") {
            String.format("%02X", it)
        }
    }
}
