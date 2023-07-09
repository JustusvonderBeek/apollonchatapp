package com.cloudsheeptech.anzuchat.database.message

import androidx.lifecycle.LiveData
import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface MessageDao {

    @Insert
    fun insertMessage(message : DisplayMessage)

    @Update
    fun updateMessage(message : DisplayMessage)

    @Query("DELETE FROM message_table WHERE messageId = :key")
    fun deleteMessage(key : Long)

    @Query("DELETE FROM message_table WHERE contactId = :key")
    fun deleteMessagesFromUser(key : Long)

    @Query("DELETE FROM message_table")
    fun clearMessages()

    @Query("SELECT * FROM message_table WHERE contactId = :contactId ORDER BY messageId ASC")
    fun getMessagesLive(contactId : Long) : LiveData<MutableList<DisplayMessage>>

    @Query("SELECT * FROM message_table WHERE contactId = :contactId ORDER BY messageId ASC")
    fun messagesByIDPaged(contactId : Long) : PagingSource<Int, DisplayMessage>

    @Query("SELECT * FROM message_table WHERE contactId = :contactId ORDER BY messageId ASC")
    fun getMessages(contactId : Long) : MutableList<DisplayMessage>?

}