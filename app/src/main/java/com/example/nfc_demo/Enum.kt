package com.example.nfc_demo


enum class CLA(val value: Byte) {
    ClassA(0x00.toByte());

    companion object {
        fun fromBytes(cla: Byte): CLA {
            return entries.find { it.value == cla }
                ?: throw IllegalArgumentException("Unknown CLA value")
        }
    }
}

enum class Instruction(val value: Byte) {
    Select(0xA4.toByte()), Authenticate(0x10.toByte()), Read(0x20.toByte()), Write(0x30.toByte());

    companion object {
        fun fromBytes(instruction: Byte): Instruction {

            return entries.find { it.value == instruction }
                ?: throw IllegalArgumentException("Unknown INS value")
        }
    }
}

enum class Status(val value: ByteArray) {
    Success(
        byteArrayOf(
            0x90.toByte(),
            0x00.toByte(),
        )
    ),
    Failure(
        byteArrayOf(
            0x6a.toByte(),
            0x82.toByte(),
        )
    );

    companion object {
        fun fromBytes(status: ByteArray): Status {
            return entries.find { it.value.contentEquals(status) } ?: Failure
        }
    }
}

var AID1: ByteArray = hexStringToByteArray("F0010203040506")
var AID2: ByteArray = hexStringToByteArray("D2760000850101")


object ApduProto {


    var selectCommand =
        CommandApdu.Builder().setClass(CLA.ClassA).setInstruction(Instruction.Select)
            .setParams(0x04, 0x00).setData(AID1).setResponseLength(0)

    var authenticateCommandBuilder =
        CommandApdu.Builder().setClass(CLA.ClassA).setInstruction(Instruction.Authenticate)
    var readCommandBuilder =
        CommandApdu.Builder().setClass(CLA.ClassA).setInstruction(Instruction.Read)


    var accept: Boolean = false

}


fun hexStringToByteArray(hex: String): ByteArray {
    require(hex.length % 2 == 0) { "Hex string must have an even length" }

    return hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}
