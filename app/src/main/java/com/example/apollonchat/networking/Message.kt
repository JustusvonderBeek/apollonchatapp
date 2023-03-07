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
)
