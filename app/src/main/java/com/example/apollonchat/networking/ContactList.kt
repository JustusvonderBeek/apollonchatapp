package com.example.apollonchat.networking

@kotlinx.serialization.Serializable
data class ContactList(
    var Category : Byte,
    var Type : Byte,
    var UserId : UInt,
    var MessageId : UInt,
    var Contacts : List<NetworkContact>?,
)
