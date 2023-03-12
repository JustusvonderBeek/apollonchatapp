package com.example.apollonchat.usercreate

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.apollonchat.database.user.User
import com.example.apollonchat.database.user.UserDatabaseDao
import com.example.apollonchat.networking.Create
import com.example.apollonchat.networking.Networking
import kotlinx.coroutines.*
import java.net.InetAddress
import kotlin.random.Random
import kotlin.random.nextUInt

class UserCreationViewModel(val database : UserDatabaseDao, val application: Application) : ViewModel() {

    private var viewModelJob = Job()

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }

    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)
    var user = database.getUserAsLive()
    val username = MutableLiveData<String>()
    val userImage = MutableLiveData<String>()

    private var _navigateUserListEvent = MutableLiveData<Boolean>()
    val navigateUserListEvent : LiveData<Boolean>
        get() = _navigateUserListEvent

    init {
        userImage.value = "@drawable/usericon.png"
        _navigateUserListEvent.value = false
    }

    fun createUser() {
        // This method creates a user with the given values from the UI and stores it into the database
        val newUser = User(userId = Random.nextLong(), username = username.value.orEmpty(), userImage = userImage.value.orEmpty())
        Log.i("UserCreationViewModel", "Creating a new user: $newUser")

        // Not blocking the main thread (longer running task)
        uiScope.launch {
            insertNewUserToDatabase(newUser)
        }
        uiScope.launch {
            writeNewUserToServer(newUser)
        }
        _navigateUserListEvent.value = true
    }

    private suspend fun insertNewUserToDatabase(newUser : User) {
        withContext(Dispatchers.IO) {
            database.clearUser()
            database.insertUser(newUser)
        }
    }

    private suspend fun writeNewUserToServer(newUser: User) {
        Log.i("UserCreationViewModel", "Writing new user to server")
        withContext(Dispatchers.IO) {
            Networking.start(InetAddress.getLocalHost())
            val create = Create(UserId = newUser.userId.toUInt(), MessageId = Random.nextUInt(), Username = newUser.username)
            Networking.write(create)
        }
    }

    fun onUserListNavigated() {
        _navigateUserListEvent.value = false
    }
}