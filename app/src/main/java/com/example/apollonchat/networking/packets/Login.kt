package com.example.apollonchat.networking.packets

import com.example.apollonchat.networking.constants.ContactType
import com.example.apollonchat.networking.constants.PacketCategories

@kotlinx.serialization.Serializable
data class Login(
    var Category : Byte,
    var Type : Byte,
    var UserId : UInt,
    var MessageId : UInt
) {
    constructor(
        UserId: UInt,
    ) : this(PacketCategories.CONTACT.cat.toByte(), ContactType.LOGIN.type.toByte(), UserId, 0U)
}
