package com.cloudsheeptech.anzuchat.networking.packets

@kotlinx.serialization.Serializable
data class Header(
    var Category : Byte,
    var Type : Byte,
    var UserId : UInt,
    var MessageId : UInt
) {
    fun toByteArray() : ByteArray {
        val buffer = ByteArray(10)
        buffer[0] = this.Category
        buffer[1] = this.Type
        // Big Endian
        buffer[2] = (this.UserId shr 24).toByte()
        buffer[3] = (this.UserId shr 16).toByte()
        buffer[4] = (this.UserId shr 8).toByte()
        buffer[5] = (this.UserId shr 0).toByte()
        buffer[6] = (this.MessageId shr 24).toByte()
        buffer[7] = (this.MessageId shr 16).toByte()
        buffer[8] = (this.MessageId shr 8).toByte()
        buffer[9] = (this.MessageId shr 0).toByte()
        return buffer
    }

    companion object {
        fun convertRawToHeader(raw : ByteArray) : Header? {
            if (raw.size != 10) {
                return null
            }
            val userId =
                (raw[2].toInt() and 0xff shl 24) or
                (raw[3].toInt() and 0xff shl 16) or
                (raw[4].toInt() and 0xff shl 8) or
                (raw[5].toInt() and 0xff)
            val messageId =
                (raw[6].toInt() and 0xff shl 24) or
                (raw[7].toInt() and 0xff shl 16) or
                (raw[8].toInt() and 0xff shl 8) or
                (raw[9].toInt() and 0xff)
            val header = Header(raw[0], raw[1], userId.toUInt(), messageId.toUInt())
            return header
        }
    }
}
