package com.example.apollonchat.networking

import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.Dispatchers
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.security.cert.X509Certificate
import java.util.concurrent.Executors
import javax.net.ssl.X509TrustManager
import kotlin.coroutines.coroutineContext

abstract class Networking(hostAddress : InetAddress) {

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

    @Volatile
    var INSTANCE : Networking? = null

    val remoteAddress : InetAddress = hostAddress
    lateinit var inputStream : InputStream
    lateinit var outputStream : OutputStream
    lateinit var socket : Socket

    /*
    ----------------------------------------------------------------
    Singleton
    ----------------------------------------------------------------
     */

    fun getInstance(hostAddress: InetAddress) : Networking {
        var instance = INSTANCE
        if(instance == null) {
            instance = this
        }
        return instance
    }

    fun write(byteArray: ByteArray) {
        try {
            outputStream.write(byteArray)
        } catch (ex : IOException) {
            ex.printStackTrace()
        }
    }

    fun start() {
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
//        val selManager = SelectorManger(Dispatchers.IO)
//        socket = aSocket(selManager).tcp().connect(remoteAddress)
        // The TLS variant
//        socket = aSocket(selManager).tcp().connect(remoteAddress).tls(coroutineContext = coroutineContext) {
//            trustManager = object : X509TrustManager {
//                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
//                override fun checkClientTrusted(p0: Array<out X509Certificate>?, p1: String?) {}
//                override fun checkServerTrusted(p0: Array<out X509Certificate>?, p1: String?) {}
//            }
//        }
        Log.i("Networking", "Listing on ...")

        startSending()
        startListening()
    }

    private fun startSending() {
        // TODO: move to own thread or handling inside of new thread
//        val sendChannel = socket.openWriteChannel()
        while(true) {
            val data = byteArrayOf()
            // Fetch data from the internal queue
//            sendChannel.write(data)
//            sendChannel.flush()
        }
    }

    private fun startListening() {

    }

}