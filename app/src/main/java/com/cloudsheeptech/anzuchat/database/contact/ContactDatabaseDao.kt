package com.cloudsheeptech.anzuchat.database.contact

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface ContactDatabaseDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertContact(contact : Contact)

    @Update
    fun updateContact(contact : Contact)

    @Query("DELETE FROM contact_table WHERE contactId = :key")
    fun deleteContact(key : Long)

    @Query("DELETE from contact_table")
    fun clearContacts()

    @Query("SELECT * FROM contact_table ORDER BY contact_name ASC")
    fun getAllContacts() : LiveData<List<Contact>>

    @Query("SELECT contactId FROM contact_table ORDER BY contactId ASC")
    fun getAllContactIds() : List<Long>

    @Query("SELECT * FROM contact_table WHERE contactId = :key")
    fun getContact(key : Long) : Contact?

}