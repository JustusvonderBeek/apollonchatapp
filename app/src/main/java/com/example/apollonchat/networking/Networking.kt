package com.example.apollonchat.networking

import android.os.Handler
import android.os.Looper
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.Executors

class Networking(hostAddress : InetAddress) : Thread() {

    /* This class contains the logic to send data to the server
    * and retrieve answers back from the server.
    * It supports connection re-establishment and DNS resolution
     */

    /*
    ----------------------------------------------------------------
    Definitions
    ----------------------------------------------------------------
     */

    val remoteAddress : InetAddress = hostAddress
    lateinit var inputStream : InputStream
    lateinit var outputStream : OutputStream
    lateinit var socket : Socket

    fun write(byteArray: ByteArray) {
        try {
            outputStream.write(byteArray)
        } catch (ex : IOException) {
            ex.printStackTrace()
        }
    }

    override fun run() {
        try {
            socket = Socket()
            socket.connect(InetSocketAddress(remoteAddress, 15467), 1000)
            inputStream = socket.getInputStream()
            outputStream = socket.getOutputStream()
        } catch (ex : IOException) {
            // TODO: Add more sophisticated error handling and UI Interaction
            ex.printStackTrace()
        }

        val executor = Executors.newSingleThreadExecutor()
        var handler = Handler(Looper.getMainLooper())


    }

}