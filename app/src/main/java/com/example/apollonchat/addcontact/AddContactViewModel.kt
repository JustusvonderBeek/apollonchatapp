package com.example.apollonchat.addcontact

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.apollonchat.database.contact.Contact
import com.example.apollonchat.database.contact.ContactDatabaseDao
import com.example.apollonchat.networking.NetworkContact
import com.example.apollonchat.networking.Networking
import com.example.apollonchat.networking.Search
import kotlinx.coroutines.*
import java.net.InetAddress
import kotlin.random.Random
import kotlin.random.nextUInt

class AddContactViewModel(val database : ContactDatabaseDao) : ViewModel() {

    // Suspend functions
    private var viewModelJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    // Data
    private val _contactList = mutableListOf<Contact>()
    private val _contacts = MutableLiveData<List<Contact>>()
    val contacts : LiveData<List<Contact>>
        get() = _contacts

    private val _navigateToContactListEvent = MutableLiveData<Boolean>()
    val navigateToContactListEvent : LiveData<Boolean>
        get() = _navigateToContactListEvent

    val contactName = MutableLiveData<String>()

    init {
        Log.i("AddContactViewModel", "Add Contact VM created")
        contactName.value = ""
        _navigateToContactListEvent.value = false
        _contacts.value = mutableListOf()
        Networking.registerContactViewModel(this)
    }

    fun searchContacts() {
        if (contactName.value != "") {
            // Creating a search user packet with the current name and send to server
            val search = Search(1234U, Random.nextUInt(), contactName.value!!)
            Log.i("AddContactViewModel", "Search: $search")
            // Should make sure that the start thingy was already called
            uiScope.launch {
                withContext(Dispatchers.IO) {
                    Networking.start(InetAddress.getLocalHost()) // TODO: This is NOT correct but since the address is not taken, its good for now!
                    Networking.write(search)
                }
            }

        }
    }

    fun showContacts(contacts : List<NetworkContact>) {
        Log.i("AddContactViewModel", "Showing contacts (${contacts.size})")
        // Clear contact list
        _contactList.clear()
        for (contact in contacts) {
            val newContact = Contact(contactId = contact.UserId.toLong(), contactName = contact.Username, contactImagePath = "@drawable/usericon.png", messages = mutableListOf())
            Log.i("AddContactViewModel", "New contact: $newContact")
            _contactList.add(newContact)
        }
        // Update the observer
        uiScope.launch {
            withContext(Dispatchers.Main) {
                _contacts.value = _contactList
            }
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
        }
    }

    fun onContactListNavigated() {
        _navigateToContactListEvent.value = false
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

}