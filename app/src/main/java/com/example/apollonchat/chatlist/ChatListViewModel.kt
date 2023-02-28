package com.example.apollonchat.chatlist

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
//import com.example.apollonchat.database.ChatUserDatabaseDao
import com.example.apollonchat.database.User
import kotlin.random.Random
import kotlin.random.nextUInt

class ChatListViewModel(application: Application) : ViewModel() {

    // Encapsulated so that no outside methods can modify this
    private val _users = MutableLiveData<List<User>>()
    val users : LiveData<List<User>>
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
        val list = mutableListOf<User>()
        var oldId = 1000u
        if (oldList.isNotEmpty())
            oldId = oldList.last().userId
        val user = User(userId = oldId + 1u, username = "Blablabla", userimage = "aisfasfd", messages = listOf("bla", "bla"))
//        list.plus(user)
        list.add(user)
//        _users.postValue(list)
        val newList = list.plus(_users.value.orEmpty())
        _users.postValue(newList)
    }

    private fun createUsers() {
        Log.i("ChatListViewMode", "Creating users")
        val list = mutableListOf<User>()
        val user = User(userId = Random.nextUInt(), username = "Blablabla", userimage = "aisfasfd", messages = listOf("This is a last message", "bla"))
//        list.plus(user)
        list.add(user)
        //users.postValue(list)
        _users.postValue(list)
    }
}