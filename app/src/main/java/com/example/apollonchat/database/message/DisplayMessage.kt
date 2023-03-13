package com.example.apollonchat.database.message

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.apollonchat.networking.Message

@Entity(tableName = "message_table")
data class DisplayMessage(
    @PrimaryKey(autoGenerate = true)
    var Id : Long,
    var messageId : Long,
    var contactId : Long,
    var own : Boolean,
    var content : String,
    var timestamp : String,
) {
    fun toMessage(userId : UInt) : Message = Message(userId, this.messageId.toUInt(), this.contactId.toUInt(), this.timestamp, 0U, this.content)
}
