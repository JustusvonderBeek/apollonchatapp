package com.example.apollonchat.database.user

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.random.Random

@Entity(tableName="user_table")
data class User(
    @PrimaryKey(autoGenerate = true)
    var userId : Long = Random.nextLong(),
    @ColumnInfo(name = "username")
    var username : String?,
    @ColumnInfo(name = "user_image")
    var userImage : String?
)
