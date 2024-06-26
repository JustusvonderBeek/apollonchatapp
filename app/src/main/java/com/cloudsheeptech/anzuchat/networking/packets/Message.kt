package com.cloudsheeptech.anzuchat.networking.packets

import com.cloudsheeptech.anzuchat.database.message.DisplayMessage
import kotlin.random.Random

@kotlinx.serialization.Serializable
data class Message(
    var ContactUserId : UInt,
    var Timestamp : Long,
    var Message : String,
) {
    // TODO: Fix time timestamp to be generated internally and have the correct format

    fun toDisplayMessage(userId : Long) : DisplayMessage = DisplayMessage(Random.nextLong(), 0, this.ContactUserId.toLong(), userId == ContactUserId.toLong(), this.Message, this.Timestamp)
}
