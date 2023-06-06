package com.example.apollonchat.networking

import android.content.Context
import android.util.Log
import com.example.apollonchat.R
import com.example.apollonchat.networking.ApollonProtocolHandler.ApollonProtocolHandler
import com.example.apollonchat.networking.certificate.ApollonNetworkConfigCreator
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.network.tls.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Semaphore
import java.io.IOException
import java.io.InputStream
import java.net.Inet4Address
import java.net.InetAddress
import java.util.Hashtable
import kotlin.concurrent.thread
import kotlin.reflect.KSuspendFunction2

object Networking {

    class Configuration (
        var remote : String,
        var headerSize : Int,
        var secure : Boolean,
    ) {
//        constructor() : this("homecloud.homeplex.org", 10, true)
        constructor() : this("10.0.2.2", 10, false)
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
    private val initBarrier : Semaphore = Semaphore(2, 2)
    private var sending : Thread? = null
    private var receiving : Thread? = null
    private var init : Boolean = false
    private var connected : Boolean = false

    private var networkingJob = Job()
    private val netScope = CoroutineScope(Dispatchers.Main + networkingJob)

    // Configuration
    private var remoteAddress : String? = null
    private var remotePort : Int = 0
    private var connectSecure : Boolean = false
    private var headerSize : Int = 10

    /*
    ----------------------------------------------------------------
    Public API
    ----------------------------------------------------------------
    */

    fun initialize(configuration : Configuration) {
        if (!init) {
            remoteAddress = configuration.remote
            connectSecure = configuration.secure
            remotePort = if (connectSecure) 50001 else 50000
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
        if (sending == null) {
            sending = thread {
                runBlocking {
                    startSending()
                }
            }
        }
        if (receiving == null) {
            receiving = thread {
                runBlocking {
                    startListening()
                }
            }
        }
    }

    // TODO: Error handling and testing
    fun write(data : ByteArray) {
        netScope.launch {
            outputChannel.send(data)
        }
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

    // TODO: Fix waiting, signalling
    private suspend fun connectDefault() {
        if (connected && socket != null && !socket!!.isClosed) {
            Log.i("Networking", "Already TCP connected")
            return
        }
         connected = withContext(Dispatchers.IO) {
            val selManager = SelectorManager(Dispatchers.IO)
            try {
                socket = aSocket(selManager).tcp().connect(InetAddress.getByName(remoteAddress).hostAddress!!, port = remotePort)

                Log.i("Networking", "Connected to remote per TCP")
                // TODO: Implement proper barrier instead of this Voudu shit
                initBarrier.release()
                initBarrier.release()
                return@withContext true
            } catch (ex : IOException) {
                Log.i("Networking", "Connection failed: $ex")
                return@withContext false
            }
        }
    }

    // TODO: Cleanup
    private suspend fun connectSecure(context: Context) {
        if (connected && socket != null && !socket!!.isClosed) {
            Log.i("Networking", "Already TLS connected")
            return
        }
        connected = withContext(Dispatchers.IO) {
            try {
                val selManager = SelectorManager(Dispatchers.IO)
                // The TLS variant
                val tlsConfig = ApollonNetworkConfigCreator.createTlsConfig(context.resources.openRawResource(R.raw.apollon))
                // If the context is set as 'coroutineContext' then the method DOES NOT return back to the caller side!
                socket = aSocket(selManager).tcp().connect(InetAddress.getByName(remoteAddress).hostAddress!!, port = remotePort).tls(Dispatchers.IO, tlsConfig)

                Log.i("Networking", "Connected to remote per TLS")
                initBarrier.release()
                initBarrier.release()
                return@withContext true
            } catch (ex : IOException) {
                Log.i("Networking", "Failed to connect to remote per TLS! $ex")
                return@withContext false
            }
        }
    }

    // Move to own section
    private fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }

    /*
    ----------------------------------------------------------------
    Internal state : Sending and Receiving
    ----------------------------------------------------------------
     */

    // TODO: Test, Error handling
    private suspend fun startSending() {
        if (sending != null && sending!!.isAlive) {
            if (sending != Thread.currentThread()) {
                Log.i("Networking", "Already sending")
                return
            }
        }
        initBarrier.acquire()
        Log.i("Networking", "Starting to send...")
        withContext(Dispatchers.IO) {
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
        initBarrier.release()
        sending = null
    }

    // TODO: Combine with possible handling method to allow for my protocol
//     including size of header field to be expected
     private suspend fun startListening() {
        if (receiving != null && receiving!!.isAlive) {
            if (receiving != Thread.currentThread()) {
                Log.i("Networking", "Already receiving")
                return
            }
        }
        initBarrier.acquire()
        Log.i("Networking", "Starting to receive...")
        val con = withContext(Dispatchers.IO) {
            try {
                val recChannel = socket!!.openReadChannel().toInputStream()
                val headerBuffer = ByteArray(headerSize)
                while(true) {
                    var read = recChannel.read(headerBuffer, 0, headerSize)
                    while(read < headerSize) {
                        read = recChannel.read(headerBuffer, read, headerSize - read)
                    }

                    // TODO: Clean this mess up
                    // Either this one line or the callback. Check what is faster
                    ApollonProtocolHandler.receiveAny(headerBuffer, recChannel)
//                    defaultCallback[0].invoke(headerBuffer, recChannel)
//                    defaultCallback[0].invoke(packetBuffer, recChannel)
                }
            } catch (ex : NullPointerException) {
                Log.i("Networking", "Failed to get read channel for remote: ${ex.printStackTrace()}")
            } catch (ex : IOException) {
                Log.i("Networking", "Failed to read from remote")
            }
        }
        initBarrier.release()
        this.receiving = null
    }

}