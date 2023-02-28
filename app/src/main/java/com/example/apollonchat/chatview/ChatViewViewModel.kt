package com.example.apollonchat.chatview

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ChatViewViewModel(application: Application) : ViewModel() {

    private val _username = MutableLiveData<String>()
    val username : LiveData<String>
        get() = _username

    private val _contactImage = MutableLiveData<String>()
    val contactImage : LiveData<String>
        get() = _contactImage

    init {
        Log.i("ChatViewViewModel", "Init")

    }

    // TODO: Add functions
}