package com.example.apollonchat.chatlist

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.apollonchat.database.contact.ContactDatabaseDao
import com.example.apollonchat.database.contact.Contact
import com.example.apollonchat.database.message.DisplayMessage
import com.example.apollonchat.database.message.MessageDao
import com.example.apollonchat.database.user.User
import com.example.apollonchat.database.user.UserDatabaseDao
import com.example.apollonchat.networking.Networking
import com.example.apollonchat.networking.constants.DataType
import com.example.apollonchat.networking.constants.PacketCategories
import com.example.apollonchat.networking.packets.Message
import kotlinx.coroutines.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.net.InetAddress
import kotlin.random.Random

class ChatListViewModel(val contactDatabase : ContactDatabaseDao, val userDatabase : UserDatabaseDao, val messageDatabase : MessageDao, val application: Application) : ViewModel() {

    // Encapsulated so that no outside methods can modify this
    private var viewModelJob = Job()
    private var uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }

    // Navigation
    private val _navigateToContactChat = MutableLiveData<Long>()
    val navigateToContactChat : LiveData<Long>
        get() = _navigateToContactChat

    // Creating a direct reference to the database.
    private val _contacts = contactDatabase.getAllContacts()
    val contacts : LiveData<List<Contact>>
        get() = _contacts

    private var _user = userDatabase.getUserAsLive()
    val user : LiveData<User>
        get() = _user

    private var json = Json { ignoreUnknownKeys = true }

    init {
        Log.i("ChatListViewModel", "ChatListViewModel created")
        uiScope.launch {
            startNetwork()
        }
        uiScope.launch {
            registerCallbackForIncomingMessages()
        }
    }

    private suspend fun registerCallbackForIncomingMessages() {
        withContext(Dispatchers.IO) {
            Networking.registerCallback(PacketCategories.DATA.cat.toLong(), DataType.TEXT.type.toLong()) { packet ->
                val message = json.decodeFromString<Message>(packet)
                var oldMessageId = 0
                if (messageDatabase.getMessages(message.UserId.toLong()) != null) {
                    oldMessageId = messageDatabase.getMessages(message.UserId.toLong())!!.size + 1
                }
                val dm = DisplayMessage(oldMessageId.toLong(), message.UserId.toLong(), false, message.Message, message.Timestamp)
                messageDatabase.insertMessage(dm)
            }
        }
    }

    private suspend fun startNetwork() {
        withContext(Dispatchers.IO) {
            Networking.start(InetAddress.getByName("homecloud.homeplex.org"), contactDatabase, userDatabase, messageDatabase)
        }
    }


    fun clearUser() {
        uiScope.launch {
            clearCurrentUser()
        }
    }

    private suspend fun clearCurrentUser() {
        withContext(Dispatchers.IO) {
            userDatabase.clearUser()
        }
    }

    fun onContactClicked(contactID : Long) {
        _navigateToContactChat.value = contactID
    }

    fun onContactNavigated() {
        _navigateToContactChat.value = -1L
    }
}