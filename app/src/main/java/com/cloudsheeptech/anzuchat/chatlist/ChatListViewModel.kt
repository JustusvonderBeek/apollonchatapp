package com.cloudsheeptech.anzuchat.chatlist

import android.app.Application
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.cloudsheeptech.anzuchat.database.ApollonDatabase
import com.cloudsheeptech.anzuchat.database.contact.Contact
import com.cloudsheeptech.anzuchat.database.user.User
import com.cloudsheeptech.anzuchat.networking.ApollonProtocolHandler.ApollonProtocolHandler
import com.cloudsheeptech.anzuchat.networking.Networking
import kotlinx.coroutines.*
import java.net.URI

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

    private var _pickImage = MutableLiveData<Boolean>()
    val pickImage : LiveData<Boolean>
        get() = _pickImage

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }

    init {
        Log.i("ChatListViewModel", "ChatListViewModel created")
        _pickImage.value = false
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

    }

    fun onToolbarClicked() {
        // Selecting a new image
        _pickImage.value = true
    }

    fun onImagePicked(image : ByteArray) {
        _pickImage.value = false
        // Handling the image as new profile image
        ApollonProtocolHandler.sendContactInformation(null, image)
    }

    fun onContactClicked(contactID : Long) {
        _navigateToContactChat.value = contactID
    }

    fun onContactNavigated() {
        _navigateToContactChat.value = -1L
    }
}