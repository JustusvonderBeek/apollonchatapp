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
    // MessageId is set by the Networking Library!
    constructor(
        Username: String
    ) : this(0x01, 0x01, 0U, 0U, Username)
}
