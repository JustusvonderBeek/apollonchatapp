package com.example.apollonchat.networking.packets

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
}
