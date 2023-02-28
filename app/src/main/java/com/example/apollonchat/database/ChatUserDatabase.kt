package com.example.apollonchat.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

//@Database(entities = [User::class], version = 3, exportSchema = false)
//@TypeConverters(value = [ChatUserConverter::class])
//abstract class ChatUserDatabase : RoomDatabase() {
//
//    abstract val chatUserDatabaseDao : ChatUserDatabaseDao
//
//    companion object {
//        @Volatile
//        private var INSTANCE : ChatUserDatabase? = null
//
//        private val MIGRATION_2_3 = object : Migration(2, 3) {
//            override fun migrate(database: SupportSQLiteDatabase) {
//                var userList : List<User> = emptyList()
//            }
//        }
//
//        fun getInstance(context: Context) : ChatUserDatabase {
//            synchronized(this) {
//                var instance = INSTANCE
//
//                if (instance == null) {
//                    instance = Room.databaseBuilder(context.applicationContext, ChatUserDatabase::class.java, "chat_user_database").build()
//                    INSTANCE = instance
//                }
//
//                return instance
//
//            }
//        }
//    }
//}