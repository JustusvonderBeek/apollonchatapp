package com.example.apollonchat.networking

import android.content.Context
import android.util.Log
import com.example.apollonchat.R
import com.example.apollonchat.addcontact.AddContactViewModel
import com.example.apollonchat.database.contact.ContactDatabaseDao
import com.example.apollonchat.database.message.MessageDao
import com.example.apollonchat.database.user.User
import com.example.apollonchat.database.user.UserDatabaseDao
import com.example.apollonchat.networking.ApollonProtocolHandler.ApollonProtocolHandler
import com.example.apollonchat.networking.certificate.ApollonNetworkConfigCreator
import com.example.apollonchat.networking.packets.*
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.network.tls.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException
import java.io.InputStream
import java.net.InetAddress
import java.util.Hashtable
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import kotlin.concurrent.thread
import kotlin.random.Random
import kotlin.random.nextUInt

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

    var inputQueue : BlockingQueue<ByteArray> = ArrayBlockingQueue(20)
    var outputQueue : BlockingQueue<ByteArray> = ArrayBlockingQueue(20)
    var outputChannel : Channel<ByteArray> = Channel(20)

    var socket : Socket? = null
    var incomingChannel : InputStream? = null
    var connectSecure : Boolean = false
    var remoteAddress : InetAddress? = null
    var startLock : Mutex = Mutex(false)
    var started : Boolean = false
    var sending : Boolean = false
    var receiving : Boolean = false
    var init : Boolean = false
    var connected : Boolean = false

    // Databases
    var contactDatabase : ContactDatabaseDao? = null
    var userDatabase : UserDatabaseDao? = null
    var messageDatabase : MessageDao? = null

    // Testing if local vars work?
    var contactViewModel: AddContactViewModel? = null
    private var json = Json { ignoreUnknownKeys = true }
    private var lastMessageId = Random.nextUInt()
    private var registeredCallbacks = Hashtable<Pair<Long, Long>, MutableList<(String, InputStream) -> Unit>>()

    private var userCreatedCallback : ((User) -> Unit)? = null

    private var networkingJob = Job()
    private val netScope = CoroutineScope(Dispatchers.Main + networkingJob)

    /*
    ----------------------------------------------------------------
    Write methods for the Apollon protocol
    ----------------------------------------------------------------
     */

    fun write(data : Login) {
        try {
            netScope.launch {
                data.MessageId = lastMessageId++
                val stringData = json.encodeToString(data)
                write(stringData)
            }
        } catch (ex : IOException) {
            ex.printStackTrace()
        }
    }

    fun write(data : ContactOption) {
        try {
            netScope.launch {
                data.MessageId = lastMessageId++
                val stringData = json.encodeToString(data)
                write(stringData)
            }
        } catch (ex : IOException) {
            ex.printStackTrace()
        }
    }

    fun write(data : Message) {
        try {
            netScope.launch {
                data.MessageId = lastMessageId++
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
                data.MessageId = lastMessageId++
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
                data.MessageId = lastMessageId++
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

    suspend fun write(data : ByteArray) {
        try {
            outputChannel.send(data)
        } catch(ex : IOException) {
            ex.printStackTrace()
        }
    }

    fun registerContactViewModel(viewModel: AddContactViewModel) {
        this.contactViewModel = viewModel
    }

    fun registerContactCreatedCallback(callback : (User) -> Unit) {
        this.userCreatedCallback = callback
    }

    fun registerCallback(category : Long, type : Long, callback : (String, InputStream) -> Unit) {
        if (registeredCallbacks[Pair(category, type)] == null) {
            registeredCallbacks[Pair(category, type)] = mutableListOf(callback)
        } else {
            registeredCallbacks[Pair(category, type)]!!.add(callback)
        }
    }

    // Default arguments (TLS) need to be placed after non default ones in order to be used
    fun initialize(remote: InetAddress, contactDao : ContactDatabaseDao, userDao : UserDatabaseDao, messageDao : MessageDao, tls : Boolean = false) {
        if (!this.init) {
            if(contactDatabase == null) contactDatabase = contactDao
            if (userDatabase == null) userDatabase = userDao
            if (messageDatabase == null) messageDatabase = messageDao
            if (remoteAddress == null) remoteAddress = remote
            connectSecure = tls
            init = true
        }
    }

    suspend fun start(context : Context) {
        if (!init) {
            throw IllegalStateException("'start()' called before 'initialize()'!")
        }

        // Connecting to the endpoint
        if (!connected) {
            if (connectSecure) {
                connectSecure(context)
//                    Log.i("Networking", "Exited connectSecure")
            } else {
                connectDefault()
            }
//            Log.i("Networking", "After connection launch")
        } else {
            Log.i("Networking", "Endpoint already connected!")
        }

        // Then starting to send and receiver
        if (!sending) {
            thread {
                netScope.launch {
                 startSending()
                }
            }
        } else {
            Log.i("Networking", "Already sending!")
        }
        if (!receiving) {
            thread {
                netScope.launch {
                    startListening()
                }
            }
        } else {
            Log.i("Networking", "Already receiving!")
        }
    }


    private suspend fun connectDefault() {
         val con = withContext(Dispatchers.IO) {
            val selManager = SelectorManager(Dispatchers.IO)
            try {
//                socket = aSocket(selManager).tcp().connect("homecloud.homeplex.org", port = 50000)
                // This address should emulate the localhost address
                socket = aSocket(selManager).tcp().connect("10.0.2.2", port = 50000)
//                        socket = aSocket(selManager).tcp().connect("192.168.178.53", port = 50000)
                Log.i("Networking", "Connected to remote per TCP")
                return@withContext true
            } catch (ex : IOException) {
                Log.i("Networking", "Connection failed: $ex")
                return@withContext false
            }
        }
        connected = con
    }

    private suspend fun connectSecure(context: Context) {
        val con = withContext(Dispatchers.IO) {
            // The TLS variant
            try {
                val selManager = SelectorManager(Dispatchers.IO)
                val tlsConfig = ApollonNetworkConfigCreator.createTlsConfig(context.resources.openRawResource(R.raw.apollon))
                // If the context is set as 'coroutineContext' then the method DOES NOT return back to the caller side!
                socket = aSocket(selManager).tcp().connect("homecloud.homeplex.org", port = 50001).tls(Dispatchers.IO, tlsConfig)
//                socket = aSocket(selManager).tcp().connect("10.0.2.2", port = 50001).tls(Dispatchers.IO, tlsConfig)

                Log.i("Networking", "Connected to remote per TLS")
                return@withContext true
            } catch (ex : IOException) {
                Log.i("Networking", "Failed to connect to remote per TLS! $ex")
                return@withContext false
            }
        }
        connected = con
    }

    private fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }
    private fun Int.to2ByteArray() : ByteArray = byteArrayOf(shr(8).toByte(), toByte())

    // Expecting Big Endian
    private fun ByteArray.toUInt16(): UInt {
        val upper = this[0].toUByte()
        val lower = this[1].toUByte()
        return (upper.toUInt() shl 8) + lower.toUInt()
    }

    /*
    ----------------------------------------------------------------
    Internal state : Sending and Receiving
    ----------------------------------------------------------------
     */

    private suspend fun startSending() {
        Log.i("Networking", "Starting to send...")
        val con = withContext(Dispatchers.IO) {
            try {
                val sendChannel = socket!!.openWriteChannel(autoFlush = true)
                sending = true
                while (true) {
                    // Fetching the next packet from the queue of packets that should be sent
                    val nextPacket = outputChannel.receive()
                    Log.i("Networking Start Sending", "Sending: ${nextPacket.toHexString()}")

                    sendChannel.toOutputStream().write(nextPacket)
                }
            } catch (ex : NullPointerException) {
                Log.i("Networking", "Failed to init send channel for remote")
            } catch (ex : IOException) {
                Log.i("Networking", "Failed to send to remote")
            }
        }
        sending = false
    }

    private suspend fun startListening() {
        Log.i("Networking", "Starting to receive...")
        incomingChannel = withContext(Dispatchers.IO) {
            return@withContext socket!!.openReadChannel().toInputStream()
        }
        val recChannel = incomingChannel
        val con = withContext(Dispatchers.IO) {
            try {
                receiving = true
                val sizeBuffer = ByteArray(2)
                while(true) {
                    var read = recChannel!!.read(sizeBuffer, 0, 2)
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

                    // TODO: Clean this mess up
                    ApollonProtocolHandler.ReceiveAny(packetBuffer)
//                    registeredCallbacks[Pair(header.Category.toLong(), header.Type.toLong())]?.let {
//                        for(cal in it) {
//                            cal.invoke(sPacket, recChannel)
//                        }
//                    }
                }
            } catch (ex : NullPointerException) {
                Log.i("Networking", "Failed to get read channel for remote")
            } catch (ex : IOException) {
                Log.i("Networking", "Failed to read from remote")
            }
        }
        this.receiving = false
    }

}