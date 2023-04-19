package com.example.apollonchat.networking.packets

import com.example.apollonchat.networking.constants.ContactType
import com.example.apollonchat.networking.constants.PacketCategories

data class ContactAck(
    var Category : Byte,
    var Type : Byte,
    var UserId : UInt,
    var MessageId : UInt,
) {
    constructor(
        UserId: UInt
    ) : this(PacketCategories.CONTACT.cat.toByte(), ContactType.CONTACT_ACK.type.toByte(), UserId, 0u)
}
