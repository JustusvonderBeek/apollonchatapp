package com.cloudsheeptech.anzuchat.networking.packets

@kotlinx.serialization.Serializable
data class ContactList(
    var Contacts : List<NetworkContact>?,
)
