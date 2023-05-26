package com.example.apollonchat.addcontact

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.apollonchat.database.contact.Contact
import com.example.apollonchat.database.contact.ContactDatabaseDao
import com.example.apollonchat.database.user.User
import com.example.apollonchat.database.user.UserDatabaseDao
import com.example.apollonchat.networking.ApollonProtocolHandler.ApollonProtocolHandler
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


// TODO: FIX this class
class AddContactViewModel(val userDatabase : UserDatabaseDao, val contactDatabase : ContactDatabaseDao) : ViewModel() {

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

    init {
//        Log.i("AddContactViewModel", "Add Contact ViewModel created")
        registerAddContactCallback()
        contactName.value = ""
        _navigateToContactListEvent.value = false
        _contacts.value = mutableListOf()
        _hideKeyboard.value = false
    }

    private fun registerAddContactCallback() {
        ApollonProtocolHandler.registerContactsCallback { payload ->
            Log.i("AddContactViewModel", "Executing contacts callback")
            // Only accept correct JSON, not some other or missing fields
            val contacts = Json.decodeFromString<ContactList>(payload)
            contacts.Contacts?.let {
                uiScope.launch {
                    showContacts(it)
                }
            }
        }
    }

    // TODO: Fix the search and move to protocol handler
    fun searchContacts() {
        if (contactName.value != "") {
            ApollonProtocolHandler.SendSearch(contactName.value!!)
            _hideKeyboard.value = true
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
                contactDatabase.insertContact(contact)
            } catch (ex : java.lang.Exception) {
                Log.i("AddContactViewModel", "Failed to insert contact into database:\n$ex")
            }
        }
        // Fails if set on a background thread
        withContext(Dispatchers.Main) {
            _navigateToContactListEvent.value = true
        }
    }

    // TODO: Move to protocol handler
    private suspend fun sendContactRequestToServer(contact: Contact) {
        withContext(Dispatchers.IO) {
            ApollonProtocolHandler.SendContactRequest(contact.contactId.toUInt())
        }
    }

}