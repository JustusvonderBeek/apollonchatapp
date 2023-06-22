package com.cloudsheeptechnologies.apollonchat.networking.packets

@kotlinx.serialization.Serializable
data class ContactList(
    var Contacts : List<NetworkContact>?,
)
