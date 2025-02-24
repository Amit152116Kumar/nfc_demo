package com.example.nfc_demo

enum class CLA(val value: Byte) {
    ISO(0x00), PROPRIETARY(0x90.toByte())
}

val DefaultApdu = CommandApdu.Builder().cla(CLA.PROPRIETARY)
val ISOApdu = CommandApdu.Builder().cla(CLA.ISO)

// Sealed hierarchy for Instruction (Instruction) categorized
sealed class Instruction(val value: Byte) {


    sealed class Transaction(value: Byte) : Instruction(value) {
        data object Abort : Transaction(0xA7.toByte())
    }


    sealed class Authentication(value: Byte) : Instruction(value) {
        data object AES : Authentication(0xAA.toByte())
        data object DES2K3 : Authentication(0x0A)
        data object ChangeKey : Authentication(0xC4.toByte())
        data object EV2First : Authentication(0x71.toByte())
        data object EV2NonFirst : Authentication(0x77)
        data object Step2Auth : Authentication(0xAF.toByte())
    }

    sealed class FileManagement(value: Byte) : Instruction(value) {
        data object SelectFile : FileManagement(0xA4.toByte())
        data object CreateFile : FileManagement(0xE0.toByte())
        data object DeleteFile : FileManagement(0xE4.toByte())
    }

    sealed class Security(value: Byte) : Instruction(value) {
        data object Verify : Security(0x20.toByte())
        data object ExternalAuthenticate : Security(0x82.toByte())
        data object InternalAuthenticate : Security(0x88.toByte())
        data object GetChallenge : Security(0x84.toByte())
        data object FormatPICC : Security(0xFC.toByte())
    }

    sealed class RecordManagement(value: Byte) : Instruction(value) {
        data object ReadRecord : RecordManagement(0xAB.toByte())
        data object WriteRecord : RecordManagement(0x8B.toByte())
        data object UpdateRecord : RecordManagement(0xBA.toByte())
        data object ClearRecord : RecordManagement(0xEB.toByte())
    }


    sealed class DataManagement(value: Byte) : Instruction(value) {
        data object GetData : DataManagement(0xCA.toByte())
        data object PutData : DataManagement(0xDA.toByte())
        data object UpdateData : DataManagement(0xDC.toByte())
    }

    sealed class Other(value: Byte) : Instruction(value) {
        data object ManageChannel : Other(0x20)
        data object GetResponse : Other(0xC0.toByte())
        data object Envelope : Other(0xC2.toByte())
    }

    data object SelectApplication : Instruction(0x5A)

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

sealed class ApduParameters(val p1: Byte, val p2: Byte) {

    // 📌 1️⃣ File Selection (SELECT FILE - 0xA4) - Now P1/P2 are separate
    class FileSelection(p1: SelectionFile, p2: Occurrence) : ApduParameters(p1.value, p2.value)

    // 📌 2️⃣ Record Selection (READ RECORD - 0xB2, WRITE RECORD - 0xD2) - Independent P1/P2
    class RecordSelection(p1: RecordMode, p2: RecordAccess) : ApduParameters(p1.value, p2.value)

    // 📌 3️⃣ Authentication (VERIFY - 0x20, EXTERNAL AUTHENTICATE - 0x82)
    sealed class Authentication(p1: Byte, p2: Byte) : ApduParameters(p1, p2) {
        data object VerifyPIN : Authentication(0x00, 0x00)
        data object ExternalAuthenticate : Authentication(0x00, 0x82.toByte())
    }

    // 📌 4️⃣ Data Handling (GET DATA - 0xCA, PUT DATA - 0xDA)
    sealed class DataTransfer(p1: Byte, p2: Byte) : ApduParameters(p1, p2) {
        class GetData(tagHigh: Byte, tagLow: Byte) : DataTransfer(tagHigh, tagLow)
        class PutData(tagHigh: Byte, tagLow: Byte) : DataTransfer(tagHigh, tagLow)
    }

    data class CustomParams(val rawP1: Byte, val rawP2: Byte) : ApduParameters(rawP1, rawP2)

    data object Default : ApduParameters(0x00, 0x00)
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
    MASTER_FILE(byteArrayOf(0x00, 0x00, 0x00)), // Master File (MF)

    // Your own payment app AID (example)
    MY_PAYMENT_APP(byteArrayOf(0xA0.toByte(), 0x00, 0x00, 0x02, 0x47, 0x10, 0x99.toByte())),

    TRANSPORT_APP(byteArrayOf(0xA0.toByte(), 0x00, 0x00, 0x03, 0x86.toByte(), 0x05, 0x01));
}


fun ByteArray.toHexString(): String {
    return joinToString("") { "0x%02X ".format(it) }
}

enum class FileType(val fileNo: Byte) {
    StandardDataFile1(0X00),
    StandardDataFile2(0x04),
    ValueFile(0x03),
    CyclicRecordFile(0x01),
    TransactionMacFile(0x0f)
}