package com.cloudsheeptech.anzuchat

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cloudsheeptech.anzuchat.networking.Networking
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.IllegalStateException

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
        netConfig.remote = "10.0.2.2"
        netConfig.secure = secure
        val recChannel = Channel<ByteArray>(20)
        Networking.initialize(netConfig, recChannel)
        Networking.initialize(netConfig, recChannel)

//        Assert.assertEquals(Networking.remoteAddress, remote)
//        Assert.assertEquals(Networking.connectSecure, secure)
    }

    @Test
    fun testStartNetworkTCP() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val job = Job()
        val netScope = CoroutineScope(Dispatchers.Main + job)

        val initBarrier = Semaphore(1, 1)

        // Starting the endpoint for the test
        netScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val selectorManager = SelectorManager(Dispatchers.IO)
                    val serverSocket = aSocket(selectorManager).tcp().bind("127.0.0.1", 50000)
                    initBarrier.release()
                    var client = serverSocket.accept()
                    val readChannel = client.openReadChannel()
                    readChannel.read(10) {
                        Log.i("NetworkTest", "Read 10 bytes")
                    }
                    client.close()
                    initBarrier.release()
                    delay(3000L)
                    client = serverSocket.accept()
                    client.openReadChannel()
                } catch (ex : Exception) {
                    Log.i("NetworkTest", "Server failed: $ex")
                }
            }
        }


        runBlocking {
            // Expecting an exception
            try {
                Networking.start(context)
                Assert.fail()
            } catch (ex : IllegalStateException) {
                Log.i("NetworkTest", "Exception correctly thrown")
            }

            val randomData = ByteArray(10)
            val netConfig = Networking.Configuration()
            netConfig.secure = false
            netConfig.remote = "127.0.0.1"
            val recChannel = Channel<ByteArray>(20)
            Networking.initialize(netConfig, recChannel)
            initBarrier.acquire()
            Networking.start(context)
            Networking.write(randomData)
            initBarrier.acquire()
            // Should fail!
            Networking.write(randomData)

            Networking.start(context)
            delay(1000L)
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
            val recChannel = Channel<ByteArray>(20)
            Networking.initialize(netConfig, recChannel)
            Networking.start(context)

            delay(500L)
            Networking.start(context)
            delay(500L)
            Networking.start(context)
            delay(500L)
        }
    }

    @Test
    fun testSending() {
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
            val recChannel = Channel<ByteArray>(20)
            Networking.initialize(netConfig, recChannel)
            Networking.start(context)

            val data = ByteArray(10)
            Networking.write(data)
            delay(200L)
        }

        job.cancel()
    }

    @Test
    fun testReceiveMethod() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val job = Job()
        val netScope = CoroutineScope(Dispatchers.Main + job)

        val channel = Channel<ByteArray>(20)

        netScope.launch {
            runBlocking {
                val netConfig = Networking.Configuration()
                netConfig.secure = false
                netConfig.remote = "10.0.2.2"
                Networking.initialize(netConfig, channel)
                Networking.start(context)
            }
        }

        var counter  = 0
        netScope.launch {
            runBlocking {
                for (i in 0..10) {
                    Assert.assertNotNull(channel.receive())
                    counter += 1
                    Log.i("NetworkTest", "Received packet!")
                }
            }
        }

        runBlocking {
            delay(1000L)
        }

        netScope.cancel("Timeout")
        Assert.assertEquals(10, counter)
    }

}