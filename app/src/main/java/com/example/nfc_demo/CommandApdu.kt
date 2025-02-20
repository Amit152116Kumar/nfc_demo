package com.example.nfc_demo

import java.nio.ByteBuffer

class CommandApdu private constructor(
    private val cla: CLA,
    private val ins: INS,
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

    fun getInstruction(): INS {
        return this.ins
    }

    fun getData(): ByteArray {
        return this.data
    }


    class Builder {
        private lateinit var cla: CLA
        private lateinit var ins: INS
        private var p1: Byte = 0x00
        private var p2: Byte = 0x00
        private lateinit var data: ByteArray
        private var le: ByteArray = ByteArray(0)
        private lateinit var lc: ByteArray
        private var size: Int = 0

        fun setClass(cla: CLA): Builder {
            this.cla = cla
            return this
        }

        fun setInstruction(ins: INS): Builder {
            this.ins = ins
            return this
        }

        fun setParams(p1: Byte, p2: Byte): Builder {
            this.p1 = p1
            this.p2 = p2
            return this
        }

        fun setData(data: ByteArray): Builder {
            this.data = data
            this.lc = encodeLength(data.size)
            return this
        }

        fun setResponseLength(le: Int): Builder {
            this.le = encodeLength(le)
            return this
        }

        fun build(): CommandApdu {
            if (!::data.isInitialized) {
                this.setData(ByteArray(0))
            }
            size = 4 + lc.size + data.size + le.size
            return CommandApdu(cla, ins, p1, p2, data, le, lc, size)
        }

        private fun encodeLength(nc: Int): ByteArray {
            require(nc in 0..65535) { "Nc must be between 0 and 65,535" }

            return when (nc) {
                0 -> byteArrayOf(0) // 0 bytes
                in 1..255 -> byteArrayOf(nc.toByte()) // 1 byte encoding
                else -> byteArrayOf(0, (nc shr 8).toByte(), nc.toByte()) // 3 bytes encoding
            }
        }

    }


}
