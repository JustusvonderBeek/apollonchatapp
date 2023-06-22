package com.cloudsheeptechnologies.apollonchat.networking.constants

enum class PacketCategories(val cat : Int) {
    CONTACT(1),
    DATA(2);

    companion object {
        fun getFromByte(value : Byte) = values().firstOrNull{ it.cat == value.toInt() }
    }
}