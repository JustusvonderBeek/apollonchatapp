package com.cloudsheeptech.anzuchat.networking.ApollonProtocolHandler

import android.content.Context
import android.icu.lang.UCharacter
import android.util.Log
import com.cloudsheeptech.anzuchat.database.ApollonDatabase
import com.cloudsheeptech.anzuchat.database.contact.Contact
import com.cloudsheeptech.anzuchat.database.contact.ContactDatabaseDao
import com.cloudsheeptech.anzuchat.database.message.DisplayMessage
import com.cloudsheeptech.anzuchat.database.message.MessageDao
import com.cloudsheeptech.anzuchat.database.user.User
import com.cloudsheeptech.anzuchat.database.user.UserDatabaseDao
import com.cloudsheeptech.anzuchat.networking.Networking
import com.cloudsheeptech.anzuchat.networking.Networking.receivePackets
import com.cloudsheeptech.anzuchat.networking.constants.ContactType
import com.cloudsheeptech.anzuchat.networking.constants.DataType
import com.cloudsheeptech.anzuchat.networking.constants.PacketCategories
import com.cloudsheeptech.anzuchat.networking.packets.ContactInfo
import com.cloudsheeptech.anzuchat.networking.packets.ContactOption
import com.cloudsheeptech.anzuchat.networking.packets.Create
import com.cloudsheeptech.anzuchat.networking.packets.FileHave
import com.cloudsheeptech.anzuchat.networking.packets.FileInfo
import com.cloudsheeptech.anzuchat.networking.packets.Header
import com.cloudsheeptech.anzuchat.networking.packets.Message
import com.cloudsheeptech.anzuchat.networking.packets.NetworkOption
import com.cloudsheeptech.anzuchat.networking.packets.Search
import com.github.luben.zstd.Zstd
import io.ktor.util.date.getTimeMillis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDateTime
import java.util.Calendar
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.random.Random

/*
* Instead of using a class we use an object.
* This creates a singleton making all accesses to this class to the same instance
* preventing any duplication or misses
 */
object ApollonProtocolHandler {
    // TODO: Include saving mechanism for outgoing messages

    private var userId : UInt = 0U
    private var randomness : Random = Random(LocalDateTime.now().nano)
    private var timeout = 2000L
    private var messageID : AtomicInteger = AtomicInteger(randomness.nextInt())
    private var unackedPackets : MutableList<StorageMessage> = ArrayList()
    // TODO: What is this for?
    private var unhandledPackets : MutableList<ByteArray> = ArrayList()
    private var incomingPackets : Channel<ByteArray> = Channel(50)
    private var loggedIn : Boolean = false
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
    private var notificationCallback : ((DisplayMessage, Contact) -> Unit)? = null

    // ---------------------------------------------------
    // Public API
    // ---------------------------------------------------

    fun initialize(userId: UInt, context: Context, callback: ((DisplayMessage, Contact) -> Unit)? = null) {
        this.imageFileDir = context.filesDir.absolutePath
        val database = ApollonDatabase.getInstance(context)
        userDatabase = database.userDao()
        protocolScope.launch {
            LoadUser(userDatabase!!)
        }
        contactDatabase = database.contactDao()
        messageDatabase = database.messageDao()
        this.userId = userId
        if (callback != null)
            notificationCallback = callback

        val networkConfig = Networking.Configuration()
        Networking.initialize(networkConfig, incomingPackets)
        Networking.start(context)

        thread {
            runBlocking {
                listenReceiveChannel()
            }
        }

        if (userId == 0u) {
            Log.i("ApollonProtocolHandler", "Initialized Protocol Handler with ID $userId")
            return
        }

        protocolScope.launch {
            login()
        }
        protocolScope.launch {
            HandleUnackedPackets()
        }
        Log.i("ApollonProtocolHandler", "Initialized Protocol Handler with ID $userId")
    }

    fun Close() {
        storeUnacked()
    }

    // Main method for initial packet handling
    fun receiveAny(packet : ByteArray) {
        val header = Header.convertRawToHeader(packet.sliceArray(0 until 10)) ?: return
        val payload = if (packet.size > 10) packet.sliceArray(10 until packet.size).toString(Charsets.UTF_8) else null
        Log.i("ApollonProtocolHandler", "Handling packet with Header: $header")
        // We cannot always read payload, because of packets that don't have a payload
        when(PacketCategories.getFromByte(header.Category)) {
            PacketCategories.CONTACT -> {
                when(ContactType.getFromByte(header.Type)) {
                    ContactType.CREATE -> {
                        // Not expecting any payload but need to create a new user in the database
                        if (userDatabase == null) {
                            throw IllegalStateException("Database is not set! Cannot store user!")
                        }
                        // Find matching send create packet
                        val matching = findMessageId(header.MessageId)
                        if (matching == null) {
                            Log.i("ApollonProtocolHandler", "Cannot find matching message ID. Either malicious server or we failed locally!")
                            return
                        }
                        val sendCreate = Json.decodeFromString<Create>(matching)
                        val newUser = User(header.UserId.toLong(), sendCreate.Username, userImage = "drawable/ic_user")
                        userId = header.UserId

                        protocolScope.launch {
                            insertUserIntoDatabase(newUser)
                            Log.i("ApollonProtocolHandler", "Wrote user into database")
                        }
                        // Remove the message that was stored for the user creation
                        unackedPackets.removeIf {
                            it.MessageID == header.MessageId
                        }
                    }
                    ContactType.CONTACTS -> {
                        // Send this further to the contact view model?
                        // Or maybe let this one handle this alone?
                        // We need to "clean" the pipe, so read anyways
                        Log.i("ApollonProtocolHandler", "Received Contacts...")
//                        val payload = readFullLine(incomingStream, timeout) ?: return
//                        val payload = incomingStream.bufferedReader().readLine()
                        Log.i("ApollonProtocolHandler", "Showing the contacts via the callback")
                        if (contactsCallback != null) {
                            if (payload == null) {
                                Log.w("ApollonProtocolHandler", "No payload for contacts type was received!")
                                return
                            }
                            contactsCallback!!.invoke(payload)
                        }
                        // Clear the search that lead to this contact list
                        unackedPackets.removeIf {
                            it.MessageID == header.MessageId
                        }
                    }
                    ContactType.OPTION -> {
                        // Differentiate further
//                        val payload = incomingStream.bufferedReader().readLine()
                        if (payload == null) {
                            Log.w("ApollonProtocolHandler", "No payload for contact option type was received!")
                            return
                        }
                        protocolScope.launch {
                            receiveContactOption(header, payload.toString())
                        }
                    }
                    ContactType.CONTACT_INFO -> {
//                        val payload = incomingStream.bufferedReader().readLine()
                        if (payload == null) {
                            Log.w("ApollonProtocolHandler", "No payload for contact option type was received!")
                            return
                        }
                        protocolScope.launch {
                            receiveContactInformation(header, payload.toString())
                        }
                    }
                    ContactType.CONTACT_ACK -> {
                        val ackedID = header.MessageId
                        unackedPackets.removeIf {
                                it.MessageID == ackedID
                        }
                    }
                    else -> {
                        Log.i("ApollonProtocolHandler", "Got incorrect type or packet that is not meant for client sides. mID: ${header.MessageId}")
                        // TODO: Decide how to handle incorrect packets
                    }
                }
            }
            PacketCategories.DATA -> {
                when(DataType.getFromByte(header.Type)) {
                    DataType.TEXT ->  {
                        // Receive only payload and then free the stream to continue receiving
//                        val payload = readFullLine(incomingStream, timeout) ?: return
//                        val payload = incomingStream.bufferedReader().readLine()
                        if (payload == null) {
                            Log.w("ApollonProtocolHandler", "No payload for text was received!")
                            return
                        }
                        Log.i("ApollonProtocolHandler", "Received text")
                        protocolScope.launch {
                            receiveTextMessage(header, payload.toString())
                        }
                    }
                    DataType.TEXT_ACK -> {
                        // Extract the messageID that was acked
                        // empty the stream for now
//                        incomingStream.bufferedReader().readLine()
                        val ackedID = header.MessageId
                        Log.i("ApollonProtocolHandler", "Got TextAck for $ackedID")
                        unackedPackets.removeIf {
                                it.MessageID == ackedID
                        }
                    }
                    DataType.FILE_INFO -> {
//                        val payload = incomingStream.bufferedReader().readLine()
                        if (payload == null) {
                            Log.w("ApollonProtocolHandler", "No payload for file info type was received!")
                            return
                        }
                        val fileInfo = Json.decodeFromString<FileInfo>(payload.toString())
                        Log.i("ApollonProtocolHandler", "Received File Information")
                        // Check for existing file information (skip for now)
                        val fileHave = FileHave(0)
                        val encoded = Json.encodeToString(fileHave)
                        val haveHeader = Header(PacketCategories.DATA.cat.toByte(), DataType.FILE_HAVE.type.toByte(), userId, header.MessageId)
                        val rawPacket = encoded.toByteArray()
                        Networking.write(haveHeader.toByteArray() + rawPacket)
                    }
                    DataType.FILE_HAVE -> {
//                        val payload = incomingStream.bufferedReader().readLine()
                        if (payload == null) {
                            Log.w("ApollonProtocolHandler", "No payload for file have type was received!")
                            return
                        }
                        val fileHave = Json.decodeFromString<FileHave>(payload.toString())

                        val sfileInfo = findMessageId(header.MessageId)
                        if (sfileInfo == null) {
                            Log.i("ApollonProtocolHandler",  "Cannot find matching file information for file transfer!")
                            return
                        }
                        val fileInfo = Json.decodeFromString<FileInfo>(sfileInfo)
                        // Transfer of file
                        val file = File(fileInfo.FileName)
                        val buffer = file.readBytes()
                        val fileHeader = Header(PacketCategories.DATA.cat.toByte(), DataType.FILE.type.toByte(), userId, header.MessageId)
                        Networking.write(fileHeader.toByteArray() + buffer.sliceArray(fileHave.FileOffset until buffer.size))
                    }
                    DataType.FILE -> {
                        val sfileInfo = findMessageId(header.MessageId)
                        if (sfileInfo == null) {
                            Log.i("ApollonProtocolHandler",  "Cannot find matching file information for file transfer!")
                            return
                        }
                        val fileInfo = Json.decodeFromString<FileInfo>(sfileInfo)
                        // Receive the full file and store it
                        val buffer = ByteArray(fileInfo.CompressedLength.toInt())
//                        var read = incomingStream.read(buffer)
//                        if (read < buffer.size) {
////                            read = incomingStream.read(buffer, read, buffer.size-read)
//                        }
                        // Decompress the image
                        val decompress = ByteArray(fileInfo.FileLength.toInt())
                        if (fileInfo.Compression == "ZSTD") {
                            val extracted = Zstd.decompress(decompress, buffer)
                            if (extracted != fileInfo.FileLength.toLong()) {
                                Log.i("ApollonProtocolHandler", "Decompression extracted different number of bytes than given by file information!")
                            }
                        } else {
                            buffer.copyInto(decompress)
                        }
                        val storageFile = File(imageFileDir, fileInfo.FileName)
                        storageFile.writeBytes(decompress)
                        // Create ack and send back
                        val fileAck = Header(PacketCategories.DATA.cat.toByte(), DataType.FILE_ACK.type.toByte(), userId, header.MessageId)
                        val rawAck = fileAck.toByteArray()
                        Networking.write(rawAck)
                    }
                    DataType.FILE_ACK -> {
                        unackedPackets.removeIf {
                            it.MessageID == header.MessageId
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
                Log.d("ApollonProtocolHandler", "Packet: ${packet.toHexString()}")
                // Skip this packet
            }
        }
    }

    private suspend fun insertMessageIntoDatabase(header: Header, message: Message, received : Boolean = false) {
        withContext(Dispatchers.IO) {
            var prevId = 0L
            // Default: receiving message
            var remoteId = header.UserId
            if (remoteId == userId) {
                // In case we SENT the message
                remoteId = message.ContactUserId
            }
            val messages = messageDatabase!!.getMessages(remoteId.toLong())
            if (messages != null) {
                prevId = messages.size + 1L
            }
            val localMessage = DisplayMessage(prevId, remoteId.toLong(), !received && header.UserId == userId, message.Message, message.Timestamp)
            messageDatabase!!.insertMessage(localMessage)

            val contact = contactDatabase!!.getContact(remoteId.toLong())
            if (contact == null) {
                Log.i("ApollonProtocolHandler", "Failed to find user for ID ${header.UserId}")
                // Should kill the connection?
                return@withContext
            }
            contact.lastMessage = message.Message
            contactDatabase!!.updateContact(contact)

            // Send a notification to the user in case of remote message
            if (header.UserId != userId)
                notificationCallback?.invoke(localMessage, contact)
        }
    }

    private suspend fun insertUserIntoDatabase(user : User) {
        withContext(Dispatchers.IO) {
            userDatabase?.insertUser(user)
        }
    }

    // TODO: Implement missing parts of the protocol
    fun sendText(text : String, to : UInt) {
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
            sendAny(header, message)

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

    fun sendCreateAccount(newUser : User) {
        try {
            if (newUser.username.isEmpty()) {
                Log.i("ApollonProtocolHandler", "Cannot create user with empty name")
                return
            }
            val create = Create(newUser.username)
            val mID = messageID.getAndAdd(1).toUInt()
            val header = Header(PacketCategories.CONTACT.cat.toByte(), ContactType.CREATE.type.toByte(), 0u, mID)
            sendAny(header, create)
        } catch (ex : Exception) {
            Log.i("ApollonProtocolHandler", "Failed to send create account to server!\n$ex")
        }
    }

    fun sendSearch(searchString: String) {
        try {
            val search = Search(searchString)
            val mID = messageID.getAndAdd(1).toUInt()
            val header = Header(PacketCategories.CONTACT.cat.toByte(), ContactType.SEARCH.type.toByte(), userId, mID)
            sendAny(header, search)
        } catch (ex : Exception) {
            Log.i("ApollonProtocolHandler", "Failed to send search to server!\n$ex")
        }
    }

    fun sendContactRequest(contactId : UInt) {
        try {
            val addOption = listOf(NetworkOption("Question", "Add"))
            val option = ContactOption(contactId, addOption)
            val mID = messageID.getAndAdd(1).toUInt()
            val header = Header(PacketCategories.CONTACT.cat.toByte(), ContactType.OPTION.type.toByte(), userId, mID)
            sendAny(header, option)
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

    @Serializable
    class StorageMessage(
        val MessageID : UInt,
        var TimeSent : Long,
        val RawPacket : ByteArray,
        val JsonPacket : String,
        var Resend : Int
    ) {
        fun increaseResend() {
            this.Resend = this.Resend + 1
            this.TimeSent = Calendar.getInstance().timeInMillis
        }
    }

    private inline fun <reified T> sendAny(header : Header, content : T) {
        // Get the message ID of the packet to ack later
        val payload = Json.encodeToString(content)
        val rawPayload = payload + "\n"
        val rawHeader = header.toByteArray()
        val packet = rawHeader + rawPayload.toByteArray(Charsets.UTF_8)
        // Struct contains the information for resend and process data
        val store = StorageMessage(header.MessageId, Calendar.getInstance().timeInMillis, packet, payload, 0)
        unackedPackets.add(store)
        Networking.write(packet)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun CoroutineScope.convertBytesToPacket(source : ReceiveChannel<ByteArray>) : ReceiveChannel<ByteArray> = produce {
        val nextPacket = mutableListOf<Byte>()
        while (true) {
            for (byte in source.receive()) {
                if (byte != 0x0a.toByte())
                    nextPacket.add(byte)
                else {
                    val packet = nextPacket.toByteArray()
                    send(packet)
                    nextPacket.clear()
                }
            }
        }
    }

    private suspend fun listenReceiveChannel() {
        Log.i("ApollonProtocolHandler", "Starting protocol listening")
        withContext(Dispatchers.IO) {
            try {
                val produce = receivePackets()
                val packets = convertBytesToPacket(produce)
                runBlocking {
                    while (true) {
                        val packet = packets.receive()

//                        Log.i("ApollonProtocolHandler", "Received packet!")
                        receiveAny(packet)
                    }
                }
            } catch (ex : Exception) {
                Log.i("ApollonProtocolHandler", "Failed to receive packets! $ex")
            }
        }
    }

    // ---------------------------------------------------
    // Helping method
    // ---------------------------------------------------

    private fun findMessageId(id : UInt) : String? {
        val packet = unackedPackets.find {
            it.MessageID == id
        } ?: return null
        return packet.JsonPacket
    }
    
    private fun storeUnacked() {
        val outfile = File(imageFileDir, "unacked.json")
        val converted = Json.encodeToString(unackedPackets)
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
            insertMessageIntoDatabase(header, message, true)
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
            sendAny(header, answer)

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
                        if (packet.TimeSent + timeout <= timeNow && packet.Resend < 3) {
                            // Sending again
//                            SendAny(packet.third)
                            // Need to check for
                            Log.i("ApollonProtocolHandler", "Sending packet ${packet.MessageID} again...")
                            Networking.write(packet.RawPacket)
                            packet.increaseResend()
                        } else {
                            // Tried sending packet already 3 times. Remove?
                            // Depending on the packet type or try when internet becomes available again
                            // Mechanism to determine connection status
                        }
                    }
                }
                unackedPackets.removeIf {
                    it.Resend > 3
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

    private suspend fun login() {
        if (loggedIn) {
            Log.i("ApollonProtocolHandler", "Already logged in")
            return
        }
        withContext(Dispatchers.IO) {
            val mId = messageID.getAndAdd(1).toUInt()
            val login = Header(PacketCategories.CONTACT.cat.toByte(), ContactType.LOGIN.type.toByte(), userId, mId)
            val packet = login.toByteArray() + "\n".toByteArray()
            Networking.write(packet)
            loggedIn = true
        }
    }

    // ---------------------------------------------------
    // Helper Functionalities
    // ---------------------------------------------------

    private fun Int.to2ByteArray() : ByteArray = byteArrayOf(shr(8).toByte(), toByte())

    private fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }


}