package com.cloudsheeptech.anzuchat.networking.packets

import kotlinx.serialization.Serializable

@Serializable
data class FileInfo(
    val ContactUserId : UInt,
    val Timestamp : String,
    val FileType : String,
    val FileName : String,
    val FileLength : UInt,
    val Compression : String,
    val CompressedLength : UInt,
    val FileHash : Long
)
