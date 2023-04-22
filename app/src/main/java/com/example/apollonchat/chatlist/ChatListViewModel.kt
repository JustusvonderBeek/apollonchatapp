package com.example.apollonchat.chatlist

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.apollonchat.database.contact.ContactDatabaseDao
import com.example.apollonchat.database.contact.Contact
import com.example.apollonchat.database.message.DisplayMessage
import com.example.apollonchat.database.message.MessageDao
import com.example.apollonchat.database.user.User
import com.example.apollonchat.database.user.UserDatabaseDao
import com.example.apollonchat.networking.ApollonProtocolHandler.ApollonProtocolHandler
import com.example.apollonchat.networking.Networking
import com.example.apollonchat.networking.constants.ContactType
import com.example.apollonchat.networking.constants.DataType
import com.example.apollonchat.networking.constants.PacketCategories
import com.example.apollonchat.networking.packets.ContactInfo
import com.example.apollonchat.networking.packets.ContactOption
import com.example.apollonchat.networking.packets.Login
import com.example.apollonchat.networking.packets.Message
import com.example.apollonchat.networking.packets.NetworkOption
import com.github.luben.zstd.Zstd
import com.github.luben.zstd.ZstdBufferDecompressingStream
import kotlinx.coroutines.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File
import java.net.InetAddress
import kotlin.random.Random

class ChatListViewModel(val contactDatabase : ContactDatabaseDao, val userDatabase : UserDatabaseDao, val messageDatabase : MessageDao, val application: Application) : ViewModel() {

    // Encapsulated so that no outside methods can modify this
    private var viewModelJob = Job()
    private var uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }

    // Navigation
    private val _navigateToContactChat = MutableLiveData<Long>()
    val navigateToContactChat : LiveData<Long>
        get() = _navigateToContactChat

    // Creating a direct reference to the database.
    private val _contacts = contactDatabase.getAllContacts()
    val contacts : LiveData<List<Contact>>
        get() = _contacts

    private var _user = userDatabase.getUserAsLive()
    val user : LiveData<User>
        get() = _user

    private var json = Json { ignoreUnknownKeys = true }

    init {
        Log.i("ChatListViewModel", "ChatListViewModel created")
        uiScope.launch {
            startNetwork()
        }
        uiScope.launch {
            registerCallbackForIncomingMessages()
        }
        uiScope.launch {
            registerCallbackForFriendRequests()
        }
        uiScope.launch {
            registerCallbackForContactInfo()
        }
    }

    private suspend fun registerCallbackForIncomingMessages() {
        withContext(Dispatchers.IO) {
            Networking.registerCallback(PacketCategories.DATA.cat.toLong(), DataType.TEXT.type.toLong()) { packet, _ ->
                val message = json.decodeFromString<Message>(packet)
                var oldMessageId = 0
                if (messageDatabase.getMessages(message.UserId.toLong()) != null) {
                    oldMessageId = messageDatabase.getMessages(message.UserId.toLong())!!.size + 1
                }
                val dm = DisplayMessage(oldMessageId.toLong(), message.UserId.toLong(), false, message.Message, message.Timestamp)
                messageDatabase.insertMessage(dm)
                // Update the last message into the contact user database to be able to show in the preview
                val contact = contactDatabase.getContact(message.UserId.toLong())
                if (contact != null) {
                    contact.lastMessage = message.Message
                    contactDatabase.updateContact(contact)
                }
            }
        }
    }

    private suspend fun registerCallbackForContactInfo() {
        Log.i("ChatListViewModel", "Got incoming ContactInfo packet")
        withContext(Dispatchers.IO) {
            Networking.registerCallback(PacketCategories.CONTACT.cat.toLong(), ContactType.CONTACT_INFO.type.toLong()) { packet, input ->
                val infoHeader = json.decodeFromString<ContactInfo>(packet)
                var imageLength = infoHeader.ImageBytes
                var imageBuffer = ByteArray(imageLength.toInt())
                var read = input.read(imageBuffer)
                if (read < imageLength.toInt()) {
                    Log.i("ChatListViewModel", "Did not read all bytes in one go")
                    return@registerCallback
                }
                Log.i("ChatListViewModel", "Got $read bytes of image data")
                // TODO: Handle image
//                var storageFile = File(application.applicationContext.filesDir, "${infoHeader.UserId}")
                // Decompress from Zstd to png or jpeg
                var decompressBuffer = ByteArray(imageLength.toInt() * 2)
                Zstd.decompress(decompressBuffer, imageBuffer)
                var storageFile = File(application.applicationContext.filesDir, "${infoHeader.UserId}.jpeg")
                storageFile.writeBytes(decompressBuffer)
                Log.i("ChatListViewModel", "Stored image to ${storageFile.absolutePath}")
            }
        }
    }

    private suspend fun registerCallbackForFriendRequests() {
        withContext(Dispatchers.IO) {
            Networking.registerCallback(PacketCategories.CONTACT.cat.toLong(), ContactType.OPTION.type.toLong()) { packet, _ ->
                Log.i("ChatListViewModel","Got incoming friend request: $packet")
                val option = json.decodeFromString<ContactOption>(packet)
                var contactRequest = false
                for (opt in option.Options) {
                    when {
                        opt.Type.contentEquals("Question") && opt.Value.contentEquals("Add") -> {
                            // Other user wants to add us to the contact list
                            // Accept by default for now and send answer back
                            var userId = 1U
                            if (_user.value != null) {
                                userId = _user.value!!.userId.toUInt()
                            }
                            var username = "Username"
                            if (_user.value != null) {
                                username = _user.value!!.username
                            }
                            val answerOptions = listOf<NetworkOption>(NetworkOption("Answer", "Accept"), NetworkOption("Username", username))
                            val answer = ContactOption(userId, option.UserId, answerOptions)
                            Networking.write(answer)
                            // TODO: Obtain friend information and add to list of contacts
                            val newContact = Contact(option.UserId.toLong(), "I dont know", "drawable/usericon.png")
                            contactDatabase.insertContact(newContact)
                        }
                    }
                }
            }
        }
    }

    private suspend fun startNetwork() {
        withContext(Dispatchers.IO) {
            try {
                // Resolving the network address without Internet results in a failure
                Networking.initialize(InetAddress.getByName("10.0.2.2"), contactDatabase, userDatabase, messageDatabase, tls = false)
//            Networking.initialize(InetAddress.getByName("homecloud.homeplex.org"), contactDatabase, userDatabase, messageDatabase, tls = true)
                Networking.start(application.applicationContext)

                // Login only makes sense if we send the correct UInt
                if (_user.value != null) {
                    val userId = _user.value!!.userId.toUInt()
                    ApollonProtocolHandler.Initilize(userId.toInt(), application)

//                    val login = Login(userId)
//                    Log.i("ChatListViewModel", "Sending login $login")
//                    Networking.write(login)
                }
                return@withContext
            } catch (ex : Exception) {
                Log.i("ChatListViewModel", "Failed to resolve addr: $ex")
            }
        }
    }


    fun clearUser() {
        uiScope.launch {
            clearCurrentUser()
        }
    }

    fun clearContacts() {
        uiScope.launch {
            clearAllContacts()
        }
    }

    private suspend fun clearCurrentUser() {
        withContext(Dispatchers.IO) {
            userDatabase.clearUser()
        }
    }

    private suspend fun clearAllContacts() {
        withContext(Dispatchers.IO) {
            contactDatabase.clearContacts()
        }
    }

    private suspend fun reconnectNetwork() {
        withContext(Dispatchers.IO) {
            Networking.start(application.applicationContext)
        }
    }

    fun onContactClicked(contactID : Long) {
        _navigateToContactChat.value = contactID
    }

    fun onContactNavigated() {
        _navigateToContactChat.value = -1L
    }
}