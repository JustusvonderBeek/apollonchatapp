package com.cloudsheeptechnologies.apollonchat.chatlist

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class ChatListViewModelFactory(val application : Application) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatListViewModel::class.java)) {
            return ChatListViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown class")
    }

}