package com.example.apollonchat.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.apollonchat.database.contact.Contact
import com.example.apollonchat.database.contact.ContactConverter
import com.example.apollonchat.database.contact.ContactDatabaseDao
import com.example.apollonchat.database.user.User
import com.example.apollonchat.database.user.UserDatabaseDao

@Database(entities = [Contact::class, User::class], version = 2, exportSchema = false)
@TypeConverters(value = [ContactConverter::class])
abstract class ApollonDatabase : RoomDatabase() {

    abstract val contactDatabaseDao : ContactDatabaseDao
    abstract val userDatabaseDao : UserDatabaseDao

    companion object {

        @Volatile // Shared across threads, not cached, good for multithreading access
        private var INSTANCE : ApollonDatabase? = null

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                var contactList : List<Contact> = emptyList()
            }
        }

        fun getInstance(context: Context) : ApollonDatabase {
            synchronized(this) {
                var instance = INSTANCE

                if (instance == null) {
                    instance = Room.databaseBuilder(context.applicationContext, ApollonDatabase::class.java, "apollon_database").fallbackToDestructiveMigration().build()
                    INSTANCE = instance
                }

                return instance

            }
        }
    }
}