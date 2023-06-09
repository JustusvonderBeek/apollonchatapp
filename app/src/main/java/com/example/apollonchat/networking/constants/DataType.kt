package com.example.apollonchat.networking.constants

enum class DataType(val type : Int) {
    TEXT(1),
    TEXT_ACK(2),
    FILE_INFO(3),
    FILE_HAVE(4),
    FILE(5),
    FILE_ACK(6);

    companion object {
        fun getFromByte(value : Byte) = values().firstOrNull{ it.type == value.toInt() }
    }
}