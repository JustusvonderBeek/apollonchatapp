package com.example.apollonchat

import android.util.Log
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.apollonchat.database.ApollonDatabase
import com.example.apollonchat.database.contact.Contact
import com.example.apollonchat.networking.Networking
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.util.hex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.IllegalStateException
import java.net.InetAddress
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
class NetworkTest {

    @Before
    fun initializeEndpoint() {

    }

    private fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }

    private fun convertIntToByteArray(number : Int) : ByteArray {
        val buffer = ByteArray(4)
        buffer[0] = (number shr 0).toByte()
        buffer[1] = (number shr 8).toByte()
        buffer[2] = (number shr 16).toByte()
        buffer[3] = (number shr 24).toByte()
        return buffer
    }

    private fun convertIntToByte(number : Int) : ByteArray {
        val buffer = ByteArray(1)
        buffer[0] = (number shr 0).toByte()
        return buffer
    }

    @Test
    fun testInitNetwork() {
        val netConfig = Networking.Configuration()
//        val remote = InetAddress.getByName("10.2.0.2")
        val secure = false
        netConfig.remote = "10.2.0.2"
        netConfig.secure = secure
        Networking.initialize(netConfig)
        Networking.initialize(netConfig)

//        Assert.assertEquals(Networking.remoteAddress, remote)
//        Assert.assertEquals(Networking.connectSecure, secure)
    }

    @Test
    fun testStartNetworkTCP() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        runBlocking {
            // Expecting an exception
            try {
                Networking.start(context)
                Assert.fail()
            } catch (ex : IllegalStateException) {
                Log.i("NetworkTest", "Exception correctly thrown")
            }

            val netConfig = Networking.Configuration()
            netConfig.secure = false
            netConfig.remote = "10.2.0.2"
            Networking.initialize(netConfig)
            Networking.start(context)

            delay(500L)
            Networking.start(context)
            delay(500L)
            Networking.start(context)
            delay(500L)
        }
    }

    @Test
    fun testStartNetworkTLS() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        runBlocking {
            // Expecting an exception
            try {
                Networking.start(context)
                Assert.fail()
            } catch (ex : IllegalStateException) {
                Log.i("NetworkTest", "Exception correctly thrown")
            }

            val netConfig = Networking.Configuration()
            netConfig.secure = true
            netConfig.remote = "10.2.0.2"
            Networking.initialize(netConfig)
            Networking.start(context)

            delay(500L)
            Networking.start(context)
            delay(500L)
            Networking.start(context)
            delay(500L)
        }
    }

    @Test
    fun TestSending() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val job = Job()
        val netScope = CoroutineScope(Dispatchers.Main + job)

        // Starting a local endpoint
        netScope.launch {
            val selectorManager = SelectorManager(Dispatchers.IO)
            val serverSocket = aSocket(selectorManager).tcp().bind("127.0.0.1", 50000)
            val c = serverSocket.accept()
            val channel = c.openReadChannel()
            channel.read(10) {
                Log.i("NetworkTest", "Buffer received")
            }
        }

        runBlocking {
            val netConfig = Networking.Configuration()
            netConfig.secure = false
            netConfig.remote = "127.0.0.1"
            Networking.initialize(netConfig)
            Networking.start(context)

            val data = ByteArray(10)
            Networking.write(data)
            delay(200L)
        }

        job.cancel()
    }

}