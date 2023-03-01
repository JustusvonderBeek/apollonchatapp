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
    private val _users = MutableLiveData<List<Contact>>()
    val users : LiveData<List<Contact>>
        get() = _users
    private var _user = MutableLiveData<User>()
    val user : LiveData<User>
        get() = _user

    init {
        Log.i("ChatListViewModel", "ChatListViewModel created")
        createUsers()
        initUser()
    }

    fun addUser() {
        var oldList = users.value
        if (oldList == null) {
            oldList = mutableListOf()
        }
        val list = mutableListOf<Contact>()
        var oldId = 1234L
        if (oldList.isNotEmpty())
            oldId = oldList.first().contactId
        val contact = Contact(contactId = (oldId + 1), contactName = "Blablabla", contactImagePath = "aisfasfd", messages = listOf("bla", "bla"))
//        list.plus(user)
        storeContact(contact)
        list.add(contact)
//        _users.postValue(list)
        val newList = list.plus(oldList)
        _users.postValue(newList)
    }

    private fun createUsers() {
        Log.i("ChatListViewModel", "Creating users")
        val list = mutableListOf<Contact>()
        val contact = Contact(contactId = Random.nextLong(), contactName = "Blablabla", contactImagePath = "aisfasfd", messages = listOf("This is a last message", "bla"))
//        list.plus(user)
        storeContact(contact)
        list.add(contact)
        //users.postValue(list)
        _users.postValue(list)
    }

    fun onContactClicked(contactID : Long) {
        _navigateToContactChat.value = contactID
    }

    private fun storeContact(contact: Contact) {
        uiScope.launch {
            storeContactInDatabase(contact)
        }
    }

    private fun initUser() {
        Log.i("ChatListViewModel", "Loading user")
        uiScope.launch {
            val loadedUser = loadUser()
            _user = loadedUser
        }
        Log.i("ChatListViewModel", "Loaded user")
    }

    private suspend fun loadUser() : MutableLiveData<User> {
        val ownUser = withContext(Dispatchers.IO) {
            // Should always return a correct user because of the login screen
            val liveUsers = uDatabase.getUser()
            val content = liveUsers.value
            val firstUser = if (content == null) {
                User(userId = Random.nextLong(), username = "Username", userImage = "drawable/usericon.png")
            } else {
                content[0]
            }
            MutableLiveData(firstUser)
        }
        return ownUser
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