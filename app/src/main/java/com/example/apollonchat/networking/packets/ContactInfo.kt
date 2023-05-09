package com.example.apollonchat.networking.packets

import com.example.apollonchat.networking.constants.ContactType
import com.example.apollonchat.networking.constants.PacketCategories
import kotlinx.serialization.Serializable

@Serializable
data class ContactInfo(
    var ContactIds : List<UInt>,
    var ImageBytes : UInt,
    var Image : ByteArray,
    var ImageFormat : String
)
