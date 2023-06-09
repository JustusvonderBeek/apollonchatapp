package com.example.apollonchat.networking.constants

enum class ContactType(val type : Int) {
    CREATE(1),
    SEARCH(2),
    CONTACTS(3),
    OPTION(4),
    LOGIN(5),
    CONTACT_INFO(6),
    CONTACT_ACK(7);

    companion object {
        fun getFromByte(value : Byte) = values().firstOrNull{ it.type == value.toInt() }
    }
}