package com.example.nfc_demo

enum class CLA(val value: Byte) {
    INTER_INDUSTRY(0x00), PROPRIETARY(0x80.toByte()), ISO_7816_4(0xA0.toByte());
}

// Sealed hierarchy for Instruction (INS) categorized
sealed class INS(val value: Byte) {

    sealed class FileManagement(value: Byte) : INS(value) {
        data object SelectFile : FileManagement(0xA4.toByte())
        data object CreateFile : FileManagement(0xE0.toByte())
        data object DeleteFile : FileManagement(0xE4.toByte())
    }

    sealed class Security(value: Byte) : INS(value) {
        data object Verify : Security(0x20.toByte())
        data object ExternalAuthenticate : Security(0x82.toByte())
        data object InternalAuthenticate : Security(0x88.toByte())
        data object GetChallenge : Security(0x84.toByte())
    }

    sealed class DataManagement(value: Byte) : INS(value) {
        data object ReadBinary : DataManagement(0xB0.toByte())
        data object ReadRecord : DataManagement(0xB2.toByte())
        data object UpdateBinary : DataManagement(0xD6.toByte())
        data object WriteBinary : DataManagement(0xD0.toByte())
        data object WriteRecord : DataManagement(0xD2.toByte())
        data object EraseBinary : DataManagement(0x0E.toByte())
    }
}

enum class SelectionFile(val value: Byte) {
    ByFileID(0x00), ByAid(0x04)
}

enum class Occurrence(val value: Byte) {
    First(0x00), Next(0x02)
}

enum class RecordMode(val value: Byte) {
    FirstRecord(0x00), LastRecord(0x01), NextRecord(0x02), PreviousRecord(0x03)
}

enum class RecordAccess(val value: Byte) {
    AbsoluteMode(0x04), NextAvailable(0x05)
}

sealed class PARAMS(val p1: Byte, val p2: Byte) {

    // 📌 1️⃣ File Selection (SELECT FILE - 0xA4) - Now P1/P2 are separate
    class FileSelection(p1: SelectionFile, p2: Occurrence) : PARAMS(p1.value, p2.value)

    // 📌 2️⃣ Record Selection (READ RECORD - 0xB2, WRITE RECORD - 0xD2) - Independent P1/P2
    class RecordSelection(p1: RecordMode, p2: RecordAccess) : PARAMS(p1.value, p2.value)

    // 📌 3️⃣ Authentication (VERIFY - 0x20, EXTERNAL AUTHENTICATE - 0x82)
    sealed class Authentication(p1: Byte, p2: Byte) : PARAMS(p1, p2) {
        data object VerifyPIN : Authentication(0x00, 0x00)
        data object ExternalAuthenticate : Authentication(0x00, 0x82.toByte())
    }

    // 📌 4️⃣ Data Handling (GET DATA - 0xCA, PUT DATA - 0xDA)
    sealed class DataTransfer(p1: Byte, p2: Byte) : PARAMS(p1, p2) {
        class GetData(tagHigh: Byte, tagLow: Byte) : DataTransfer(tagHigh, tagLow)
        class PutData(tagHigh: Byte, tagLow: Byte) : DataTransfer(tagHigh, tagLow)
    }

    data class CustomParams(val rawP1: Byte, val rawP2: Byte) : PARAMS(rawP1, rawP2)
}


sealed class Status(val sw1: Byte, val sw2: Byte) {

    // 📌 Success Responses
    data object Success : Status(0x90.toByte(), 0x00.toByte())

    // 📌 Error Responses
    data object FileNotFound : Status(0x6A.toByte(), 0x82.toByte())
    data object WrongLength : Status(0x67.toByte(), 0x00.toByte())
    data object SecurityNotSatisfied : Status(0x69.toByte(), 0x82.toByte())
    data object CommandNotAllowed : Status(0x69.toByte(), 0x86.toByte())
    data object IncorrectParameters : Status(0x6A.toByte(), 0x86.toByte())

    // 📌 Unknown Status
    data class UnknownStatus(val rawSw1: Byte, val rawSw2: Byte) : Status(rawSw1, rawSw2)

    companion object {
        fun fromBytes(status: ByteArray): Status {
            return listOf(
                Success,
                FileNotFound,
                WrongLength,
                SecurityNotSatisfied,
                CommandNotAllowed,
                IncorrectParameters
            ).find { it.sw1 == status[0] && it.sw2 == status[1] } ?: UnknownStatus(
                status[0],
                status[1]
            ) // Handle unknown cases safely
        }
    }
}

enum class AID(val value: ByteArray) {
    FILE_SYSTEM(byteArrayOf(0x3F, 0x00)), // Master File (MF)

    // Your own payment app AID (example)
    MY_PAYMENT_APP(byteArrayOf(0xA0.toByte(), 0x00, 0x00, 0x02, 0x47, 0x10, 0x99.toByte())),

    TRANSPORT_APP(byteArrayOf(0xA0.toByte(), 0x00, 0x00, 0x03, 0x86.toByte(), 0x05, 0x01));
}


