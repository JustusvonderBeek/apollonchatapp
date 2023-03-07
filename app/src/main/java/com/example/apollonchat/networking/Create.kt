package com.example.apollonchat.networking

@kotlinx.serialization.Serializable
data class Create(
    var Category : Byte = 0x1,
    var Type : Byte = 0x2,
    var UserId : UInt,
    var MessageId : UInt,
    var Username : String,
)
