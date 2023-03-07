package com.example.apollonchat.networking

import android.util.Log
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.util.date.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException
import java.io.InputStream
import java.net.InetAddress
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue

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
    lateinit var socket : Socket
    var started : Boolean = false

    private var networkingJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + networkingJob)
    private val ioScope = CoroutineScope(Dispatchers.IO + networkingJob)

    fun write(data : Message) {
        try {
            val sData = Json.encodeToString(data)
            val rawPacketData = sData.toByteArray(charset = Charsets.UTF_8)
            val packet = (rawPacketData.size + 2).to2ByteArray() + rawPacketData
            outputQueue.put(packet)
        } catch (ex : IOException) {
            ex.printStackTrace()
        }
    }

    fun start(remoteAddress: InetAddress) {
        this.remoteAddress = remoteAddress
        this.outputQueue = ArrayBlockingQueue(20)
        this.inputQueue = ArrayBlockingQueue(20)

        if (this.started) {
            Log.i("Networking", "Already started the network...")
            return
        } else {
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
        uiScope.launch {
            withContext(Dispatchers.IO) {
                val selManager = SelectorManager(Dispatchers.IO)
                socket = aSocket(selManager).tcp().connect("192.168.2.10", port = 50000)

                // The TLS variant
//        socket = aSocket(selManager).tcp().connect(remoteAddress).tls(coroutineContext = coroutineContext) {
//            trustManager = object : X509TrustManager {
//                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
//                override fun checkClientTrusted(p0: Array<out X509Certificate>?, p1: String?) {}
//                override fun checkServerTrusted(p0: Array<out X509Certificate>?, p1: String?) {}
//            }
//        }
//                Log.i("Networking", "Listing on ...")

                startSending()
                startListening()
            }

        }

    }

    private fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }
    private fun Int.to2ByteArray() : ByteArray = byteArrayOf(shr(8).toByte(), toByte())

    private suspend fun startSending() {
        Log.i("Networking", "Starting to send...")
        ioScope.launch {
            val sendChannel = socket.openWriteChannel(autoFlush = true)
            while(true) {
                val packet = withContext(Dispatchers.IO) {
                    return@withContext outputQueue.take()
                }
                // TODO: Fetch data from queue
//                val message = Message(Category = 0x2, Type = 0x1, UserId = 12345U, MessageId = 814223U, ContactUserId = 54321U, Timestamp = getTimeMillis().toString(), Part = 0U, Message = "Das ist eine erste Nachricht :)")
//                val sMessage = Json.encodeToString(message)
//                val rawMessage = sMessage.toByteArray(charset("UTF-8"))
//                val length = (rawMessage.size + 2).to2ByteArray()
//                val packet = length + rawMessage

//                Log.i("Networking", "Sending: $message \n ${packet.toHexString()}")
                Log.i("Networking", "Sending: ${packet.toHexString()}")
                withContext(Dispatchers.IO) {
                    sendChannel.toOutputStream().write(packet)
                }
            }
        }
    }

    private var json = Json { ignoreUnknownKeys = true }

    private fun startListening() {
        Log.i("Networking", "Starting to receive...")
        ioScope.launch {
            val recChannel = socket.openReadChannel().toInputStream()
            val sizeBuffer = ByteArray(2)
            while(true) {
                var read = 0
                withContext(Dispatchers.IO) {
                    read += recChannel.read(sizeBuffer)
//                    while(read < 2) {
//                        recChannel.read(sizeBuffer, read, sizeBuffer.size - read)
//                    }
                }
//                val size = (sizeBuffer[0].toInt() shl 8) and sizeBuffer[1].toInt()
                val size = sizeBuffer[1].toInt() - 2
                Log.i("Networking", "Size expected: $size - ${sizeBuffer.toHexString()} - ${sizeBuffer[1]}")
                val packetBuffer = ByteArray(size)
                withContext(Dispatchers.IO) {
                    read = recChannel.read(packetBuffer)
                }
                // Save the read data into a consumer queue for anthoer thread to handle
                // Convert the packet to String and give to JSON to handle
                val sPacket = packetBuffer.toString(Charsets.UTF_8)
                val header = json.decodeFromString<Header>(sPacket)
                // TODO: Decode
                Log.i("Networking", "Received cat: ${header.Category}, type: ${header.Type}")

            }
        }
    }



    /*
    ----------------------------------------------------------------
    Singleton
    ----------------------------------------------------------------
     */

}