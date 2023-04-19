package com.example.apollonchat.usercreate

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.apollonchat.database.contact.ContactDatabaseDao
import com.example.apollonchat.database.user.User
import com.example.apollonchat.database.user.UserDatabaseDao
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

    private var json = Json { ignoreUnknownKeys = true }

    init {
        userImage.value = "@drawable/usericon.png"
        _navigateUserListEvent.value = false
        _clickable.value = true
        uiScope.launch {
            registerUserCreationCallback()
        }
    }

    // This method creates a user with the given values from the UI and stores it into the database
    fun createUser() {
        // We get the userId from the server (to avoid duplicates)
        val newUser = User(userId = 0, username = username.value.orEmpty(), userImage = userImage.value.orEmpty())
        Log.i("UserCreationViewModel", "Creating a new user: $newUser")

        // First we need to obtain the ID from the server
        uiScope.launch {
            writeNewUserToServer(newUser)
        }
        // Expecting the Network to signal the answer
        // Therefore disabling the creation button
        _clickable.value = false
    }

    private suspend fun registerUserCreationCallback() {
        Networking.registerCallback(PacketCategories.CONTACT.cat.toLong(), ContactType.CREATE.type.toLong()) { packet, _ ->
            uiScope.launch {
                val createAnswer = json.decodeFromString<Create>(packet)
                val newUser = User(createAnswer.UserId.toLong(), createAnswer.Username, userImage = "drawable/usericon.png")
                userCreated(newUser)
                Log.i("UserCreationViewModel", "Wrote user into database")
                _navigateUserListEvent.value = true
            }
        }
    }

    private suspend fun userCreated(user : User) {
        withContext(Dispatchers.IO) {
            insertNewUserToDatabase(user)
        }
        // The variables must be set on the main thread
        withContext(Dispatchers.Main) {
            _navigateUserListEvent.value = true
        }
    }

    private suspend fun insertNewUserToDatabase(newUser : User) {
        withContext(Dispatchers.IO) {
            userDatabase.clearUser()
            userDatabase.insertUser(newUser)
        }
    }

    private suspend fun writeNewUserToServer(newUser: User) {
        Log.i("UserCreationViewModel", "Writing new user to server")
        withContext(Dispatchers.IO) {
            // Expecting the network to be already started
//            Networking.start(InetAddress.getLocalHost(), cDatabase, database, null)
            val create = Create(Username = newUser.username)
            Networking.write(create)
        }
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