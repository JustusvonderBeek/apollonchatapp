package com.example.apollonchat.networking

import android.util.Log
import com.example.apollonchat.addcontact.AddContactViewModel
import com.example.apollonchat.database.contact.ContactDatabaseDao
import com.example.apollonchat.database.message.DisplayMessage
import com.example.apollonchat.database.message.MessageDao
import com.example.apollonchat.database.user.UserDatabaseDao
import com.example.apollonchat.networking.constants.ContactType
import com.example.apollonchat.networking.constants.DataType
import com.example.apollonchat.networking.constants.PacketCategories
import com.example.apollonchat.networking.packets.*
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.InetAddress
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import kotlin.concurrent.thread
import kotlin.random.Random

object Networking {

    /* This class contains the logic to send data to the server
    * and retrieve answers back from the server.
    * It supports connection re-establishment and DNS resolution
     */

    /* Relies on the ktor API to work */

    /*
    ----------------------------------------------------------------
    Definitions
    ----------------------------------------------------------------
     */

    lateinit var remoteAddress : InetAddress
    lateinit var inputQueue : BlockingQueue<ByteArray>
    lateinit var outputQueue : BlockingQueue<ByteArray>
    lateinit var outputChannel : Channel<ByteArray>

    var socket : Socket? = null
    var started : Boolean = false
    var connected : Boolean = false
    var database : ContactDatabaseDao? = null
    var userDatabase : UserDatabaseDao? = null
    var messageDatabase : MessageDao? = null

    // Testing if local vars work?
    var contactViewModel: AddContactViewModel? = null
    private var json = Json { ignoreUnknownKeys = true }

    private var networkingJob = Job()
    private val netScope = CoroutineScope(Dispatchers.Main + networkingJob)

    fun write(data : Message) {
        try {
            netScope.launch {
                val stringData = Json.encodeToString(data)
                write(stringData)
            }
        } catch (ex : IOException) {
            ex.printStackTrace()
        }
    }

    fun write(data : Search) {
        try {
            netScope.launch {
                val stringData = Json.encodeToString(data)
                write(stringData)
            }
        } catch (ex : Exception) {
            // TODO: Make better handling
            ex.printStackTrace()
        }
    }

    fun write(data : Create) {
        try {
            netScope.launch {
                val stringData = Json.encodeToString(data)
                write(stringData)
            }
        } catch (ex : java.lang.Exception) {
            ex.printStackTrace()
        }
    }

    private suspend fun write(data : String) {
        try {
            // Starting a new coroutine allowing to suspend execution
            val rawPacketData = data.toByteArray(charset = Charsets.UTF_8)
            // Packet length includes the length of the size field (2 bytes)
            val packet = (rawPacketData.size + 2).to2ByteArray() + rawPacketData
//                outputQueue.put(packet)
            outputChannel.send(packet)
        } catch (ex : IOException) {
            ex.printStackTrace()
        }
    }

    fun registerContactViewModel(viewModel: AddContactViewModel) {
        this.contactViewModel = viewModel
    }

    fun start(remoteAddress: InetAddress, database : ContactDatabaseDao, userDatabase : UserDatabaseDao?, messageDatabase : MessageDao?) {
        if (this.started) {
            Log.i("Networking", "Already started the network...")
//            return
        } else {
            this.database = database
            this.userDatabase = userDatabase
            this.messageDatabase = messageDatabase
            this.remoteAddress = remoteAddress
            this.outputQueue = ArrayBlockingQueue(20)
            this.inputQueue = ArrayBlockingQueue(20)
            this.outputChannel = Channel(20)
            this.started = true
        }

//        try {
//            socket = Socket()
//            socket.connect(InetSocketAddress(remoteAddress, 15467), 1000)
//            inputStream = socket.getInputStream()
//            outputStream = socket.getOutputStream()
//        } catch (ex : IOException) {
//            // TODO: Add more sophisticated error handling and UI Interaction
//            ex.printStackTrace()
//        }
//
//        val executor = Executors.newSingleThreadExecutor()
//        var handler = Handler(Looper.getMainLooper())
//
        if (!this.connected) {
            // Starting a new coroutine and connecting inside to the server
            var connectionStatus = false
            netScope.launch {
                withContext(Dispatchers.IO) {
                    val selManager = SelectorManager(Dispatchers.IO)
                    try {
                        // This address should emulate the localhost address
//                        socket = aSocket(selManager).tcp().connect("homecloud.homeplex.org", port = 50000)
                        socket = aSocket(selManager).tcp().connect("10.0.2.2", port = 50000)
//                        socket = aSocket(selManager).tcp().connect("192.168.178.53", port = 50000)
                        connectionStatus = true
                    } catch (ex : IOException) {
                        Log.i("Networking", "Connection failed: $ex")
                        return@withContext
                    }

                    // The TLS variant
//        socket = aSocket(selManager).tcp().connect(remoteAddress).tls(coroutineContext = coroutineContext) {
//            trustManager = object : X509TrustManager {
//                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
//                override fun checkClientTrusted(p0: Array<out X509Certificate>?, p1: String?) {}
//                override fun checkServerTrusted(p0: Array<out X509Certificate>?, p1: String?) {}
//            }
//        }
//                Log.i("Networking", "Listing on ...")

                }
                thread {
                    netScope.launch {
                        startSending()
                    }
                }
                thread {
                    netScope.launch {
                        startListening()
                    }
                }
            }

            this.connected = connectionStatus
        }
    }

    private fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }
    private fun Int.to2ByteArray() : ByteArray = byteArrayOf(shr(8).toByte(), toByte())

    // Expecting Big Endian
    private fun ByteArray.toUInt16(): UInt {
        val upper = this[0].toUByte()
        val lower = this[1].toUByte()
        return (upper.toUInt() shl 8) + lower.toUInt()
    }

    private suspend fun startSending() {
        Log.i("Networking", "Starting to send...")
        withContext(Dispatchers.IO) {
            val sendChannel = socket!!.openWriteChannel(autoFlush = true)
            while (true) {
                // Fetching the next packet from the queue of packets that should be sent
                val nextPacket = outputChannel.receive()
                Log.i("Networking Start Sending", "Sending: ${nextPacket.toHexString()}")

                sendChannel.toOutputStream().write(nextPacket)
            }
        }
    }

    private suspend fun startListening() {
        Log.i("Networking", "Starting to receive...")
        withContext(Dispatchers.IO) {
            val recChannel = socket!!.openReadChannel().toInputStream()
            val sizeBuffer = ByteArray(2)
            while(true) {
                var read = recChannel.read(sizeBuffer, 0, 2)
                while(read < 2) {
                    recChannel.read(sizeBuffer, read, sizeBuffer.size - read)
                }
                // Big endian
                val size = sizeBuffer.toUInt16() - 2U
                Log.i("Networking", "Size expected: $size - ${sizeBuffer.toHexString()} - ${sizeBuffer[1].toUByte()}")
                val packetBuffer = ByteArray(size.toInt())
                read = recChannel.read(packetBuffer)
                // Save the read data into a consumer queue for another thread to handle
                // Convert the packet to String and give to JSON to handle
                val sPacket = packetBuffer.toString(Charsets.UTF_8)
                val header = json.decodeFromString<Header>(sPacket)
                // TODO: Decode
                Log.i("Networking", "Received cat: ${header.Category}, type: ${header.Type}")
                when {
                    header.Category.toInt() == PacketCategories.CONTACT.cat && header.Type.toInt() == ContactType.CONTACTS.type  -> {
                        Log.i("Networking", "Received contact list: $sPacket")
                        // Failed: We have to allow NULL in the data class in order to allow the list to be null
                        val contactList = json.decodeFromString<ContactList>(sPacket)
                        Log.i("Networking", "Contact list. ${contactList.Contacts}")
                        contactList.Contacts?.let {
                            contactViewModel?.showContacts(it)
                        }
                    }
                    header.Category.toInt() == PacketCategories.DATA.cat && header.Type.toInt() == DataType.TEXT.type -> {
                        Log.i("Networking", "Received text message")
                        val message = json.decodeFromString<Message>(sPacket)
                        Log.i("Networking", "Message: ${message.Message}")
                        // TODO: Add the message to the messages of the client
                        // TODO: Check if user exists
                        messageDatabase?.let {db ->
                            // TODO: Fix the message ID to be the last + 1
                            var oldMessageId = 0
                            if (db.getMessages(message.UserId.toLong()) != null) {
                                oldMessageId = db.getMessages(message.UserId.toLong())!!.size + 1
                            }
                            val dm = DisplayMessage(Random.nextLong(), messageId = oldMessageId.toLong(), message.UserId.toLong(), false, message.Message, message.Timestamp)
                            db.insertMessage(dm)
                        }
                    }
                    else -> {
                        Log.i("Networking", "Got unexpected category back")
                        continue
                    }
                }
            }
        }
    }



    /*
    ----------------------------------------------------------------
    Singleton
    ----------------------------------------------------------------
     */

}