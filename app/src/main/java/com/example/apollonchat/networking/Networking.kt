package com.example.apollonchat.networking

import android.content.Context
import android.util.Log
import com.example.apollonchat.R
import com.example.apollonchat.networking.ApollonProtocolHandler.ApollonProtocolHandler
import com.example.apollonchat.networking.certificate.ApollonNetworkConfigCreator
import com.example.apollonchat.networking.packets.*
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.network.tls.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.IOException
import java.io.InputStream
import java.net.InetAddress
import java.util.Hashtable
import kotlin.concurrent.thread
import kotlin.reflect.KSuspendFunction2

object Networking {

    class Configuration (
        var remote : InetAddress,
        var headerSize : Int,
        var secure : Boolean,
    ) {
        constructor() : this(InetAddress.getByName("10.0.2.2"), 10, false)
    }

    /*
    * This class contains the logic to send data to the server
    * and retrieve answers back from the server.
    * It supports connection re-establishment and DNS resolution.
    * Features:
    * + Independent sending and receiving
    * + Injection of packet handling callbacks
    * + Usage of both plain TCP and TCP/TLS connections
     */

    /* Relies on the ktor API to work */

    /*
    ----------------------------------------------------------------
    Definitions
    ----------------------------------------------------------------
     */

    // Expecting not more than 1 or 2 messages normally at the same time (exception images)
    private var outputChannel : Channel<ByteArray> = Channel(20)
    private var socket : Socket? = null
    private var sending : Thread? = null
    private var receiving : Thread? = null
    private var init : Boolean = false
    private var connected : Boolean = false
    private var json = Json { ignoreUnknownKeys = true }

    private var networkingJob = Job()
    private val netScope = CoroutineScope(Dispatchers.Main + networkingJob)

    private var registeredCallbacks = Hashtable<Pair<Long, Long>, MutableList<(String, InputStream) -> Unit>>()
    private var defaultCallback = mutableListOf<KSuspendFunction2<ByteArray, InputStream, Unit>>()

    // Configuration
    private var remoteAddress : InetAddress? = null
    private var connectSecure : Boolean = false
    private var headerSize : Int = 2

    /*
    ----------------------------------------------------------------
    Public API
    ----------------------------------------------------------------
    */

    fun initialize(configuration : Configuration) {
        if (!init) {
            remoteAddress = configuration.remote
            connectSecure = configuration.secure
            headerSize = configuration.headerSize
            init = true
        }
    }

    // TODO: Test, Cleanup
    fun start(context : Context) {
        if (!init) {
            throw IllegalStateException("'start()' called before 'initialize()'!")
        }
        // Connecting to the endpoint
        connect(context)

        // Then starting to send and receiver
        if (sending == null || !sending!!.isAlive) {
            sending = thread {
                netScope.launch { startSending() }
            }
        }
        if (receiving == null || !receiving!!.isAlive) {
            receiving = thread {
                netScope.launch { startListening() }
            }
        }
    }

    // TODO: Error handling and testing
    fun write(data : ByteArray) {
        netScope.launch {
            outputChannel.send(data)
        }
    }

    // TODO: Restructure to allow for injection of packet handling routine
    fun registerCallback(category : Long, type : Long, callback : (String, InputStream) -> Unit) {
        if (registeredCallbacks[Pair(category, type)] == null) {
            registeredCallbacks[Pair(category, type)] = mutableListOf(callback)
        } else {
            registeredCallbacks[Pair(category, type)]!!.add(callback)
        }
    }

    // TODO: Remove and combine in above function
    fun registerCallback(callback: KSuspendFunction2<ByteArray, InputStream, Unit>) {
        defaultCallback.add(callback)
    }

    /*
    ----------------------------------------------------------------
    Private API
    ----------------------------------------------------------------
     */

    private fun connect(context: Context) {
        if (connected && socket != null && !socket!!.isClosed) {
            Log.i("Networking", "Network already connected")
            return
        }
        if (connectSecure) {
            netScope.launch { connectSecure(context) }
//            Log.i("Networking", "Exited connectSecure")
        } else {
            netScope.launch { connectDefault() }
        }
//        Log.i("Networking", "After connection launch")
    }

    // TODO: Cleanup
    private suspend fun connectDefault() {
        if (connected) {
            Log.i("Networking", "Already connected")
            return
        }
         val con = withContext(Dispatchers.IO) {
            val selManager = SelectorManager(Dispatchers.IO)
            try {
//                socket = aSocket(selManager).tcp().connect("homecloud.homeplex.org", port = 50000)
                // This address should emulate the localhost address
                socket = aSocket(selManager).tcp().connect(remoteAddress!!.hostAddress!!, port = 50000)
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

    // TODO: Cleanup
    private suspend fun connectSecure(context: Context) {
        val con = withContext(Dispatchers.IO) {
            // The TLS variant
            try {
                val selManager = SelectorManager(Dispatchers.IO)
                val tlsConfig = ApollonNetworkConfigCreator.createTlsConfig(context.resources.openRawResource(R.raw.apollon))
                // If the context is set as 'coroutineContext' then the method DOES NOT return back to the caller side!
                socket = aSocket(selManager).tcp().connect(remoteAddress!!.hostAddress!!, port = 50001).tls(Dispatchers.IO, tlsConfig)
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

    // Move to own section
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

    // TODO: Test, Error handling
    private suspend fun startSending() {
        if (sending != null && sending!!.isAlive) {
            Log.i("Networking", "Already sending")
            return
        }
        Log.i("Networking", "Starting to send...")
        val con = withContext(Dispatchers.IO) {
            try {
                val sendChannel = socket!!.openWriteChannel(autoFlush = true)
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
        sending = null
    }

    // TODO: Combine with possible handling method to allow for my protocol
//     including size of header field to be expected
     private suspend fun startListening() {
        if (receiving != null && receiving!!.isAlive) {
            Log.i("Networking", "Already receiving")
            return
        }
        Log.i("Networking", "Starting to receive...")
        val con = withContext(Dispatchers.IO) {
            try {
                val recChannel = socket!!.openReadChannel().toInputStream()
//                val payloadReader = recChannel.bufferedReader(Charsets.UTF_8)
                val headerBuffer = ByteArray(headerSize)
                while(true) {
                    var read = recChannel.read(headerBuffer, 0, headerSize)
                    while(read < headerSize) {
                        read = recChannel.read(headerBuffer, read, headerBuffer.size - read)
                    }

                    // Big endian
//                    val size = sizeBuffer.toUInt16() - 2U
//                    Log.i("Networking", "Size expected: $size - ${sizeBuffer.toHexString()} - ${sizeBuffer[1].toUByte()}")
//                    val packetBuffer = ByteArray(size.toInt())
//                    read = recChannel.read(packetBuffer)
//                    val content = payloadReader.readLine()
                    // Save the read data into a consumer queue for another thread to handle
                    // Convert the packet to String and give to JSON to handle
//                    val sPacket = packetBuffer.toString(Charsets.UTF_8)
//                    val header = json.decodeFromString<Header>(content)
//                     TODO: Decode
//                    Log.i("Networking", "Received cat: ${header.Category}, type: ${header.Type}")

                    // TODO: Clean this mess up
//                    ApollonProtocolHandler.ReceiveAny(packetBuffer, recChannel)
                    ApollonProtocolHandler.ReceiveAny(headerBuffer, recChannel)
//                    defaultCallback[0].invoke(packetBuffer, recChannel)
//                    registeredCallbacks[Pair(header.Category.toLong(), header.Type.toLong())]?.let {
//                        for(cal in it) {
//                            cal.invoke(sPacket, recChannel)
//                        }
//                    }
                }
            } catch (ex : NullPointerException) {
                Log.i("Networking", "Failed to get read channel for remote: ${ex.printStackTrace()}")
            } catch (ex : IOException) {
                Log.i("Networking", "Failed to read from remote")
            }
        }
        this.receiving = null
    }

}