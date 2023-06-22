package com.cloudsheeptechnologies.apollonchat.chatlist

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cloudsheeptechnologies.apollonchat.database.ApollonDatabase
import com.cloudsheeptechnologies.apollonchat.database.contact.Contact
import com.cloudsheeptechnologies.apollonchat.database.user.User
import com.cloudsheeptechnologies.apollonchat.networking.Networking
import kotlinx.coroutines.*

class ChatListViewModel(val application: Application) : ViewModel() {

    /* ------------------------------------
    Class Members
     -------------------------------------- */

    private var viewModelJob = Job()
    private var uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    // Navigation
    private val _navigateToContactChat = MutableLiveData<Long>()
    val navigateToContactChat : LiveData<Long>
        get() = _navigateToContactChat

    // Creating a direct reference to the database.
    private val contactDatabase = ApollonDatabase.getInstance(application).contactDao()
    private val userDatabase = ApollonDatabase.getInstance(application).userDao()
    private val messageDatabase = ApollonDatabase.getInstance(application).messageDao()
    private val _contacts = contactDatabase.getAllContacts()
    val contacts : LiveData<List<Contact>>
        get() = _contacts
    private var _user = userDatabase.getUserAsLive()
    val user : LiveData<User>
        get() = _user

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }

    init {
        Log.i("ChatListViewModel", "ChatListViewModel created")
    }


    fun clearUser() {
        uiScope.launch {
            clearCurrentUser()
        }
    }

    fun clearContacts() {
        uiScope.launch {
            clearAllContacts()
        }
    }

    fun clearMessages() {
        uiScope.launch {
            clearAllMessages()
        }
    }

    fun reconnectNetwork() {
        uiScope.launch {
            restartNetwork()
        }
    }

    private suspend fun clearCurrentUser() {
        withContext(Dispatchers.IO) {
            userDatabase.clearUser()
        }
    }

    private suspend fun clearAllContacts() {
        withContext(Dispatchers.IO) {
            contactDatabase.clearContacts()
        }
    }

    private suspend fun clearAllMessages() {
        withContext(Dispatchers.IO) {
            messageDatabase.clearMessages()
        }
    }

    private suspend fun restartNetwork() {
        withContext(Dispatchers.IO) {
            Networking.start(application.applicationContext)
        }
    }

    fun onContactClicked(contactID : Long) {
        _navigateToContactChat.value = contactID
    }

    fun onContactNavigated() {
        _navigateToContactChat.value = -1L
    }
}