package com.example.apollonchat.chatview

import android.app.Application
import android.util.Log
import androidx.databinding.Observable
import androidx.databinding.ObservableField
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.apollonchat.database.contact.Contact
import com.example.apollonchat.database.contact.ContactDatabaseDao
import com.example.apollonchat.networking.Message
import com.example.apollonchat.networking.Networking
import io.ktor.util.date.*
import kotlinx.coroutines.*
import java.io.IOException
import java.net.Inet4Address
import java.net.InetAddress
import kotlin.random.Random

class ChatViewViewModel(val contactID: Long, val database: ContactDatabaseDao, val application: Application) : ViewModel() {

    private var viewModelJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    private val _contact = MutableLiveData<Contact>()
    val contact : LiveData<Contact>
        get() = _contact

    // Used to display messages in the fragment
    private val _messages = MutableLiveData<List<DisplayMessage>>()
    val messages : LiveData<List<DisplayMessage>>
        get() = _messages

    // Used to add or remove messages to the list
    private val _localMessages = mutableListOf<DisplayMessage>()

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

            val addr = Inet4Address.getLoopbackAddress()
            Log.i("ChatViewViewModel", "Trying to connect to $addr")
            uiScope.launch {
                Networking.start(addr)
            }
            val netMessage = Message(UserId = 12345U, MessageId = 814223U, ContactUserId = 54321U, Timestamp = getTimeMillis().toString(), Part = 0U, Message = message)
            uiScope.launch {
                Networking.write(netMessage)
            }
            // TODO: Fix the ID generation, obtaining correct one
            val displayMessage = DisplayMessage(Random.nextInt(), own = true, content = message, timestamp = "")
            _localMessages.add(displayMessage)
            _messages.value = _localMessages

            // Making message persistent
            _contact.value?.messages!!.add(message)

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
            for (m in localContact.messages) {
                _localMessages.add(DisplayMessage(Random.nextInt(), false, content = m, timestamp = ""))
            }
            _messages.value = _localMessages
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