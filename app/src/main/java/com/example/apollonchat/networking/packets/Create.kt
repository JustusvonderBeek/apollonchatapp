package com.example.apollonchat.networking.packets

@kotlinx.serialization.Serializable
data class Create(
    var Category : Byte,
    var Type : Byte,
    var UserId : UInt,
    var MessageId : UInt,
    var Username : String,
) {
    // According to the protocol we should NOT send a userId with the create account request!
    constructor(
        MessageId: UInt,
        Username: String
    ) : this(0x01, 0x01, 0U, MessageId, Username)
}
