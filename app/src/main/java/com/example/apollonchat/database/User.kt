package com.example.apollonchat.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.concurrent.timer
import kotlin.random.Random
import kotlin.random.nextUInt

@Entity(tableName="user_table")
data class User (
    @PrimaryKey(autoGenerate = true)
    var userId : UInt = 0u,
    @ColumnInfo(name = "username")
    var username : String? = "",
    @ColumnInfo(name = "image")
    var userimage : String? = "",
    @ColumnInfo(name = "messages")
    var messages : List<String> = emptyList(),
)