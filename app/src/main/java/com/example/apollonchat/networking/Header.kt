package com.example.apollonchat.networking

@kotlinx.serialization.Serializable
data class Header(
    var Category : Byte,
    var Type : Byte,
    var UserId : UInt,
    var MessageId : UInt
)
