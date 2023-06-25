package com.cloudsheeptech.anzuchat.chatview

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.cloudsheeptech.anzuchat.database.ApollonDatabase

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