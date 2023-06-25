package com.cloudsheeptech.anzuchat.database

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.cloudsheeptech.anzuchat.database.contact.Contact
import com.cloudsheeptech.anzuchat.database.contact.ContactDatabaseDao
import com.cloudsheeptech.anzuchat.database.message.DisplayMessage
import com.cloudsheeptech.anzuchat.database.message.MessageDao
import com.cloudsheeptech.anzuchat.database.user.User
import com.cloudsheeptech.anzuchat.database.user.UserDatabaseDao

@Database(entities = [Contact::class, User::class, DisplayMessage::class], version = 1, exportSchema = false)
@TypeConverters(value = [DatabaseTypeConverter::class])
abstract class ApollonDatabase : RoomDatabase() {

    abstract fun contactDao() : ContactDatabaseDao
    abstract fun userDao() : UserDatabaseDao
    abstract fun messageDao() : MessageDao

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
                    Log.i("ApollonDatabase", "Creating new database")
                    instance = Room.databaseBuilder(context.applicationContext, ApollonDatabase::class.java, "apollon_database").fallbackToDestructiveMigration(). build()
                    INSTANCE = instance
                }

                return instance

            }
        }
    }
}