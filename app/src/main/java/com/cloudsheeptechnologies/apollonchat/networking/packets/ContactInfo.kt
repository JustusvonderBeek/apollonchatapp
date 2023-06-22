package com.cloudsheeptechnologies.apollonchat.networking.packets

import kotlinx.serialization.Serializable

@Serializable
data class ContactInfo(
    var ContactIds : List<UInt>,
    var ImageBytes : UInt,
    var Image : ByteArray,
    var ImageFormat : String
)
