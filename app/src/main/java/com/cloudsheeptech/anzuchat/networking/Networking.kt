package com.cloudsheeptech.anzuchat.networking

import android.content.Context
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
import kotlinx.coroutines.sync.Semaphore
import java.io.IOException
import java.net.InetAddress
import kotlin.concurrent.thread

object Networking {

    class Configuration (
        var remote : String,
        var secure : Boolean,
    ) {
//        constructor() : this("homecloud.homeplex.org", true)
        constructor() : this("10.0.2.2", false)
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
    private var init : Boolean = false
    private var connected : Boolean = false

    private var networkingJob = Job()
    private val netScope = CoroutineScope(Dispatchers.Main + networkingJob)

    // Configuration
    private var remoteAddress : String? = null
    private var remotePort : Int = 0
    private var connectSecure : Boolean = false

    // Runtime and reception
    private var receiveChannel : Channel<ByteArray>? = null

    /*
    ----------------------------------------------------------------
    Public API
    ----------------------------------------------------------------
    */

    fun initialize(configuration : Configuration, chan : Channel<ByteArray>) {
        if (!init) {
            remoteAddress = configuration.remote
            connectSecure = configuration.secure
            remotePort = if (connectSecure) 50001 else 50000
            receiveChannel = chan
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

        // TODO: Move to same producer consumer pattern?
        // Then starting to send and receiver
        if (sending == null) {
            sending = thread {
                runBlocking {
                    startSending()
                }
            }
        }
    }

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
                    Log.i("Networking", "Sending: ${nextPacket.toHexString()}")

                    sendChannel.toOutputStream().write(nextPacket)
                }
            } catch (ex : NullPointerException) {
                Log.i("Networking", "Failed to init send channel for remote")
            } catch (ex : IOException) {
                Log.i("Networking", "Failed to send to remote")
            } catch (ex : Exception) {
                Log.i("Networking", "Some error occured: $ex")
                return@withContext
            }
        }
        initBarrier.release()
        sending = null
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun CoroutineScope.receivePackets() : ReceiveChannel<ByteArray> = produce {
        initBarrier.acquire()
        Log.i("Networking", "Starting receive...")
        val readBuffer = ByteArray(100)
        val recChannel = socket!!.openReadChannel().toInputStream()

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
}