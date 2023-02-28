package com.example.apollonchat.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.random.Random
import kotlin.random.nextUInt
import kotlin.random.nextULong

@Entity(tableName="user_table")
data class User (
    @PrimaryKey(autoGenerate = true)
    var userId : Long = Random.nextLong(),
    @ColumnInfo(name = "username")
    var username : String = "",
    @ColumnInfo(name = "image")
    var userImagePath : String = "drawable/usericon.png",
    @ColumnInfo(name = "messages")
    var messages : List<String> = emptyList(),
)