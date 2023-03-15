package com.example.apollonchat.database.contact

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.random.Random

@Entity(tableName="contact_table")
data class Contact (
    @PrimaryKey(autoGenerate = true)
    var contactId : Long = Random.nextLong(),
    @ColumnInfo(name = "contact_name")
    var contactName : String = "",
    @ColumnInfo(name = "contact_image")
    var contactImagePath : String = "drawable/usericon.png",
    var lastMessage : String = "",
) {
    constructor(
        contactId : Long,
        contactName: String,
        contactImagePath: String,
    ) : this(contactId, contactName, contactImagePath, "")
}