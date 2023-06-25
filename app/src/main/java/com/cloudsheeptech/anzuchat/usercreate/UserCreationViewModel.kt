package com.cloudsheeptech.anzuchat.usercreate

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cloudsheeptech.anzuchat.database.user.User
import com.cloudsheeptech.anzuchat.database.user.UserDatabaseDao
import com.cloudsheeptech.anzuchat.networking.ApollonProtocolHandler.ApollonProtocolHandler
import com.cloudsheeptech.anzuchat.networking.Networking
import kotlinx.coroutines.*

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

    private val _waiting = MutableLiveData<Boolean>()
    val waiting : LiveData<Boolean>
        get() = _waiting

    private var _navigateUserListEvent = MutableLiveData<Boolean>()
    val navigateUserListEvent : LiveData<Boolean>
        get() = _navigateUserListEvent

    init {
        userImage.value = "@drawable/usericon.png"
        _navigateUserListEvent.value = false
        _clickable.value = true
        _waiting.value = false
    }

    // This method creates a user with the given values from the UI and stores it into the database
    fun createUser() {
        // We get the userId from the server (to avoid duplicates)
        val newUser = User(userId = 0, username = username.value.orEmpty(), userImage = userImage.value.orEmpty())
        Log.i("UserCreationViewModel", "Creating a new user: $newUser")

        // First we need to obtain the ID from the server
        ApollonProtocolHandler.sendCreateAccount(newUser)
        // Expecting the Network to signal the answer
        // Therefore disabling the creation button
        _clickable.value = false
        _waiting.value = true
    }

    fun reconnectNetwork() {
        uiScope.launch {
            Networking.start(application.applicationContext)
        }
    }

    fun onUserListNavigated() {
        _navigateUserListEvent.value = false
        _waiting.value = false

    }

}