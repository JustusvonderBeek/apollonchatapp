package com.cloudsheeptechnologies.apollonchat.networking.packets

@kotlinx.serialization.Serializable
data class MessageAck(
    var ContactUserId : UInt,
    var Timestamp : String,
)
