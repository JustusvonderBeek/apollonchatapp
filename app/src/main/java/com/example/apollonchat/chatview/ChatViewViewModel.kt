package com.example.apollonchat.chatview

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.apollonchat.database.contact.Contact
import com.example.apollonchat.database.contact.ContactDatabaseDao
import kotlinx.coroutines.*
import java.io.IOException

class ChatViewViewModel(val contactID: Long, val database: ContactDatabaseDao, val application: Application) : ViewModel() {

    private var viewModelJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    private val _username = MutableLiveData<String>()
    val username : LiveData<String>
        get() = _username

    private val _contact = MutableLiveData<Contact>()

    private val _contactImage = MutableLiveData<String>()
    val contactImage : LiveData<String>
        get() = _contactImage

    private val _messages = MutableLiveData<List<String>>()
    val messages : LiveData<List<String>>
        get() = _messages

    init {
        Log.i("ChatViewViewModel", "Init")
        loadMessages(contactID)
    }

    // TODO: Add functions
    private fun loadMessages(contactID : Long) {
        uiScope.launch {
            val contact = loadContactFromDatabase(contactID)
            _contact.value = contact
            _messages.value = contact.messages
        }
    }

    private suspend fun loadContactFromDatabase(contactID: Long) : Contact {
        val contact = withContext(Dispatchers.IO) {
            val chatContact = database.getContact(contactID) ?: throw IOException("User $contactID not found!")
            chatContact
        }
        return contact
    }
}