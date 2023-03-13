package com.example.apollonchat.networking

import com.example.apollonchat.database.message.DisplayMessage
import kotlin.random.Random

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

    fun toDisplayMessage(userId : Long) : DisplayMessage = DisplayMessage(Random.nextLong(), this.MessageId.toLong(), this.ContactUserId.toLong(), userId == this.UserId.toLong(), this.Message, this.Timestamp)
}
