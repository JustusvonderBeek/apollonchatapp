package com.example.apollonchat.usercreate

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.apollonchat.database.user.User
import com.example.apollonchat.database.user.UserDatabaseDao
import kotlinx.coroutines.*
import kotlin.random.Random

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
        userImage.value = "drawable/usericon.png"
        _navigateUserListEvent.value = false
        loadUser()
    }

    private fun loadUser() {
        uiScope.launch {
            val localUser = loadUserFromDatabase()
            if (localUser != null) {
                _navigateUserListEvent.value = true
//                user.value = localUser
            }
        }
    }

    fun createUser() {
        // This method creates a user with the given values from the UI and stores it into the database
        val newUser = User(userId = Random.nextLong(), username = username.value.orEmpty(), userImage = userImage.value.orEmpty())
        Log.i("UserCreationViewModel", "Creating a new user: $newUser")

        // Not blocking the main thread (longer running task)
        uiScope.launch {
            insertNewUserToDatabase(newUser)
        }
        _navigateUserListEvent.value = true
    }

    private suspend fun loadUserFromDatabase() : User? {
        withContext(Dispatchers.IO) {
            return@withContext database.getUser()
        }
        return null
    }

    private suspend fun insertNewUserToDatabase(newUser : User) {
        withContext(Dispatchers.IO) {
            database.insertUser(newUser)
        }
    }

    fun onUserListNavigated() {
        _navigateUserListEvent.value = false
    }
}