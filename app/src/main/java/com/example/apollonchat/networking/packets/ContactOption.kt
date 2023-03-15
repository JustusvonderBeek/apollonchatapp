package com.example.apollonchat.networking.packets

import com.example.apollonchat.networking.constants.ContactType
import com.example.apollonchat.networking.constants.PacketCategories

@kotlinx.serialization.Serializable
data class ContactOption(
    var Category : Byte,
    var Type : Byte,
    var UserId : UInt,
    var MessageId : UInt,
    var ContactUserId : UInt,
    var Options : List<NetworkOption>
) {
    constructor(
        UserId : UInt,
        ContactUserId : UInt,
        Options : List<NetworkOption>
    ) : this(PacketCategories.CONTACT.cat.toByte(), ContactType.OPTION.type.toByte(), UserId, 0U, ContactUserId, Options)
}
