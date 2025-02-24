package com.example.nfc_demo

import android.nfc.tech.IsoDep
import android.util.Log
import java.nio.ByteBuffer

class CommandApdu private constructor(
    private val cla: CLA,
    private val ins: Instruction,
    private val params: ApduParameters,
    private val data: ByteArray,
    private val le: ByteArray,
    private val lc: ByteArray,
    private val _size: Int
) {

    fun toByteArray(): ByteArray {
        val buffer = ByteBuffer.allocate(_size)
        buffer.put(cla.value).put(ins.value).put(params.p1).put(params.p2).put(lc).put(data).put(le)
        return buffer.array()
    }


    fun sendAPDU(isoDep: IsoDep): ResponseApdu {
        val cmdApdu = this.toByteArray()
        Log.d("NFC", "Sending Command APDU: ${cmdApdu.toHexString()}")
        val response = isoDep.transceive(cmdApdu)
        return ResponseApdu.fromByteArray(response)
    }


    class Builder {
        private lateinit var cla: CLA
        private lateinit var ins: Instruction
        private var params: ApduParameters = ApduParameters.Default
        private var data = ByteArray(0)
        private var le = ByteArray(1) { 0x00 }
        private var lc = ByteArray(0)
        private var size: Int = 0

        fun cla(cla: CLA): Builder {
            this.cla = cla
            return this
        }

        fun ins(ins: Instruction): Builder {
            this.ins = ins
            return this
        }

        fun p1p2(params: ApduParameters): Builder {
            this.params = params
            return this
        }

        fun data(data: ByteArray): Builder {
            this.data = data
            this.lc = encodeLength(data.size)
            return this
        }

        private fun le(le: Int): Builder {
            this.le = encodeLength(le)
            return this
        }

        fun build(): CommandApdu {
            size = 4 + lc.size + data.size + le.size
            return CommandApdu(cla, ins, params, data, le, lc, size)
        }

        private fun encodeLength(nc: Int): ByteArray {
            require(nc in 0..65535) { "Nc must be between 0 and 65,535" }

            return when (nc) {
                in 0..255 -> byteArrayOf(nc.toByte()) // 1 byte encoding
                else -> byteArrayOf(0, (nc shr 8).toByte(), nc.toByte()) // 3 bytes encoding
            }
        }

    }
}
