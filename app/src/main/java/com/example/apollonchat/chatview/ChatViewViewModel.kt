package com.example.apollonchat.chatview

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.apollonchat.database.contact.Contact
import com.example.apollonchat.database.contact.ContactDatabaseDao
import com.example.apollonchat.database.message.DisplayMessage
import com.example.apollonchat.database.message.MessageDao
import com.example.apollonchat.database.user.User
import com.example.apollonchat.database.user.UserDatabaseDao
import com.example.apollonchat.networking.packets.Message
import com.example.apollonchat.networking.Networking
import io.ktor.util.date.*
import kotlinx.coroutines.*
import java.io.IOException

class ChatViewViewModel(val contactID: Long, val contactDatabase: ContactDatabaseDao, val userDatabase : UserDatabaseDao, val messageDatabase : MessageDao, val application: Application) : ViewModel() {

    private var viewModelJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    // TODO: Refactor and maybe remove if not necessary
    private val _contact = MutableLiveData<Contact>()
    val contact : LiveData<Contact>
        get() = _contact

    // Used to display messages in the fragment
    private val _messages = messageDatabase.getMessagesLive(contactID)
    val messages : LiveData<MutableList<DisplayMessage>>
        get() = _messages

    private var _user : User? = null

    private val _hideKeyboard = MutableLiveData<Boolean>()
    val hideKeyboard : LiveData<Boolean>
        get() = _hideKeyboard

    val inputMessage = MutableLiveData<String>()

    init {
        Log.i("ChatViewViewModel", "Init")
        _hideKeyboard.value = false
        loadMessages(contactID)
        uiScope.launch {
            loadUser()
        }
    }

    fun sendMessage() {
        Log.i("ChatViewViewModel", "Message Sent Pressed")
        val message = inputMessage.value
        if (message != null && !message.contentEquals("")) {
            Log.i("ChatViewViewModel", "Message != null")

            var userId = 12345U
            if (_user != null) {
                userId = _user!!.userId.toUInt()
            }
            var messageId = 0
            if (_messages.value != null) {
                messageId = _messages.value!!.size + 1
            }
            val netMessage = Message(UserId = userId, MessageId = messageId.toUInt(), ContactUserId = contactID.toUInt(), Timestamp = getTimeMillis().toString(), Part = 0U, Message = message)
            uiScope.launch {
                Networking.write(netMessage)
            }

            val displayMessage = netMessage.toDisplayMessage(userId.toLong())
            uiScope.launch {
                insertMessage(displayMessage)
            }

            // Adding the last message to the contact to show in the list preview
            uiScope.launch {
                updateLastMessage(message!!)
            }

//            _messages.value!!.add(displayMessage)


            // TODO: Fix the ID generation, obtaining correct one
//            val displayMessage = DisplayMessage(Random.nextInt(), own = true, content = message, timestamp = "")
//            _localMessages.add(displayMessage)
//            _messages.value = _localMessages
//            _messages.value?.add(message)

            // Making message persistent
            if (_contact.value != null) {
                Log.i("ChatViewViewModel", "Adding new message to contact")
//                _contact.value!!.messages.add(message)
//                updateContact(_contact.value!!)
            }

            // Clearing input and hiding keyboard
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
//            _localMessages = loadMessages()
//            for (m in localContact.messages) {
//                _localMessages.add(DisplayMessage(Random.nextInt(), false, content = m, timestamp = ""))
//            }
//            _messages.value = _localMessages
        }
    }

//    private suspend fun loadMessages() : LiveData<MutableList<DisplayMessage>> {
//        val res = withContext(Dispatchers.IO) {
//            val messages = mDatabase.getMessages(contactID)
//            return@withContext messages
//        }
//        return res
//    }

    private suspend fun updateLastMessage(message : String) {
        withContext(Dispatchers.IO) {
            val contact = contactDatabase.getContact(contactID)
            if (contact != null) {
                contact.lastMessage = message
                contactDatabase.updateContact(contact)
            }
        }
    }

    private suspend fun insertMessage(message: DisplayMessage) {
        withContext(Dispatchers.IO) {
            messageDatabase.insertMessage(message)
        }
    }

    private fun updateContact(contact : Contact) {
        uiScope.launch {
            updateContactInDatabase(contact)
        }
    }

    private suspend fun loadContactFromDatabase(contactID: Long) : Contact {
        val contact = withContext(Dispatchers.IO) {
            val chatContact = contactDatabase.getContact(contactID) ?: throw IOException("User $contactID not found!")
            chatContact
        }
        return contact
    }

    private suspend fun updateContactInDatabase(contact : Contact) {
        withContext(Dispatchers.IO) {
            contactDatabase.updateContact(contact)
        }
    }

    private  suspend fun loadUser() {
        val user = withContext(Dispatchers.IO) {
            return@withContext userDatabase.getUser()
        }
        if (user != null) {
            this._user = user
        }
    }
}