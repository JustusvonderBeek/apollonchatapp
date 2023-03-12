package com.example.apollonchat.networking

import io.ktor.util.date.*
@kotlinx.serialization.Serializable
data class Message(
    var Category : Byte,
    var Type : Byte,
    var UserId : UInt,
    var MessageId : UInt,
    var ContactUserId : UInt,
    var Timestamp : String,
    var Part : UInt,
    var Message : String,
) {
    // TODO: Fix time timestamp to be generated internally and have the correct format
    constructor(
        UserId: UInt,
        MessageId: UInt,
        ContactUserId: UInt,
        Timestamp: String,
        Part: UInt,
        Message: String
    ) : this(0x02, 0x01, UserId, MessageId, ContactUserId, Timestamp, Part, Message)
}
