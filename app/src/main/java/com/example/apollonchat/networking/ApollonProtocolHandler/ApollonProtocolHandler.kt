package com.example.apollonchat.networking.ApollonProtocolHandler

import android.content.Context
import android.icu.lang.UCharacter
import android.util.Log
import android.util.TimeUtils
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
import com.example.apollonchat.networking.packets.ContactOption
import com.example.apollonchat.networking.packets.Create
import com.example.apollonchat.networking.packets.Header
import com.example.apollonchat.networking.packets.Message
import com.example.apollonchat.networking.packets.NetworkOption
import com.example.apollonchat.networking.packets.Search
import com.github.luben.zstd.Zstd
import io.ktor.util.date.getTimeMillis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.time.LocalDateTime
import java.util.Calendar
import java.util.Date
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

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
    private var timeout = 2000L
    private var messageID : AtomicInteger = AtomicInteger(randomness.nextInt())
    private var unackedPackets : MutableList<Triple<Pair<UInt, Int>, Long, ByteArray>> = ArrayList()
    // TODO: What is this for?
    private var unhandledPackets : MutableList<ByteArray> = ArrayList()
    private var ignoreUnknownJson = Json { ignoreUnknownKeys = true }
    private var imageFileDir : String? = null
    private var user : User? = null
    private var userDatabase : UserDatabaseDao? = null
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
        userDatabase = database.userDao()
        protocolScope.launch {
            LoadUser(userDatabase!!)
        }
        contactDatabase = database.contactDao()
        messageDatabase = database.messageDao()
        this.userId = userId

//        Networking.registerCallback(this::ReceiveAny)

        if (userId == 0u) {
            Log.i("ApollonProtocolHandler", "Initialized Protocol Handler with ID $userId")
            return
        }

        protocolScope.launch {
            Login()
        }

        protocolScope.launch {
            HandleUnackedPackets()
        }

        Log.i("ApollonProtocolHandler", "Initialized Protocol Handler with ID $userId")
    }

    fun Close() {
        storeUnacked()
    }

    private fun readFullLine(incoming : InputStream, timeout : Long) : String? {
        try {
            val payload = incoming.bufferedReader().readLine()
//            val payload = runBlocking {
//                withTimeoutOrNull(Duration.parse("1s")) {
//                    val finalString = StringBuilder()
//                    val bufRead = incoming.bufferedReader()
//                    // TODO: Read all until delimiter and timeout
////                    do {
////                        val json = bufRead.readText()
////                        finalString.append(json)
////                    } while (!json.endsWith("\n") && !json.endsWith("\r\n"))
//                    Log.i("ApollonProtocolHandler", "Starting to read string data")
//                    val json = bufRead.readText()
//                    finalString.append(json)
//                    Log.i("ApollonProtocolHandler", "Finished reading protocol data")
//                    bufRead.close()
//                    finalString.toString()
//                }
//            }
            return payload
        } catch (ex : Exception) {
            Log.i("ApollonProtocolHandler", "Failed to receive! $ex")
            return null
        }
    }

    // Main method for initial packet handling
    fun receiveAny(headerBuffer : ByteArray, incomingStream: InputStream) {
        val header = Header.convertRawToHeader(headerBuffer) ?: return
        Log.i("ApollonProtocolHandler", "Header: $header")
        // We cannot always read payload, because of packets that don't have a payload
        when(header.Category.toLong()) {
            PacketCategories.CONTACT.cat.toLong() -> {
                when(header.Type.toLong()) {
                    ContactType.CREATE.type.toLong() -> {
                        // Not expecting any payload but need to create a new user in the database
                        if (userDatabase == null) {
                            Log.i("ApollonProtocolHandler", "Database is null. Cannot insert new user!")
                            return
                        }
                        val newUser = User(header.UserId.toLong(), "TODO", userImage = "drawable/usericon.png")
                        // TODO: BUGFIX might running on wrong thread and leading to starvation
                        userDatabase!!.insertUser(newUser)
                        Log.i("ApollonProtocolHandler", "Wrote user into database")
                    }
                    ContactType.CONTACTS.type.toLong() -> {
                        // Send this further to the contact view model?
                        // Or maybe let this one handle this alone?
                        // We need to "clean" the pipe, so read anyways
                        Log.i("ApollonProtocolHandler", "Received Contacts...")
//                        val payload = readFullLine(incomingStream, timeout) ?: return
                        val payload = incomingStream.bufferedReader().readLine()
                        Log.i("ApollonProtocolHandler", "Showing the contacts via the callback")
                        if (contactsCallback != null) {
                            contactsCallback!!.invoke(payload)
                        }
                    }
                    ContactType.OPTION.type.toLong() -> {
                        // Differentiate further
                        val payload = readFullLine(incomingStream, timeout) ?: return
                        protocolScope.launch {
                            receiveContactOption(header, payload)
                        }
                    }
                    ContactType.CONTACT_INFO.type.toLong() -> {
                        val payload = readFullLine(incomingStream, timeout) ?: return
                        protocolScope.launch {
                            receiveContactInformation(header, payload)
                        }
                    }
                    ContactType.CONTACT_ACK.type.toLong() -> {
                        val ackedID = header.MessageId
                        unackedPackets.removeIf {
                                pair -> pair.first.first == ackedID
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
//                        val payload = readFullLine(incomingStream, timeout) ?: return
                        val payload = incomingStream.bufferedReader().readLine()
                        Log.i("ApollonProtocolHandler", "Received text")
                        protocolScope.launch {
                            receiveTextMessage(header, payload)
                        }
                    }
                    DataType.TEXT_ACK.type.toLong() -> {
                        // Extract the messageID that was acked
                        // empty the stream for now
                        incomingStream.bufferedReader().readLine()
                        val ackedID = header.MessageId
                        unackedPackets.removeIf {
                                pair -> pair.first.first == ackedID
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

    private suspend fun insertMessageIntoDatabase(header: Header, message: Message) {
        withContext(Dispatchers.IO) {
            var prevId = 0L
            val messages = messageDatabase!!.getMessages(message.ContactUserId.toLong())
            if (messages != null) {
                prevId = messages.size + 1L
            }
            val localMessage = DisplayMessage(prevId, message.ContactUserId.toLong(), true, message.Message, message.Timestamp)
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
            SendAny(header, message)

            // Adding the text into the message database
            protocolScope.launch {
                insertMessageIntoDatabase(header, message)
            }
        } catch (ex : NullPointerException) {
            Log.i("ApollonProtocolHandler", "Internal programming error!")
            ex.printStackTrace()
        } catch (ex : Exception) {
            Log.i("ApollonProtocolHandler", "Failed to send text message!\n$ex")
        }
    }

    fun SendContactInformationUpdate() {

    }

    fun SendCreateAccount(newUser : User) {
        try {
            if (newUser.username.isEmpty()) {
                Log.i("ApollonProtocolHandler", "Cannot create user with empty name")
                return
            }
            val create = Create(newUser.username)
            val mID = messageID.getAndAdd(1).toUInt()
            val header = Header(PacketCategories.CONTACT.cat.toByte(), ContactType.CREATE.type.toByte(), 0u, mID)
            SendAny(header, create)
        } catch (ex : Exception) {
            Log.i("ApollonProtocolHandler", "Failed to send create account to server!\n$ex")
        }
    }

    fun SendSearch(searchString: String) {
        try {
            val search = Search(searchString)
            val mID = messageID.getAndAdd(1).toUInt()
            val header = Header(PacketCategories.CONTACT.cat.toByte(), ContactType.SEARCH.type.toByte(), userId, mID)
            SendAny(header, search)
        } catch (ex : Exception) {
            Log.i("ApollonProtocolHandler", "Failed to send search to server!\n$ex")
        }
    }

    fun SendContactRequest(contactId : UInt) {
        try {
            val addOption = listOf(NetworkOption("Question", "Add"))
            val option = ContactOption(contactId, addOption)
            val mID = messageID.getAndAdd(1).toUInt()
            val header = Header(PacketCategories.CONTACT.cat.toByte(), ContactType.OPTION.type.toByte(), userId, mID)
            SendAny(header, option)
        } catch (ex : Exception) {
            Log.i("ApollonProtocolHandler", "Failed to send search to server!\n$ex")
        }
    }

    fun registerContactsCallback(callback : (String) -> Unit) {
        this.contactsCallback = callback
    }


    // ---------------------------------------------------
    // Private API
    // ---------------------------------------------------

    private fun SendAny(header : Header, payload : Search) {
        val rawPayload = ignoreUnknownJson.encodeToString(payload)
        SendAny(header, rawPayload)
    }

    private fun SendAny(header : Header, payload : Create) {
        val rawPayload = ignoreUnknownJson.encodeToString(payload)
        SendAny(header, rawPayload)
    }

    private fun SendAny(header: Header, payload: Message) {
        val rawPayload = ignoreUnknownJson.encodeToString(payload)
        SendAny(header, rawPayload)
    }

    private fun SendAny(header: Header, payload: ContactOption) {
        val rawPayload = ignoreUnknownJson.encodeToString(payload)
        SendAny(header, rawPayload)
    }

    // TODO: Divide visually from receive methods
    private fun SendAny(header : Header, payload : String) {
        // Get the message ID of the packet to ack later
        val rawPayload = payload + "\n"
        val rawHeader = header.toByteArray()
        val packet = rawHeader + rawPayload.toByteArray(Charsets.UTF_8)
        val triple = Triple(Pair(header.MessageId, 0), Calendar.getInstance().timeInMillis, packet)
        unackedPackets.add(triple)
        Networking.write(packet)
    }

    // ---------------------------------------------------
    // Helping method
    // ---------------------------------------------------

    @Serializable
    data class RawPacket (
        var id : UInt,
        var content : ByteArray
    )

    private fun storeUnacked() {
        val outfile = File(imageFileDir, "unacked.json")
        val rawPackets = mutableListOf<RawPacket>()
        for (pack in unackedPackets) {
            val raw = RawPacket(pack.first.first, pack.third)
            rawPackets.add(raw)
        }
        val converted = Json.encodeToString(rawPackets)
        val outlist = converted.toByteArray()
        outfile.writeBytes(outlist)
    }

    // ---------------------------------------------------
    // Helping method end
    // ---------------------------------------------------

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
        Log.i("ApollonProtocolHandler", "Payload: $payload")
        withContext(Dispatchers.IO) {
            // Use the default JSON here because we only want to accept the correct Message here!
            val res = kotlin.runCatching {
                Json.decodeFromString<Message>(payload)
            }
            if (res.isFailure) {
                Log.i("ApollonProtocolHandler", "Failed to decode text payload")
                return@withContext
            }
            val message = res.getOrNull()!!
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
            SendAny(header, answer)

            // Saving the new contact in the list of contacts
            val imageFile = File(imageFileDir, "${contactId}.png")
            val newContact = Contact(contactId.toLong(), username, imageFile.absolutePath)
            contactDatabase!!.insertContact(newContact)
        }
    }

    private suspend fun HandleUnackedPackets() {
        withContext(Dispatchers.IO) {
            while (true) {
                if (unackedPackets.size > 0) {
                    val timeNow = Calendar.getInstance().timeInMillis
                    for (i in unackedPackets.indices) {
                        val packet = unackedPackets[i]
                        if (packet.second + timeout <= timeNow && packet.first.second < 3) {
                            // Sending again
//                            SendAny(packet.third)
                            // Need to check for
                            Log.i("ApollonProtocolHandler", "Sending packet ${packet.first} again...")
                            Networking.write(packet.third)
                            val first = Pair(packet.first.first, packet.first.second + 1)
                            unackedPackets[i] = Triple(first, Calendar.getInstance().timeInMillis, packet.third)
                        } else {
                            // Tried sending packet already 3 times. Remove?
                            // Depending on the packet type or try when internet becomes available again
                            // Mechanism to determine connection status
                        }
                    }
                }
                Thread.sleep(timeout + 10)
            }
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
            val mId = messageID.getAndAdd(1).toUInt()
            val login = Header(PacketCategories.CONTACT.cat.toByte(), ContactType.LOGIN.type.toByte(), userId, mId)
            val packet = login.toByteArray()
            Networking.write(packet)
        }
    }

    // ---------------------------------------------------
    // Helper Functionalities
    // ---------------------------------------------------

    private fun Int.to2ByteArray() : ByteArray = byteArrayOf(shr(8).toByte(), toByte())

}