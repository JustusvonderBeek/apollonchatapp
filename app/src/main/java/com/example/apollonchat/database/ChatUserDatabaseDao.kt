package com.example.apollonchat.database

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface ChatUserDatabaseDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insert(user : User)

    @Update
    fun update(user : User)

    @Query("DELETE FROM user_table WHERE userId = :key")
    fun deleteUser(key : Long)

    @Query("DELETE FROM user_table")
    fun clear()

    @Query("SELECT * FROM user_table ORDER BY username ASC")
    fun getAllUsers() : LiveData<List<User>>

    @Query("SELECT * FROM user_table WHERE userId = :key")
    fun getUser(key : Long) : User?

}