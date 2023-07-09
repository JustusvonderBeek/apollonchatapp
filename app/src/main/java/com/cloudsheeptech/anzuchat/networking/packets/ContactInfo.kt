package com.cloudsheeptech.anzuchat.networking.packets

import kotlinx.serialization.Serializable

@Serializable
data class ContactInfo @OptIn(ExperimentalUnsignedTypes::class) constructor(
    var Username : String,
    var ContactIds : List<UInt>,
    var ImageBytes : UInt,
    var ImageFormat : String,
    var Image : UByteArray,
)
