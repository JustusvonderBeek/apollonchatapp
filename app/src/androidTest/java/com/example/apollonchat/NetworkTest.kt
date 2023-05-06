package com.example.apollonchat

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.apollonchat.database.ApollonDatabase
import com.example.apollonchat.database.contact.Contact
import com.example.apollonchat.networking.Networking
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
        val remote = InetAddress.getByName("10.2.0.2")
        val secure = false
        netConfig.remote = remote
        netConfig.secure = secure
        Networking.initialize(netConfig)

//        Assert.assertEquals(Networking.remoteAddress, remote)
//        Assert.assertEquals(Networking.connectSecure, secure)
    }

    @Test
    fun testStartNetwork() {
        val netConfig = Networking.Configuration()
        Networking.initialize(netConfig)

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val job = Job()
        val testScope = CoroutineScope(Dispatchers.Main + job)
        testScope.launch {
            Networking.start(context)
        }
        runBlocking {
            delay(1000L)
        }
        job.cancel()
    }

    @Test
    @Throws(Exception::class)
    fun testSimpleConnection() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        val netConfig = Networking.Configuration()
        Networking.initialize(netConfig)
        val uijob = Job()
        val scope = CoroutineScope(Dispatchers.Main + uijob)
        scope.launch {
            Networking.start(context)
        }
        uijob.complete()
    }

}