package com.example.apollonchat.addcontact

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.apollonchat.database.contact.Contact
import com.example.apollonchat.database.contact.ContactDatabaseDao
import com.example.apollonchat.database.user.User
import com.example.apollonchat.database.user.UserDatabaseDao
import com.example.apollonchat.networking.Networking
import com.example.apollonchat.networking.constants.ContactType
import com.example.apollonchat.networking.constants.PacketCategories
import com.example.apollonchat.networking.packets.*
import kotlinx.coroutines.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.net.InetAddress
import kotlin.random.Random
import kotlin.random.nextUInt

class AddContactViewModel(val uDatabase : UserDatabaseDao, val database : ContactDatabaseDao) : ViewModel() {

    // Suspend functions
    private var viewModelJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }

    // Data
    private val _contactList = mutableListOf<Contact>()
    private val _contacts = MutableLiveData<List<Contact>>()
    val contacts : LiveData<List<Contact>>
        get() = _contacts

    private val _navigateToContactListEvent = MutableLiveData<Boolean>()
    val navigateToContactListEvent : LiveData<Boolean>
        get() = _navigateToContactListEvent

    private val _hideKeyboard = MutableLiveData<Boolean>()
    val hideKeyboard : LiveData<Boolean>
        get() = _hideKeyboard

    val contactName = MutableLiveData<String>()

    private var _user : User? = null

    private var json = Json { ignoreUnknownKeys = true }

    init {
        Log.i("AddContactViewModel", "Add Contact VM created")
        uiScope.launch {
            loadUser()
        }
        uiScope.launch {
            registerAddContactCallback()
        }
        contactName.value = ""
        _navigateToContactListEvent.value = false
        _contacts.value = mutableListOf()
        _hideKeyboard.value = false
//        Networking.registerContactViewModel(this)
    }

    private fun registerAddContactCallback() {
        Networking.registerCallback(PacketCategories.CONTACT.cat.toLong(), ContactType.CONTACTS.type.toLong()) {packet ->
            Log.i("AddContactViewModel", "Executing contacts callback")
            val contacts = json.decodeFromString<ContactList>(packet)
            contacts.Contacts?.let {
                uiScope.launch {
                    showContacts(it)
                }
            }
        }
    }

    fun searchContacts() {
        if (contactName.value != "") {
            // Creating a search user packet with the current name and send to server
            var id = 1234L
            if (_user != null) {
                id = _user!!.userId
            }
            val search = Search(id.toUInt(), Random.nextUInt(), contactName.value!!)
            Log.i("AddContactViewModel", "Search: $search")
            // Should make sure that the start thingy was already called
            uiScope.launch {
                writeSearchPacket(search)
            }
            _hideKeyboard.value = true
        }
    }

    private suspend fun writeSearchPacket(search: Search) {
        withContext(Dispatchers.IO) {
            Networking.write(search)
        }
    }

    private suspend fun showContacts(contacts : List<NetworkContact>) {
        Log.i("AddContactViewModel", "Showing contacts (${contacts.size})")
        // Clear contact list
        _contactList.clear()
        for (contact in contacts) {
            val newContact = Contact(contactId = contact.UserId.toLong(), contactName = contact.Username, contactImagePath = "@drawable/usericon.png", lastMessage = "")
            Log.i("AddContactViewModel", "New contact: $newContact")
            _contactList.add(newContact)
        }
        withContext(Dispatchers.Main) {
            // Update the observer
            _contacts.value = _contactList
        }
    }

    fun addContact(contactId : Long) {
        val filteredList = _contactList.filter { cont -> cont.contactId == contactId }
        if (filteredList.size != 1) {
            Log.i("AddContactViewModel", "The filter applied to more than a single contact! That should not be possible")
        }
        val newContact = filteredList.firstOrNull()
        newContact?.let {
            uiScope.launch {
                insertContactIntoDatabase(it)
            }
            uiScope.launch {
                sendContactRequestToServer(it)
            }
        }
    }

    fun onContactListNavigated() {
        _contactList.clear()
        _contacts.value = listOf()
        _navigateToContactListEvent.value = false
    }

    fun hideKeyboardDone() {
        _hideKeyboard.value = false
    }

    private suspend fun insertContactIntoDatabase(contact: Contact) {
        withContext(Dispatchers.IO) {
            try {
//                database.clearContacts()
                database.insertContact(contact)
            } catch (ex : java.lang.Exception) {
                Log.i("AddContactViewModel", "Failed to insert contact into database:\n$ex")
            }
        }
        // Fails if set on a background thread
        withContext(Dispatchers.Main) {
            _navigateToContactListEvent.value = true
        }
    }

    private suspend fun sendContactRequestToServer(contact: Contact) {
        withContext(Dispatchers.IO) {
            var userId = 1U
            if (_user != null) {
               userId = _user!!.userId.toUInt()
            }
            val options = listOf(NetworkOption("Question", "Add"))
            val addOption = ContactOption(userId, contact.contactId.toUInt(), options)
            Networking.write(addOption)
        }
    }

    private suspend fun loadUser() {
        val user = withContext(Dispatchers.IO) {
            return@withContext uDatabase.getUser()
        }
        if (user != null) {
            _user = user
        }
    }

}