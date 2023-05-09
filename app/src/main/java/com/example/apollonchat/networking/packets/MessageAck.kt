package com.example.apollonchat.networking.packets

@kotlinx.serialization.Serializable
data class MessageAck(
    var ContactUserId : UInt,
    var Timestamp : String,
)
