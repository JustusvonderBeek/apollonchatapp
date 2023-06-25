package com.cloudsheeptech.anzuchat.networking.packets

@kotlinx.serialization.Serializable
data class NetworkOption(
    var Type : String,
    var Value : String
)
