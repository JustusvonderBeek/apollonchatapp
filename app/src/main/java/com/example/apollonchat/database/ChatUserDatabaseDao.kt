package com.example.apollonchat.database

import androidx.lifecycle.LiveData
import androidx.room.*

//@Dao
//interface ChatUserDatabaseDao {
//
//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    fun insert(user : User)
//
//    @Update
//    fun update(user : User)
//
//    @Query("DELETE from user_table WHERE userId = :key")
//    fun deleteUser(key : Long)
//
//    @Query("DELETE from user_table")
//    fun clear()
//
//    @Query("SELECT * from user_table ORDER BY username ASC")
//    fun getAllUsers() : LiveData<List<User>>
//
//    @Query("SELECT * from user_table WHERE userId = :key")
//    fun getUser(key : Long) : User?
//
//}