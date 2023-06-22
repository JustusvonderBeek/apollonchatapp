package com.cloudsheeptechnologies.apollonchat.chatview

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.cloudsheeptechnologies.apollonchat.database.contact.Contact
import com.cloudsheeptechnologies.apollonchat.database.contact.ContactDatabaseDao
import com.cloudsheeptechnologies.apollonchat.database.message.DisplayMessage
import com.cloudsheeptechnologies.apollonchat.database.message.MessageDao
import com.cloudsheeptechnologies.apollonchat.database.user.User
import com.cloudsheeptechnologies.apollonchat.database.user.UserDatabaseDao
import com.cloudsheeptechnologies.apollonchat.networking.ApollonProtocolHandler.ApollonProtocolHandler
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import java.io.IOException

class ChatViewViewModel(val contactID: Long = -1L, val contactDatabase: ContactDatabaseDao, val userDatabase : UserDatabaseDao, val messageDatabase : MessageDao, val application: Application) : ViewModel() {

    private var viewModelJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    // TODO: Refactor and maybe remove if not necessary
    private val _contact = MutableLiveData<Contact>()
    val contact : LiveData<Contact>
        get() = _contact

    // Used to display messages in the fragment
    private val _messages = messageDatabase.messagesByID(contactID)
    val messages : Flow<PagingData<DisplayMessage>> = Pager(
        config = PagingConfig(
            pageSize = 50,
            enablePlaceholders = false,
            maxSize = 300,
        )
    ) {
        messageDatabase.messagesByID(contactID)
    }.flow.map {
        it
    }.cachedIn(viewModelScope)

    private val _lastOnline = MutableLiveData("Last Online: Never")
    val lastOnline : LiveData<String>
        get() = _lastOnline

    private val _scroll = MutableLiveData(-1)
    val scrollToBottom : LiveData<Int>
        get() = _scroll

    private var _user : User? = null
    private var userId : Long = 0L

    private val _hideKeyboard = MutableLiveData<Boolean>()
    val hideKeyboard : LiveData<Boolean>
        get() = _hideKeyboard

    val inputMessage = MutableLiveData<String>()

    val userImage = MutableLiveData<String>()

    init {
        Log.i("ChatViewViewModel", "Init for $contactID")
        _hideKeyboard.value = false
        loadUser(contactID)
        loadMessages(contactID)
        userImage.value = File(application.applicationContext.filesDir, "$contactID.jpeg").absolutePath
//        userImage.value = Uri.parse("android.resource://" + R.drawable.owl).path
    }

    fun sendMessage() {
        Log.i("ChatViewViewModel", "Message Sent Pressed")
        val message = inputMessage.value
        if (message != null && !message.contentEquals("")) {
            Log.i("ChatViewViewModel", "Message != null")

            ApollonProtocolHandler.sendText(message, contactID.toUInt())

            // Clearing input and hiding keyboard
            _hideKeyboard.value = true
            inputMessage.value = ""
        }
    }

    fun showContactInformation() {
        Log.i("ChatViewViewModel", "Showing contact information...")
    }

    fun removeUser() {
        Log.i("ChatViewViewModel", "Removing the user $contactID")
        _contact.value?.let { removeContact(it) }
    }

    fun hideKeyboardDone() {
        _hideKeyboard.value = false
    }

    private fun loadMessages(contactID : Long) {
        uiScope.launch {
            val localContact = loadContactFromDatabase(contactID)
            _contact.value = localContact
            ScrollBottom()
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

    fun ScrollBottom() {
//        this._scroll.value = messages.count()?.let { it.size - 1 }
    }

    fun ScrolledBottom() {
        this._scroll.value = -1

    }

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

    private fun removeContact(contact: Contact) {
        uiScope.launch {
            removeContactFromDatabase(contact)
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

    private suspend fun removeContactFromDatabase(contact: Contact) {
        withContext(Dispatchers.IO) {
            contactDatabase.deleteContact(contact.contactId)
        }
    }

    private fun loadUser(contactID: Long) {
        uiScope.launch {
            loadUserFromDatabase(contactID)
        }
    }

    private suspend fun loadUserFromDatabase(contactID: Long) {
        val user = withContext(Dispatchers.IO) {
            return@withContext userDatabase.getUser()
        }
        if (user != null) {
            this._user = user
            this.userId = user.userId
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }
}