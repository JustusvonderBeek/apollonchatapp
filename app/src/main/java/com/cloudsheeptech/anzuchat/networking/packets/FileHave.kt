package com.cloudsheeptech.anzuchat.networking.packets

import kotlinx.serialization.Serializable

@Serializable
data class FileHave(
    val FileOffset : Int,
)
