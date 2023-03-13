package com.example.apollonchat.networking.packets

@kotlinx.serialization.Serializable
data class Search(
    var Category : Byte,
    var Type : Byte,
    var UserId : UInt,
    var MessageId : UInt,
    var UserIdentifier : String,
) {
    constructor(
        UserId: UInt,
        MessageId: UInt,
        UserIdentifier: String,
    ) : this(0x01, 0x02, UserId, MessageId, UserIdentifier)
}
