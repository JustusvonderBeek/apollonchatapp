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

    private val _contact = MutableLiveData<Contact>()
    val contact : LiveData<Contact>
        get() = _contact

    private val _messages = MutableLiveData<MutableList<String>>()
    val messages : LiveData<MutableList<String>>
        get() = _messages

    private val _hideKeyboard = MutableLiveData<Boolean>()
    val hideKeyboard : LiveData<Boolean>
        get() = _hideKeyboard

    val inputMessage = MutableLiveData<String>()

    init {
        Log.i("ChatViewViewModel", "Init")
        _hideKeyboard.value = false
        loadMessages(contactID)
    }

    fun sendMessage() {
        Log.i("ChatViewViewModel", "Message Sent Pressed")
        val message = inputMessage.value
        if (message != null && !message.contentEquals("")) {
            Log.i("ChatViewViewModel", "Message Not null")
//            if (_contact.value != null && _contact.value?.messages != null) {
                // TODO: Not persistent. Fix
//                _contact.value?.messages?.add(message)
//                _contact.value?.messages = mutableListOf("ABC")
//            }
//            _contact.value?.messages?.add(message)
//            _contact.value = _contact.value
            _messages.value?.add(message)
            _contact.value?.messages = _messages.value!!
            _contact.value?.let { updateContact(it) }
            _hideKeyboard.value = true
            inputMessage.value = ""
        }
    }

    fun hideKeyboardDone() {
        _hideKeyboard.value = false
    }

    private fun loadMessages(contactID : Long) {
        uiScope.launch {
            val localContact = loadContactFromDatabase(contactID)
            _contact.value = localContact
            _messages.value = localContact.messages
        }
    }

    private fun updateContact(contact : Contact) {
        uiScope.launch {
            updateContactInDatabase(contact)
        }
    }

    private suspend fun loadContactFromDatabase(contactID: Long) : Contact {
        val contact = withContext(Dispatchers.IO) {
            val chatContact = database.getContact(contactID) ?: throw IOException("User $contactID not found!")
            chatContact
        }
        return contact
    }

    private suspend fun updateContactInDatabase(contact : Contact) {
        withContext(Dispatchers.IO) {
            database.updateContact(contact)
        }
    }
}