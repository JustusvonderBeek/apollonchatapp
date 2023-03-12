package com.example.apollonchat.networking

@kotlinx.serialization.Serializable
data class NetworkContact(
    var UserId : UInt,
    var Username : String,
)
