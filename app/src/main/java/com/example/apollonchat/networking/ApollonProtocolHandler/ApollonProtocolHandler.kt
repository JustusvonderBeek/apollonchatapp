package com.example.apollonchat.networking.ApollonProtocolHandler

import android.app.Application
import android.content.Context
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
import com.example.apollonchat.networking.constants.ContactType
import com.example.apollonchat.networking.constants.DataType
import com.example.apollonchat.networking.constants.PacketCategories
import com.example.apollonchat.networking.packets.ContactInfo
import com.example.apollonchat.networking.packets.ContactList
import com.example.apollonchat.networking.packets.ContactOption
import com.example.apollonchat.networking.packets.Header
import com.example.apollonchat.networking.packets.Message
import com.example.apollonchat.networking.packets.NetworkContact
import com.example.apollonchat.networking.packets.NetworkOption
import com.github.luben.zstd.Zstd
import io.ktor.util.date.getTimeMillis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecoder
import java.io.File
import java.io.InputStream
import java.lang.Exception
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random
import kotlin.random.nextUInt

/*
* Instead of using a class we use an object.
* This creates a singleton making all accesses to this class to the same instance
* preventing any duplication or misses
 */
object ApollonProtocolHandler {
    // TODO: Cleanup and divide this section visually from the methods below
    // TODO: Include saving mechanism for outgoing messages

    private var userId : UInt = 0U
    private var randomness : Random = Random(LocalDateTime.now().nano)
    private var messageID : AtomicInteger = AtomicInteger(randomness.nextInt())
    private var unackedPackets : MutableList<Pair<UInt, ByteArray>> = ArrayList()
    // TODO: What is this for?
    private var unhandledPackets : MutableList<ByteArray> = ArrayList()
    private var ignoreUnknownJson = Json { ignoreUnknownKeys = true }
    private var imageFileDir : String? = null
    private var user : User? = null
    private var messageDatabase : MessageDao? = null
    private var contactDatabase : ContactDatabaseDao? = null

    private var protocolJobs = Job()
    private var protocolScope = CoroutineScope(Dispatchers.Main + protocolJobs)

    // If this stays the only callback, then handling it exclusively is fine
    private var contactsCallback : ((String) -> Unit)? = null

    // ---------------------------------------------------
    // Public API
    // ---------------------------------------------------

    fun initialize(userId : UInt, context: Context) {
        this.imageFileDir = context.filesDir.absolutePath
        val database = ApollonDatabase.getInstance(context)
        val userDatabase = database.userDao()
        protocolScope.launch {
            LoadUser(userDatabase)
        }
        contactDatabase = database.contactDao()
        messageDatabase = database.messageDao()
        this.userId = userId

//        Networking.registerCallback(this::ReceiveAny)

        protocolScope.launch {
            Login()
        }
    }


    // Main method for initial packet handling
    fun ReceiveAny(headerBuffer : ByteArray, incomingStream: InputStream) {
        val header = Header.convertRawToHeader(headerBuffer) ?: return
        // We cannot always read payload, because of packets that don't have a payload
        when(header.Category.toLong()) {
            PacketCategories.CONTACT.cat.toLong() -> {
                when(header.Type.toLong()) {
                    ContactType.CONTACTS.type.toLong() -> {
                        // Send this further to the contact view model?
                        // Or maybe let this one handle this alone?
                        // We need to "clean" the pipe, so read anyways
                        val payload = incomingStream.bufferedReader(Charsets.UTF_8).readLine()
                        if (contactsCallback != null) {
                            contactsCallback!!.invoke(payload)
                        }
                    }
                    ContactType.OPTION.type.toLong() -> {
                        // Differentiate further
                        val payload = incomingStream.bufferedReader(Charsets.UTF_8).readLine()
                        protocolScope.launch {
                            receiveContactOption(header, payload)
                        }
                    }
                    ContactType.CONTACT_INFO.type.toLong() -> {
                        val payload = incomingStream.bufferedReader(Charsets.UTF_8).readLine()
                        protocolScope.launch {
                            receiveContactInformation(header, payload)
                        }
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
                        // Receive only payload and then free the stream to continue receiving
                        val payload = incomingStream.bufferedReader(Charsets.UTF_8).readLine()
                        protocolScope.launch {
                            receiveTextMessage(header, payload)
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

    // TODO: Implement missing parts of the protocol
    fun SendText(text : String, to : UInt) {
        try {
            if (text.contentEquals("")) {
                Log.i("ApollonProtocolHandler", "Empty string \"\" to send. Returning")
                return
            }
            // Get given UserID
            // Get current MessageID
            val mID = messageID.getAndAdd(1).toUInt()
            val header = Header(PacketCategories.DATA.cat.toByte(), DataType.TEXT.type.toByte(), userId, mID)
            val message = Message(to, getTimeMillis().toString(), text)
            SendAny(mID, header, message)
        } catch (ex : NullPointerException) {
            Log.i("ApollonProtocolHandler", "Internal programming error!")
            ex.printStackTrace()
        } catch (ex : Exception) {
            Log.i("ApollonProtocolHandler", "Failed to send text message!\n$ex")
        }
    }

    fun SendContactInformationUpdate() {

    }

    fun SendCreateAccount() {

    }

    fun registerContactsCallback(callback : (String) -> Unit) {
        this.contactsCallback = callback
    }


    // ---------------------------------------------------
    // Private API
    // ---------------------------------------------------

    // TODO: Divide visually from receive methods
    private fun SendAny(messageId : UInt, header : Header, payload : Any) {
        // Get the message ID of the packet to ack later
        val rawPayload = ignoreUnknownJson.encodeToString(payload) + "\n"
        val rawHeader = header.toByteArray()
        val packet = rawHeader + rawPayload.toByteArray(Charsets.UTF_8)
        val pair = Pair(messageId, packet)
        unackedPackets.add(pair)
        Networking.write(packet)
    }

    // TODO: Test, Cleanup
    private suspend fun receiveContactInformation(header : Header, payload : String) {
        if (contactDatabase == null || messageDatabase == null) {
            throw IllegalStateException("Protocol Handler not initialized! Wrong usage!")
        }

        withContext(Dispatchers.IO) {
            Log.i("ApollonProtocolHandler", "Expecting Contact Information")
            val info = ignoreUnknownJson.decodeFromString<ContactInfo>(payload)

            // Include the information how much compression is in packet
            val decomImage = ByteArray(info.ImageBytes.toInt())
            Zstd.decompress(decomImage, info.Image)
            // Store the image to the application specific storage under "contactID.file"
            val storageFile = File(imageFileDir, "${header.UserId}.${UCharacter.toLowerCase(info.ImageFormat)}")
            storageFile.writeBytes(decomImage)
            Log.i("ApollonProtocolHandler", "Stored contact ${header.UserId} picture under ${storageFile.absolutePath}")
        }
    }


    // TODO: Test, Cleanup
    private suspend fun receiveTextMessage(header : Header, payload : String) {
        if (contactDatabase == null || messageDatabase == null) {
            throw IllegalStateException("Protocol Handler not initialized! Wrong usage!")
        }
        withContext(Dispatchers.IO) {
            val message = ignoreUnknownJson.decodeFromString<Message>(payload)
            // Inserting the new message into the database
            var prevId = 0L
            val messages = messageDatabase!!.getMessages(header.UserId.toLong())
            if (messages != null) {
                prevId = messages.size + 1L
            }
            val localMessage = DisplayMessage(prevId, header.UserId.toLong(), false, message.Message, message.Timestamp)
            messageDatabase!!.insertMessage(localMessage)
            val contact = contactDatabase!!.getContact(header.UserId.toLong())
            if (contact == null) {
                Log.i("ApollonProtocolHandler", "Failed to find user for ID ${header.UserId}")
                // Should kill the connection?
                return@withContext
            }
            contact.lastMessage = message.Message
            contactDatabase!!.updateContact(contact)
        }
    }

    // TODO: Test, Cleanup
    private suspend fun receiveContactOption(header: Header, payload: String) {
        if (contactDatabase == null || messageDatabase == null) {
            throw IllegalStateException("Protocol Handler not initialized! Wrong usage!")
        }
        withContext(Dispatchers.IO) {
            if (user == null) {
                // Keep the packet for later so that it can be properly handled
                // TODO: Implement handling packets later
                //                unhandledPackets.add(payload)
                return@withContext
            }
            val request = ignoreUnknownJson.decodeFromString<ContactOption>(payload)
            var contactRequest = false
            var username = "Username"
            if (request.Options.size > 2) {
                Log.i("ApollonProtocolHandler", "Received contact option with more than 2 options! Currently NOT POSSIBLE!")
                return@withContext
            }
            for (opt in request.Options) {
                if (opt.Type.contentEquals("Question") && opt.Value.contentEquals("Add")) {
                    // Found a friend request, "can" expect a username
                    contactRequest = true
                }
                // Can be possible, but MUST not be contained
                if (contactRequest && opt.Type.contentEquals("Username")) {
                    if (request.ContactUserId != userId) {
                        Log.i("ApollonProtocolHandler", "The requested UserID does not match with the local userId!")
                        return@withContext
                    }
                    username = opt.Value
                }
                if (opt.Type.contentEquals("Add")) {
                    // TODO: Other user accepted the request
                    username = opt.Value
                }
                if (opt.Type.contentEquals("Remove")) {
                    // TODO:
                }
                if (opt.Type.contentEquals("RemoveAck")) {
                    // TODO:
                }
            }
            // TODO: Include a way for the user to accept or decline the request
            // For now instant accept
            val answerOptions = listOf(NetworkOption("Add", "${user?.username}"))
            val answer = ContactOption(header.UserId, answerOptions)
            val contactId = header.UserId
            // Flip the IDs and send back
            header.UserId = userId
            SendAny(header.MessageId, header, answer)

            // Saving the new contact in the list of contacts
            val imageFile = File(imageFileDir, "${contactId}.png")
            val newContact = Contact(contactId.toLong(), username, imageFile.absolutePath)
            contactDatabase!!.insertContact(newContact)
        }
    }

    // TODO: Divide visually, test
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
            val login = Header(PacketCategories.CONTACT.cat.toByte(), ContactType.LOGIN.type.toByte(), userId, Random.nextUInt())
            val packet = login.toByteArray()
            Networking.write(packet)
        }
    }

    // ---------------------------------------------------
    // Helper Functionalities
    // ---------------------------------------------------

    private fun Int.to2ByteArray() : ByteArray = byteArrayOf(shr(8).toByte(), toByte())

}