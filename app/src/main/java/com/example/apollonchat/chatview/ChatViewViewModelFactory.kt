package com.example.apollonchat.chatview

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.apollonchat.database.ApollonDatabase
import com.example.apollonchat.database.contact.ContactDatabaseDao
import com.example.apollonchat.database.message.MessageDao
import com.example.apollonchat.database.user.UserDatabaseDao

class ChatViewViewModelFactory(val contactID : Long, val application: Application) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewViewModel::class.java)) {
            val contactDb = ApollonDatabase.getInstance(application).contactDao()
            val userDb = ApollonDatabase.getInstance(application).userDao()
            val messageDb = ApollonDatabase.getInstance(application).messageDao()
            return ChatViewViewModel(contactID, contactDb, userDb, messageDb, application) as T
        }
        throw java.lang.IllegalArgumentException("Unknown init class")
    }

}