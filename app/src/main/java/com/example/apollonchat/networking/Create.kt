package com.example.apollonchat.networking

@kotlinx.serialization.Serializable
data class Create(
    var Category : Byte,
    var Type : Byte,
    var UserId : UInt,
    var MessageId : UInt,
    var Username : String,
) {
    constructor(
        UserId: UInt,
        MessageId: UInt,
        Username: String
    ) : this(0x01, 0x01, UserId, MessageId, Username)
}
