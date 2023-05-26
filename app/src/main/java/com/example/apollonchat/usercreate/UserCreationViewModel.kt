package com.example.apollonchat.usercreate

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.apollonchat.database.contact.ContactDatabaseDao
import com.example.apollonchat.database.user.User
import com.example.apollonchat.database.user.UserDatabaseDao
import com.example.apollonchat.networking.ApollonProtocolHandler.ApollonProtocolHandler
import com.example.apollonchat.networking.packets.Create
import com.example.apollonchat.networking.Networking
import com.example.apollonchat.networking.constants.ContactType
import com.example.apollonchat.networking.constants.PacketCategories
import kotlinx.coroutines.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.net.InetAddress
import kotlin.random.Random
import kotlin.random.nextUInt

class UserCreationViewModel(val userDatabase : UserDatabaseDao, val application: Application) : ViewModel() {

    // Context for async running jobs
    private var viewModelJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }

    /*
    * -----------------------------------------------------------
    * Parameter
    * -----------------------------------------------------------
    */

    var user = userDatabase.getUserAsLive()
    val username = MutableLiveData<String>()
    // TODO: Add user image to view and allow selection
    val userImage = MutableLiveData<String>()
    // Disable the create button if creation was already signalled
    private val _clickable = MutableLiveData<Boolean>()
    val clickable : LiveData<Boolean>
        get() = _clickable

    private var _navigateUserListEvent = MutableLiveData<Boolean>()
    val navigateUserListEvent : LiveData<Boolean>
        get() = _navigateUserListEvent

    init {
        userImage.value = "@drawable/usericon.png"
        _navigateUserListEvent.value = false
        _clickable.value = true
    }

    // This method creates a user with the given values from the UI and stores it into the database
    fun createUser() {
        // We get the userId from the server (to avoid duplicates)
        val newUser = User(userId = 0, username = username.value.orEmpty(), userImage = userImage.value.orEmpty())
        Log.i("UserCreationViewModel", "Creating a new user: $newUser")

        // First we need to obtain the ID from the server
        ApollonProtocolHandler.SendCreateAccount(newUser)
        // Expecting the Network to signal the answer
        // Therefore disabling the creation button
        _clickable.value = false
    }

    fun reconnectNetwork() {
        uiScope.launch {
            Networking.start(application.applicationContext)
        }
    }

    fun onUserListNavigated() {
        _navigateUserListEvent.value = false
    }
}