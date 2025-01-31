package com.example.nfc_demo

import com.example.nfc_demo.Status.Failure
import java.nio.ByteBuffer


class ResponseApdu(private val status: Status, private val data: ByteArray?) {

    fun getStatus(): Status {
        return this.status
    }

    fun getData(): ByteArray? {
        return this.data
    }


    fun toByteArray(): ByteArray {
        val buffer = ByteBuffer.allocate((data?.size ?: 0) + 2)
        if (data != null) {
            buffer.put(data)
        }
        buffer.put(status.value)
        return buffer.array()
    }

    companion object {
        fun fromByteArray(apdu: ByteArray): ResponseApdu {

            val statusBytes = apdu.takeLast(2).toByteArray()
            // Extract the data (everything except the last two bytes)
            val data = apdu.copyOfRange(0, apdu.size - 2)

            // Determine the status based on SW1 and SW2
            val status = Status.fromBytes(statusBytes)

            return ResponseApdu(status, data)
        }

    }
}

class CommandApdu private constructor(
    private val cla: CLA,
    private val ins: Instruction,
    private val p1: Byte,
    private val p2: Byte,
    private val data: ByteArray,
    private val le: ByteArray,
    private val lc: ByteArray,
    private val _size: Int
) {

    fun toByteArray(): ByteArray {
        val buffer = ByteBuffer.allocate(_size)
        buffer.put(cla.value).put(ins.value).put(p1).put(p2).put(lc).put(data).put(le)
        return buffer.array()
    }

    fun getInstruction(): Instruction {
        return this.ins
    }

    fun getData(): ByteArray {
        return this.data
    }

    companion object {
        fun fromByteArray(apdu: ByteArray): CommandApdu {
            require(apdu.size >= 4) { "Invalid APDU: Minimum length is 4 bytes (CLA, INS, P1, P2)" }

            val buffer = ByteBuffer.wrap(apdu)

            val cla = CLA.fromBytes(buffer.get())

            val ins = Instruction.fromBytes(buffer.get())

            val p1 = buffer.get()
            val p2 = buffer.get()

            val lc: ByteArray
            val data: ByteArray
            val le: ByteArray

            if (buffer.remaining() > 0) {
                val lcValue = buffer.get().toInt() and 0xFF
                lc = byteArrayOf(lcValue.toByte())

                data = if (lcValue > 0) {
                    ByteArray(lcValue).also { buffer.get(it) }
                } else {
                    byteArrayOf()
                }

                le = if (buffer.remaining() > 0) {
                    ByteArray(1).also { buffer.get(it) }
                } else {
                    byteArrayOf()
                }
            } else {
                lc = byteArrayOf()
                data = byteArrayOf()
                le = byteArrayOf()
            }
            val size = 4 + lc.size + data.size + le.size

            return CommandApdu(cla, ins, p1, p2, data, le, lc, size)
        }

    }

    class Builder {
        private var cla: CLA = CLA.ClassA
        private lateinit var ins: Instruction
        private var p1: Byte = 0x00
        private var p2: Byte = 0x00
        private var data: ByteArray? = null
        private var le: ByteArray = ByteArray(0) // Nullable
        private lateinit var lc: ByteArray
        private var size: Int = 0

        fun setClass(cla: CLA): Builder {
            this.cla = cla
            return this
        }

        fun setInstruction(ins: Instruction): Builder {
            this.ins = ins
            return this
        }

        fun setParams(p1: Byte, p2: Byte): Builder {
            this.p1 = p1
            return this
        }

        fun setData(data: ByteArray): Builder {
            this.data = data
            this.lc = encodeLc(data.size)
            return this
        }

        fun setResponseLength(le: Int): Builder {
            this.le = encodeLc(le)
            return this
        }

        fun build(): CommandApdu {
            if (data == null) {
                data = ByteArray(0)
                this.lc = encodeLc(0)
            }
            size = 4 + lc.size + data!!.size + le.size
            return CommandApdu(cla, ins, p1, p2, data!!, le, lc, size)
        }

        private fun encodeLc(nc: Int): ByteArray {
            require(nc in 0..65535) { "Nc must be between 0 and 65,535" }

            return when (nc) {
                0 -> byteArrayOf(0) // 0 bytes
                in 1..255 -> byteArrayOf(nc.toByte()) // 1 byte encoding
                else -> byteArrayOf(0, (nc shr 8).toByte(), nc.toByte()) // 3 bytes encoding
            }
        }

    }


}

