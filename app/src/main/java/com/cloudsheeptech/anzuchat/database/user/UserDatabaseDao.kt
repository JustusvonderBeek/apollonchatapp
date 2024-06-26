package com.cloudsheeptech.anzuchat.database.user

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface UserDatabaseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertUser(user : User)

    @Update
    fun updateUser(user: User)

    @Query("DELETE FROM user_table")
    fun clearUser()

    @Query("SELECT * FROM user_table")
    fun getUserAsLive() : LiveData<User>

    @Query("SELECT * FROM user_table")
    fun getUser() : User?

}