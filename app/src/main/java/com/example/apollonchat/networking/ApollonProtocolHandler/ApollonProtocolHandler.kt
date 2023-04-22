package com.example.apollonchat.networking.ApollonProtocolHandler

import android.app.Application
import android.hardware.biometrics.BiometricManager.Strings
import android.icu.lang.UCharacter
import android.util.Log
import com.example.apollonchat.database.ApollonDatabase
import com.example.apollonchat.database.contact.Contact
import com.example.apollonchat.database.contact.ContactDatabaseDao
import com.example.apollonchat.database.message.DisplayMessage
import com.example.apollonchat.database.message.MessageDao
import com.example.apollonchat.database.user.User
import com.example.apollonchat.database.user.UserDatabaseDao
import com.example.apollonchat.networking.Networking
import com.example.apollonchat.networking.Networking.messageDatabase
import com.example.apollonchat.networking.Networking.userDatabase
import com.example.apollonchat.networking.constants.ContactType
import com.example.apollonchat.networking.constants.DataType
import com.example.apollonchat.networking.constants.PacketCategories
import com.example.apollonchat.networking.packets.ContactInfo
import com.example.apollonchat.networking.packets.ContactOption
import com.example.apollonchat.networking.packets.Header
import com.example.apollonchat.networking.packets.Login
import com.example.apollonchat.networking.packets.Message
import com.example.apollonchat.networking.packets.NetworkOption
import com.github.luben.zstd.Zstd
import io.ktor.network.sockets.openReadChannel
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream

/*
* Instead of using a class we use an object.
* This creates a singleton making all accesses to this class to the same instance
* preventing any duplication or misses
 */
object ApollonProtocolHandler {
    // TODO: Implement the Apollon Protocol here and group all operations
    // in a single place to keep it concise and easy to understand

    private var userId : Int = 0
    private var unackedPackets : MutableList<Pair<UInt, ByteArray>> = ArrayList()
    private var unhandledPackets : MutableList<ByteArray> = ArrayList()
    private var json = Json { ignoreUnknownKeys = true }
    private var application : Application? = null
    private var user : User? = null
    private var messageDatabase : MessageDao? = null
    private var contactDatabase : ContactDatabaseDao? = null
    private var incomingStream : InputStream? = null

    private var protocolJobs = Job()
    private var protocolScope = CoroutineScope(Dispatchers.Main + protocolJobs)

    suspend fun ReceiveAny(packet : ByteArray) {
        val sPacket = packet.toString(Charsets.UTF_8)
        val header = json.decodeFromString<Header>(sPacket)
        when(header.Category.toLong()) {
            PacketCategories.CONTACT.cat.toLong() -> {
                when(header.Type.toLong()) {
                    ContactType.CONTACTS.type.toLong() -> {

                    }
                    ContactType.OPTION.type.toLong() -> {

                    }
                    ContactType.CONTACT_INFO.type.toLong() -> {
//                        protocolScope.launch {
                            ReceiveContactInformation(packet)
//                        }
                    }
                    ContactType.CONTACT_ACK.type.toLong() -> {
                        val ackedID = header.MessageId
                        unackedPackets.removeIf {
                            pair -> pair.first == ackedID
                        }
                    }
                    else -> {
                        Log.i("ApollonProtocolHandler", "Got incorrect type or packet that is not meant for client sides. mID: ${header.MessageId}")
                        // TODO: Decide how to handle incorrect packets
                    }
                }
            }
            PacketCategories.DATA.cat.toLong() -> {
                when(header.Type.toLong()) {
                    DataType.TEXT.type.toLong() ->  {
                        protocolScope.launch {
                            ReceiveTextMessage(packet)
                        }
                    }
                    DataType.TEXT_ACK.type.toLong() -> {
                        // Extract the messageID that was acked
                        val ackedID = header.MessageId
                        unackedPackets.removeIf {
                            pair -> pair.first == ackedID
                        }
                    }
                    else -> {
                        Log.i("ApollonProtocolHandler", "Got incorrect data type. mID: ${header.MessageId}")
                        // Skip the packet
                    }
                }
            }
            else -> {
                Log.i("ApollonProtocolHandler", "Got incorrect packet category (${header.Category}")
                // Skip this packet
            }
        }
    }

    private fun SendAny(messageId : UInt, packet : ByteArray) {
        // Get the message ID of the packet to ack later
        val pair = Pair(messageId, packet)
        unackedPackets.add(pair)
        protocolScope.launch {
            Networking.write(packet)
        }
    }

    private suspend fun ReceiveContactInformation(packet: ByteArray) {
        if (contactDatabase == null || messageDatabase == null) {
            throw IllegalStateException("Protocol Handler not initialized! Wrong usage!")
        }
        withContext(Dispatchers.IO) {
            Log.i("ApollonProtocolHandler", "Expecting Contact Information")
            val info = json.decodeFromString<ContactInfo>(packet.toString(Charsets.UTF_8))
            val imageLength = info.ImageBytes
            val imageBuffer = ByteArray(imageLength.toInt())
            val read = Networking.incomingChannel!!.read(imageBuffer)
            if (read < imageLength.toInt()) {
                Log.i("ApollonProtocolHandler", "Failed to read all image bytes!")
                return@withContext
            }
            Log.i("ApollonProtocolHandler", "Got $read bytes of image data")
            // Include the information how much compression is in packet
            val decomImage = ByteArray(imageLength.toInt() * 2)
            Zstd.decompress(decomImage, imageBuffer)
            val storageFile = File(application!!.applicationContext.filesDir, "${info.UserId}.${UCharacter.toLowerCase(info.ImageFormat)}")
            storageFile.writeBytes(decomImage)
            Log.i("ApollonProtocolHandler", "Stored contact ${info.UserId} picture under ${storageFile.absolutePath}")
        }
    }

    private suspend fun ReceiveTextMessage(packet : ByteArray) {
        if (contactDatabase == null || messageDatabase == null) {
            throw IllegalStateException("Protocol Handler not initialized! Wrong usage!")
        }
        withContext(Dispatchers.IO) {
            val message = json.decodeFromString<Message>(packet.toString(Charsets.UTF_8))
            // Inserting the new message into the database
            var prevId = 0L
            val messages = messageDatabase!!.getMessages(message.UserId.toLong())
            if (messages != null) {
                prevId = messages.size + 1L
            }
            val localMessage = DisplayMessage(prevId, message.UserId.toLong(), false, message.Message, message.Timestamp)
            messageDatabase!!.insertMessage(localMessage)
            val contact = contactDatabase!!.getContact(message.UserId.toLong())
            if (contact == null) {
                Log.i("ApollonProtocolHandler", "Failed to find user for ID ${message.UserId}")
                // Should kill the connection?
                return@withContext
            }
            contact.lastMessage = message.Message
            contactDatabase!!.updateContact(contact)
        }
    }

    private suspend fun ReceiveFriendRequest(packet: ByteArray) {
        if (contactDatabase == null || messageDatabase == null) {
            throw IllegalStateException("Protocol Handler not initialized! Wrong usage!")
        }
        withContext(Dispatchers.IO) {
            if (user == null) {
                // Keep the packet for later so that it can be properly handled
                unhandledPackets.add(packet)
                return@withContext
            }
            val request = json.decodeFromString<ContactOption>(packet.toString(Charsets.UTF_8))
            var contactRequest = false
            var username = "Username"
            for (opt in request.Options) {
                if (opt.Type.contentEquals("Question") && opt.Value.contentEquals("Add")) {
                    // Found a friend request
                    contactRequest = true
                }
                if (contactRequest && opt.Type.contentEquals("Username")) {
                    username = opt.Value
                }
            }
            // TODO: Include a way for the user to accept or decline the request
            // For now instant accept
            val answerOptions = listOf(NetworkOption("Answer", "Accept"), NetworkOption("Username", user!!.username))
            val answer = ContactOption(user!!.userId.toUInt(), request.UserId, answerOptions)
            Networking.write(answer)

            // Saving the new contact in the list of contacts
            val imageFile = File(application!!.applicationContext.filesDir, "${request.UserId}.png")
            val newContact = Contact(request.UserId.toLong(), username, imageFile.absolutePath)
            contactDatabase!!.insertContact(newContact)
        }
    }

    private suspend fun LoadUser(userDatabase : UserDatabaseDao) {
        val user = withContext(Dispatchers.IO) {
            return@withContext userDatabase.getUser()
        }
        if (user != null) {
            this.user = user
        }
    }

    private suspend fun Login() {
        withContext(Dispatchers.IO) {
            val login = Login(userId.toUInt())
            Networking.write(login)
        }
    }

    // ---------------------------------------------------
    // Public API
    // ---------------------------------------------------

    fun Initilize(userId : Int, application: Application) {
        val database = ApollonDatabase.getInstance(application)
        val userDatabase = database.userDao()
        protocolScope.launch {
            LoadUser(userDatabase)
        }
        contactDatabase = database.contactDao()
        messageDatabase = database.messageDao()
        this.userId = userId

        protocolScope.launch {
            Login()
        }

        // The following is VERY wild!!! Consider this a work-around and not a solution
        // The Network Stream handled in the Networking class read in another class
//        incomingStream = Networking.socket!!.openReadChannel().toInputStream()
    }

    fun SendText() {

    }

    fun SendContactInformationUpdate() {

    }

    fun SendCreateAccount() {

    }

}