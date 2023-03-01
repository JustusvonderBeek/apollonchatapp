package com.example.apollonchat.chatview

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.apollonchat.database.contact.ContactDatabaseDao

class ChatViewViewModelFactory(val contactID : Long, val database : ContactDatabaseDao, val application: Application) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewViewModel::class.java)) {
            return ChatViewViewModel(contactID, database, application) as T
        }
        throw java.lang.IllegalArgumentException("Unknown init class")
    }

}