package com.example.apollonchat.networking.packets

@kotlinx.serialization.Serializable
data class NetworkContact(
    var UserId : UInt,
    var Username : String,
)