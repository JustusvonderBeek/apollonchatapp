package com.example.apollonchat.networking.packets

import com.example.apollonchat.networking.constants.ContactType
import com.example.apollonchat.networking.constants.PacketCategories
import kotlinx.serialization.Serializable

@Serializable
data class ContactInfo(
    var Category : Byte,
    var Type : Byte,
    var UserId : UInt,
    var MessageId : UInt,
    var ContactIds : List<UInt>,
    var ImageBytes : UInt,
    var ImageFormat : String
) {
    constructor(
        UserId: UInt,
        ContactIds: List<UInt>,
        ImageBytes: UInt,
        ImageFormat: String
    ) : this(PacketCategories.CONTACT.cat.toByte(), ContactType.CONTACT_INFO.type.toByte(), UserId, 0u, ContactIds, ImageBytes, ImageFormat)
}
