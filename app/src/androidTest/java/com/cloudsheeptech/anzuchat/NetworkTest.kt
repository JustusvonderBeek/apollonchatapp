package com.cloudsheeptech.anzuchat

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cloudsheeptech.anzuchat.networking.Networking
import com.cloudsheeptech.anzuchat.networking.constants.ContactType
import com.cloudsheeptech.anzuchat.networking.constants.DataType
import com.cloudsheeptech.anzuchat.networking.constants.PacketCategories
import com.cloudsheeptech.anzuchat.networking.packets.Header
import com.cloudsheeptech.anzuchat.networking.packets.Message
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.network.tls.tls
import io.ktor.util.date.getTimeMillis
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.errors.IOException
import io.ktor.utils.io.jvm.javaio.toInputStream
import io.ktor.utils.io.jvm.javaio.toOutputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

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
//        Networking.initialize(netConfig, recChannel)
//        Networking.initialize(netConfig, recChannel)

//        Assert.assertEquals(Networking.remoteAddress, remote)
//        Assert.assertEquals(Networking.connectSecure, secure)
    }

    @Test
    fun testConnectLocalHost() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val job = Job()
        val netScope = CoroutineScope(Dispatchers.Main + job)

        netScope.launch {
            Networking.connect(context)
        }

        runBlocking {
            Thread.sleep(1000)
        }

        netScope.launch {
            Networking.connect(context)
        }

        runBlocking {
            Thread.sleep(1000)
        }
    }

    @Test
    fun testClosingStreamWhileRead() {
        val job = Job()
        val testMethodScope = CoroutineScope(Dispatchers.Main + job)

//        Log.i("NetworkTest", "Whats up?")
        var localSocket : Socket? = null
        val readSema = Semaphore(1, 1)
        val writeSema = Semaphore(1, 1)
        val fixMutex = Mutex(locked = true)
        testMethodScope.launch {
            withContext(Dispatchers.IO) {
                localSocket = aSocket(SelectorManager(Dispatchers.IO)).tcp().connect("10.0.2.2", 50001).tls(Dispatchers.IO)
                writeSema.release()
                var readChannel = localSocket!!.openReadChannel()
                var readStream = readChannel.toInputStream()
                fixMutex.withLock {
                    localSocket!!.close()
                    Log.i("NetworkTest", "Closed socket")
                    Thread.sleep(100) // wait for the socket to be really closed I guess
                }
//                Log.i("NetworkTest", "Attempting read")
                val buf = ByteArray(100)
                try {
                    // Buffers, so should return -1 in case of EOF (aka. closed socket)
                    var read = readStream.read(buf, 0, buf.size)
                    Log.i("NetworkTest", "Buffer: ${buf.toHexString()} - read: $read")
                    read = readStream.read(buf, 0, buf.size)
                    Log.i("NetworkTest", "Buffer: ${buf.toHexString()} - read: $read")
                } catch (ex : IOException) {
                    Log.i("NetworkTest", "$ex")
                }
            }
        }

        testMethodScope.launch {
            var writeChannel : ByteWriteChannel? = null
            writeSema.acquire()
            writeChannel = localSocket!!.openWriteChannel(autoFlush = true)
            val login = Header(PacketCategories.CONTACT.cat.toByte(), ContactType.LOGIN.type.toByte(), 1812181819u, 14441u)
            val packet = login.toByteArray() + "\n".toByteArray(Charsets.UTF_8)
            val header = Header(PacketCategories.DATA.cat.toByte(), DataType.TEXT.type.toByte(), 1812181819u, 14442u)
            val message = Message(1812181819u, getTimeMillis().toString(), "Blablabla")
            val payload = Json.encodeToString(message).toByteArray(Charsets.UTF_8)
            val packet2 = header.toByteArray() + payload + "\n".toByteArray(Charsets.UTF_8)
            val outStream = writeChannel!!.toOutputStream()
            withContext(Dispatchers.IO) {
                outStream.write(packet + packet2)
            }
            Log.i("NetworkTest", "Wrote to endpoint")
            fixMutex.unlock()
        }

        runBlocking {
            Thread.sleep(1000)
        }
    }

    @Test
    fun testTLSMethods() {
        val job = Job()
        val testMethodScope = CoroutineScope(Dispatchers.Main + job)

//        Log.i("NetworkTest", "Whats up?")
        var localSocket : Socket? = null
        val readSema = Semaphore(1, 1)
        val writeSema = Semaphore(1, 1)
        val writeMutex = Mutex(true)
        val readMutex = Mutex(true)
        testMethodScope.launch {
            withContext(Dispatchers.IO) {
                localSocket = aSocket(SelectorManager(Dispatchers.IO)).tcp().connect("10.0.2.2", 50001).tls(Dispatchers.IO)
                readSema.release() // let one thread pass
                writeSema.release()
            }
        }

        testMethodScope.launch {
            var writeChannel : ByteWriteChannel? = null
            writeSema.acquire()
            writeChannel = localSocket!!.openWriteChannel(autoFlush = true)
            val login = Header(PacketCategories.CONTACT.cat.toByte(), ContactType.LOGIN.type.toByte(), 1812181819u, 14441u)
            val packet = login.toByteArray() + "\n".toByteArray(Charsets.UTF_8)
            val header = Header(PacketCategories.DATA.cat.toByte(), DataType.TEXT.type.toByte(), 1812181819u, 14442u)
            val message = Message(1812181819u, getTimeMillis().toString(), "Blablabla")
            val payload = Json.encodeToString(message).toByteArray(Charsets.UTF_8)
            val packet2 = header.toByteArray() + payload + "\n".toByteArray(Charsets.UTF_8)
            var outStream = writeChannel!!.toOutputStream()
            withContext(Dispatchers.IO) {
                outStream.write(packet + packet2)
            }
            Log.i("NetworkTest", "Wrote to endpoint")
            writeMutex.unlock()
            writeSema.acquire()
            writeChannel = localSocket!!.openWriteChannel(autoFlush = true)
            outStream = writeChannel.toOutputStream()
            withContext(Dispatchers.IO) {
                outStream.write(packet + packet2)
            }
        }

        testMethodScope.launch {
            readSema.acquire()
            var readChannel = localSocket!!.openReadChannel()
            Log.i("NetworkTest", "Available sema: ${readSema.availablePermits}")
            var inStream = readChannel!!.toInputStream()
            val inBuffer = ByteArray(100)
            val packetBuf = mutableListOf<Byte>()
            var j = 0
            var read = 0
            while(j < 7) {
                if (j == 1) {
                    readMutex.lock()
                }
                try {
                    read = withContext(Dispatchers.IO) {
                        inStream.read(inBuffer, read, inBuffer.size)
                    }
                    if (read < 0) {
                        Log.i("NetworkTest", "Remote was closed")
                        readSema.acquire()
                        Log.i("NetworkTest", "Reconnected")
                        readChannel = localSocket!!.openReadChannel()
                        inStream = readChannel.toInputStream()
                        read = 0
                        j++
                        continue
                    }
                } catch (ex : Exception) {
                    Log.i("NetworkTest", "Available semas: ${readSema.availablePermits}")
                    Log.i("NetworkTest", "Ex: $ex")
                    readSema.acquire()
                    readChannel = localSocket!!.openReadChannel()
                    inStream = readChannel.toInputStream()
                    read = 0
                    j++
                    continue
                }

                for (i in 0 until read) {
                    if (inBuffer[i] == 0x0a.toByte()) {
                        Log.i("NetworkTest", "Got full packet:\n${packetBuf.toByteArray().toHexString()}")
                        packetBuf.clear()
                    } else {
                        packetBuf.add(inBuffer[i])
                    }
                }
                read = 0
                j++
            }
        }

        runBlocking {
            writeMutex.lock()
            withContext(Dispatchers.IO) {
                localSocket!!.close()
                readMutex.unlock()
            }
            Thread.sleep(500)
            withContext(Dispatchers.IO) {
                localSocket = aSocket(SelectorManager(Dispatchers.IO)).tcp().connect("10.0.2.2", 50001).tls(Dispatchers.IO)
                readSema.release()
                writeSema.release()
            }
            Thread.sleep(500)
        }

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
//                Networking.start(context)
                Assert.fail()
            } catch (ex : IllegalStateException) {
                Log.i("NetworkTest", "Exception correctly thrown")
            }

            val randomData = ByteArray(10)
            val netConfig = Networking.Configuration()
            netConfig.secure = false
            netConfig.remote = "127.0.0.1"
            val recChannel = Channel<ByteArray>(20)
//            Networking.initialize(netConfig, recChannel)
            initBarrier.acquire()
//            Networking.start(context)
//            Networking.write(randomData)
            initBarrier.acquire()
            // Should fail!
//            Networking.write(randomData)

//            Networking.start(context)
            delay(1000L)
//            Networking.start(context)
            delay(500L)
        }
    }

    @Test
    fun testStartNetworkTLS() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        runBlocking {
            // Expecting an exception
            try {
//                Networking.start(context)
                Assert.fail()
            } catch (ex : IllegalStateException) {
                Log.i("NetworkTest", "Exception correctly thrown")
            }

            val netConfig = Networking.Configuration()
            netConfig.secure = true
            netConfig.remote = "10.2.0.2"
            val recChannel = Channel<ByteArray>(20)
//            Networking.initialize(netConfig, recChannel)
//            Networking.start(context)

            delay(500L)
//            Networking.start(context)
            delay(500L)
//            Networking.start(context)
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
//            Networking.initialize(netConfig, recChannel)
//            Networking.start(context)

            val data = ByteArray(10)
//            Networking.write(data)
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
//                Networking.initialize(netConfig, channel)
//                Networking.start(context)
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