package com.cloudsheeptech.anzuchat.networking

import android.content.Context
import android.content.res.Resources
import android.util.Log
import com.cloudsheeptech.anzuchat.R
import com.cloudsheeptech.anzuchat.networking.certificate.ApollonNetworkConfigCreator
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.network.tls.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import java.io.IOException
import java.net.InetAddress
import kotlin.concurrent.thread

/*
   * This class contains the logic to send and receive data from the server
   * It supports connection re-establishment and DNS resolution.
   * Features:
   * Using TCP and TCP/TLS to communicate with an endpoint
   * Easy API with only send and receive
   * Automatic connection and reconnection mechanism
   * Disconnect allows to go offline
    */
/* Relies on the ktor API to work */
object Networking {

    class Configuration (
        var remote : String,
        var secure : Boolean,
        var bufferSize : Int,
    ) {
//        constructor() : this("anzuchat.cloudsheeptech.com", true, 1024)
//        constructor() : this("10.0.2.2", false, 1024)
        constructor() : this("10.124.25.166", false, 1024)
    }

    /*
    ----------------------------------------------------------------
    Definitions
    ----------------------------------------------------------------
     */

    private var networkingJob = Job()
    private val netScope = CoroutineScope(Dispatchers.Main + networkingJob)

    // Internal state for connection management
    private var connected : Boolean = false

    // Expecting not more than 1 or 2 messages normally at the same time (exception images)
    private var outputChannel : Channel<ByteArray> = Channel(20)
    private var socket : Socket? = null
    private var sendingThread : Thread? = null
    private val initBarrier : Semaphore = Semaphore(2, 2)

    // Configuration
    private var remotePort : Int = 50000
    private lateinit var tlsConfig : TLSConfig
    private lateinit var config : Configuration

    // Runtime and reception
    private var receiveChannel : Channel<ByteArray>? = null

    /*
    ----------------------------------------------------------------
    Private Constructor
    ----------------------------------------------------------------
     */


    fun init(config : Networking.Configuration, context: Context) {
        this.config = config
        this.remotePort = if (config.secure) 50001 else 50000
        this.tlsConfig = ApollonNetworkConfigCreator.createTlsConfig(context.resources.openRawResource(R.raw.apollon))
        this.connected = false
    }

    /*
    ----------------------------------------------------------------
    Public API:
    - Send
    - Receive
    - Disconnect
    ----------------------------------------------------------------
    */

    fun send(data : ByteArray) {
        connect()
        netScope.launch {
            outputChannel.send(data)
        }
    }

    fun disconnect() {
        socket?.close()
        sendingThread.runCatching {
            this?.interrupt()
        }
        this.connected = false
    }

    /*
    ----------------------------------------------------------------
    Private API:
    - Connect
    - Disconnect
    - Send Thread
    - Receive Thread
    ----------------------------------------------------------------
     */

    private fun connect() {
        if (connected && socket != null && !socket!!.isClosed) {
            Log.i("Networking", "Network already connected")
            return
        }

        if (config.secure) {
            netScope.launch {
                initBarrier.withPermit { connectSecure() }
                initBarrier.release() // Release for the receiving to start
            }
            Log.d("Networking", "Exited connectSecure")
        } else {
            netScope.launch {
                initBarrier.withPermit { connectDefault() }
                initBarrier.release() // Release for the receiving to start
            }
            Log.d("Networking", "Exited connect")
        }

        if (sendingThread == null) {
            try {
                sendingThread = thread {
                    runBlocking {
                        initBarrier.withPermit {
                            send(outputChannel)
                        }
                    }
                }
            } catch (ex : InterruptedException) {
                Log.i("Networking", "Disconnect called and sending interrupted")
            }
        }
    }

    private suspend fun connectDefault() : Boolean {
        if (connected && socket != null && !socket!!.isClosed) {
            Log.i("Networking", "Already TCP connected")
            return true
        }
        val status = withContext(Dispatchers.IO) {
            val selManager = SelectorManager(Dispatchers.IO)
            try {
                socket = aSocket(selManager).tcp().connect(InetAddress.getByName(config.remote).hostAddress!!, port = remotePort)

                Log.i("Networking", "Connected to remote per TCP")
                return@withContext true
            } catch (ex : IOException) {
                Log.i("Networking", "Connection failed: $ex")
                return@withContext false
            }
        }
        return status
    }

    private suspend fun connectSecure() : Boolean {
        if (connected && socket != null && !socket!!.isClosed) {
            Log.i("Networking", "Already TLS connected")
            return true
        }
        val status = withContext(Dispatchers.IO) {
            try {
                val selManager = SelectorManager(Dispatchers.IO)

                // If the context is set as 'coroutineContext' then the method DOES NOT return back to the caller side!
                socket = aSocket(selManager).tcp().connect(InetAddress.getByName(config.remote).hostAddress!!, port = remotePort) {
                    socketTimeout = 1000
                }.tls(Dispatchers.IO, tlsConfig)

                Log.i("Networking", "Connected to remote per TLS")
                return@withContext true
            } catch (ex : IOException) {
                Log.i("Networking", "Failed to connect to remote per TLS! $ex")
                return@withContext false
            }
        }
        return status
    }

    /*
    ----------------------------------------------------------------
    Internal state : Sending and Receiving
    ----------------------------------------------------------------
     */

    private suspend fun CoroutineScope.send(packets : ReceiveChannel<ByteArray>) {
        val sendChannel = socket?.openWriteChannel(autoFlush = true)
        if (sendChannel == null) {
            sendingThread = null
            return
        }
        val networkChannel = sendChannel.toOutputStream(networkingJob)
        try {
            withContext(Dispatchers.IO) {
                while (true) {
                    val nextPacket = packets.receive()
                    Log.d("Networking", "Sending: ${nextPacket.sliceArray(0..9).toHexString()}")
                    networkChannel.write(nextPacket)
                }
            }
        } catch (ex : NullPointerException) {
            Log.i("Networking", "Failed to init send channel for remote")
        } catch (ex : IOException) {
            Log.i("Networking", "Failed to send to remote: $ex")
        } catch (ex : Exception) {
            Log.i("Networking", "Some error occurred: $ex")
        }
    }

//
//    // TODO: Test, Error handling
//    private suspend fun startSending() {
//        if (sendingThread != null && sendingThread!!.isAlive) {
//            if (sendingThread != Thread.currentThread()) {
//                Log.d("Networking", "Already sending")
//                return
//            }
//        }
//        sendingThread = Thread.currentThread()
//        Log.i("Networking", "Starting to send...")
//        withContext(Dispatchers.IO) {
//            try {
//                if (socket == null) {
//                    connect()
//                    // Should spawn a new thread doing the work we are currently doing, so exit
//                    return@withContext
//                }
//                val sendChannel = socket?.openWriteChannel(autoFlush = true)
//                if (sendChannel == null) {
//                    sendingThread = null
//                    return@withContext
//                }
//                while (true) {
//                    // Fetching the next packet from the queue of packets that should be sent
//                    val nextPacket = outputChannel.receive()
//
//                    Log.d("Networking", "Sending: ${nextPacket.sliceArray(0..9).toHexString()}")
//
//                    sendChannel.toOutputStream().write(nextPacket)
//                }
//            } catch (ex : NullPointerException) {
//                Log.i("Networking", "Failed to init send channel for remote")
//            } catch (ex : IOException) {
//                Log.i("Networking", "Failed to send to remote: $ex")
//            } catch (ex : Exception) {
//                Log.i("Networking", "Some error occurred: $ex")
//                return@withContext
//            }
//        }
//        sendingThread = null
//    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun CoroutineScope.produceBytes() : ReceiveChannel<ByteArray> = produce {
        initBarrier.acquire()
        Log.d("Networking", "Starting receive...")
        val readBuffer = ByteArray(config.bufferSize)
        val recChannel = socket?.openReadChannel()?.toInputStream() ?: return@produce

        withContext(Dispatchers.IO) {
            while(true) {
                val read = recChannel.read(readBuffer, 0, readBuffer.size - 1)
//                val read = recChannel.readBytes()
                if (read == 0) {
                    Log.i("Networking", "Remote endpoint closed connection")
                    return@withContext
                }

                send(readBuffer.sliceArray(0 until read))
//                send(read)
            }
        }
    }


    /*
    ----------------------------------------------------------------
    Helper functionality
    ----------------------------------------------------------------
     */

    private fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }
}