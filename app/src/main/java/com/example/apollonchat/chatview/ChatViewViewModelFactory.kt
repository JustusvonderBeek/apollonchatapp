package com.example.apollonchat.chatview

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.apollonchat.database.contact.ContactDatabaseDao
import com.example.apollonchat.database.message.MessageDao
import com.example.apollonchat.database.user.UserDatabaseDao

class ChatViewViewModelFactory(val contactID : Long, val database : ContactDatabaseDao, val uDatabase : UserDatabaseDao, val mDatabase : MessageDao, val application: Application) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewViewModel::class.java)) {
            return ChatViewViewModel(contactID, database, uDatabase, mDatabase, application) as T
        }
        throw java.lang.IllegalArgumentException("Unknown init class")
    }

}