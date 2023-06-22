package com.cloudsheeptechnologies.apollonchat.networking.packets

@kotlinx.serialization.Serializable
data class ContactOption(
    var ContactUserId : UInt,
    var Options : List<NetworkOption>
)
