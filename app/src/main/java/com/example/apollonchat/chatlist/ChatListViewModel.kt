package com.example.apollonchat.chatlist

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.apollonchat.database.contact.ContactDatabaseDao
import com.example.apollonchat.database.contact.Contact
import kotlin.random.Random

class ChatListViewModel(database : ContactDatabaseDao, application: Application) : ViewModel() {

    // Encapsulated so that no outside methods can modify this
    private val _users = MutableLiveData<List<Contact>>()
    val users : LiveData<List<Contact>>
        get() = _users
    private val _testVal = MutableLiveData<String>()
    val testVal : LiveData<String>
        get() = _testVal

    init {
        Log.i("ChatListViewModel", "ChatListViewModel created")
        createUsers()
        _testVal.value = "Test"
    }

    fun updateText() {
        _testVal.value = Random.nextInt().toString()
    }

    fun addUser() {
        var oldList = users.value
        if (oldList == null) {
            oldList = mutableListOf()
        }
        val list = mutableListOf<Contact>()
        var oldId = 1000L
        if (oldList.isNotEmpty())
            oldId = oldList.last().contactId
        val contact = Contact(contactId = oldId + 1L, contactName = "Blablabla", contactImagePath = "aisfasfd", messages = listOf("bla", "bla"))
//        list.plus(user)
        list.add(contact)
//        _users.postValue(list)
        val newList = list.plus(_users.value.orEmpty())
        _users.postValue(newList)
    }

    private fun createUsers() {
        Log.i("ChatListViewMode", "Creating users")
        val list = mutableListOf<Contact>()
        val contact = Contact(contactId = Random.nextLong(), contactName = "Blablabla", contactImagePath = "aisfasfd", messages = listOf("This is a last message", "bla"))
//        list.plus(user)
        list.add(contact)
        //users.postValue(list)
        _users.postValue(list)
    }
}