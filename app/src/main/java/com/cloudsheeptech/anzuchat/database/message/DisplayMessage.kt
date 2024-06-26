package com.cloudsheeptech.anzuchat.database.message

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.random.Random

@Entity(tableName = "message_table")
data class DisplayMessage(
    @PrimaryKey(autoGenerate = true)
    var Id : Long,
    var messageId : Long,
    var contactId : Long,
    var own : Boolean,
    var content : String,
    var timestamp : Long,
) {
    constructor(
        messageId: Long,
        contactId: Long,
        own: Boolean,
        content: String,
        timestamp: Long,
    ) : this(Random.nextLong(), messageId, contactId, own, content, timestamp)
//    fun toMessage(userId : UInt) : Message = Message(userId, this.messageId.toUInt(), this.contactId.toUInt(), this.timestamp, 0U, this.content)
}
