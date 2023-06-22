package com.cloudsheeptechnologies.apollonchat.networking.packets

@kotlinx.serialization.Serializable
data class NetworkOption(
    var Type : String,
    var Value : String
)
