package com.example.apollonchat.addcontact

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.apollonchat.database.contact.ContactDatabaseDao
import com.example.apollonchat.database.user.UserDatabaseDao

class AddContactViewModelFactory(val uDatabase : UserDatabaseDao, val database : ContactDatabaseDao) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddContactViewModel::class.java)) {
            return AddContactViewModel(uDatabase, database) as T
        }
        throw IllegalArgumentException("Unknown class")
    }
}