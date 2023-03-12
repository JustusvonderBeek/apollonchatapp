package com.example.apollonchat.chatlist

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.apollonchat.database.contact.ContactDatabaseDao
import com.example.apollonchat.database.contact.Contact
import com.example.apollonchat.database.user.User
import com.example.apollonchat.database.user.UserDatabaseDao
import kotlinx.coroutines.*
import kotlin.random.Random

class ChatListViewModel(val database : ContactDatabaseDao, val uDatabase : UserDatabaseDao, val application: Application) : ViewModel() {

    private var viewModelJob = Job()

    // Navigation
    private val _navigateToContactChat = MutableLiveData<Long>()
    val navigateToContactChat : LiveData<Long>
        get() = _navigateToContactChat

    // Encapsulated so that no outside methods can modify this
    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)
    // Creating a direct reference to the database.
    private val _contacts = database.getAllContacts()
    val contacts : LiveData<List<Contact>>
        get() = _contacts

    private var _user = uDatabase.getUserAsLive()
    val user : LiveData<User>
        get() = _user

    init {
        Log.i("ChatListViewModel", "ChatListViewModel created")
//        createUsers()
//        initUser()
//        uDatabase.clearUser()
    }

    fun clearUser() {
        uiScope.launch {
            clearCurrentUser()
        }
    }

    private suspend fun clearCurrentUser() {
        withContext(Dispatchers.IO) {
            uDatabase.clearUser()
        }
    }

    fun addUser() {
        var oldList = _contacts.value
        if (oldList == null) {
            oldList = mutableListOf()
        }
        val list = mutableListOf<Contact>()
        var oldId = 1234L
        if (oldList.isNotEmpty())
            oldId = oldList.last().contactId
        val contact = Contact(contactId = oldId + 1, contactName = "Blablabla", contactImagePath = "aisfasfd", messages = mutableListOf("bla", "bla", "Das ist ein langer Text um zzu testen wie das mit dem Layout aussieht usw. Langer Text, Langer Text, Langer Text.............!"))
        // Storing the new contact directly in the room database. This is enough to update the live data reference to the contact list
        // and show the new contact in the recycler view
        storeContact(contact)
    }

    private fun createUsers() {
        Log.i("ChatListViewModel", "Creating users")
        val list = mutableListOf<Contact>()
        val contact = Contact(contactId = Random.nextLong(), contactName = "Blablabla", contactImagePath = "aisfasfd", messages = mutableListOf("This is a last message", "bla"))
        storeContact(contact)
    }

    fun onContactClicked(contactID : Long) {
        _navigateToContactChat.value = contactID
    }

    private fun storeContact(contact: Contact) {
        uiScope.launch {
            storeContactInDatabase(contact)
        }
    }

    private suspend fun storeContactInDatabase(contact : Contact) {
        withContext(Dispatchers.IO) {
            database.insertContact(contact)
        }
    }

    fun onContactNavigated() {
        _navigateToContactChat.value = -1L
    }
}