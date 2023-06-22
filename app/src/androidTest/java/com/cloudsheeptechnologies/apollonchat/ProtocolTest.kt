package com.cloudsheeptechnologies.apollonchat

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cloudsheeptechnologies.apollonchat.database.ApollonDatabase
import com.cloudsheeptechnologies.apollonchat.networking.ApollonProtocolHandler.ApollonProtocolHandler
import com.cloudsheeptechnologies.apollonchat.networking.constants.DataType
import com.cloudsheeptechnologies.apollonchat.networking.constants.PacketCategories
import com.cloudsheeptechnologies.apollonchat.networking.packets.Header
import com.cloudsheeptechnologies.apollonchat.networking.packets.Message
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.IOException
import kotlin.random.Random
import kotlin.random.nextUInt

@RunWith(AndroidJUnit4::class)
class ProtocolTest {

    private val userId = 12345u
    private var messageId = 23768943u
    private var contactId = 38585298u
    private lateinit var db : ApollonDatabase
    private lateinit var random : Random

    @Before
    fun initDatabase() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        db = Room.inMemoryDatabaseBuilder(context, ApollonDatabase::class.java).allowMainThreadQueries().build()

        random = Random.Default
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    private fun createTextMessage(randomContact : Boolean = false, randomMessageId : Boolean = false, timestamp : String = "12:44:00+09:00", text : String = "Testing the protocol handler for correctness") : Pair<Header, Message> {
        var localContactId = contactId
        var localMessageId = messageId++
        if (randomContact) {
            localContactId = random.nextUInt()
        }
        if (randomMessageId) {
            localMessageId = random.nextUInt()
        }
        val header = Header(PacketCategories.DATA.cat.toByte(), DataType.TEXT.type.toByte(), localContactId, localMessageId)
        val message = Message(userId, timestamp, text)
        return Pair(header, message)
    }

    private fun convertTextToRaw(text : Pair<Header, Message>) : Pair<ByteArray, ByteArray> {
        val header = text.first
        val message = text.second
        val rawHeader = header.toByteArray()
        val stringMessage = Json.encodeToString(message) + "\n"
        val rawMessage = stringMessage.toByteArray(Charsets.UTF_8)
        return Pair(rawHeader, rawMessage)
    }

    private fun increaseMessageId(header : ByteArray, increase : Int) : ByteArray {
        val infoHeader = Header.convertRawToHeader(header)
        infoHeader!!.MessageId += increase.toUInt()
        return infoHeader!!.toByteArray()
    }

    @Test
    fun testReceiveMessage() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val messageDao = ApollonDatabase.getInstance(context).messageDao()
        messageDao.clearMessages()

        ApollonProtocolHandler.initialize(userId, context.applicationContext)

        var text = "Testing the procotol handling of chat protocol"
        var pair = createTextMessage(text = text)
        var raw = convertTextToRaw(pair)
        var rawHeader = raw.first
        var stringMessage = raw.second

        var tmpFile = File(context.filesDir, "protocolTest.txt")
        tmpFile.deleteOnExit()
        // Writing the data into the file so that the method can work with it
        var outStream = tmpFile.outputStream()
        outStream.write(stringMessage)
        outStream.flush()

        ApollonProtocolHandler.receiveAny(rawHeader)

        // Wait for database to finish writing data into it (100ms should be enough for most)
        Thread.sleep(100)
        var messages = messageDao.getMessages(contactId.toLong())

        Assert.assertNotNull(messages)
        Assert.assertEquals(1, messages!!.size)
        Assert.assertEquals(text, messages[0].content)
        Assert.assertEquals(1, messages[0].messageId)

        // Prepare for next run
        outStream.close()
        tmpFile = File(context.filesDir, "protocolTest.txt")
        tmpFile.deleteOnExit()
        outStream = tmpFile.outputStream()

        text = "Testing the protocol a second time"
        pair = createTextMessage(text = text)
        raw = convertTextToRaw(pair)
        stringMessage = raw.second
        outStream.write(stringMessage)
        outStream.flush()
        rawHeader = increaseMessageId(rawHeader, 1)

        ApollonProtocolHandler.receiveAny(rawHeader)

        Thread.sleep(100)

        messages = messageDao.getMessages(contactId.toLong())

        Assert.assertNotNull(messages)
        Assert.assertEquals(2, messages!!.size)
//        println("Messages: ${messages[0]} & ${messages[1]}")
        Assert.assertEquals(text, messages[1].content)
        Assert.assertEquals(2, messages[1].messageId)

        outStream.close()
    }

    @Test
    fun testWrongTextInput() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val messageDao = ApollonDatabase.getInstance(context).messageDao()
        messageDao.clearMessages()

        ApollonProtocolHandler.initialize(userId, context.applicationContext)

        var pair = createTextMessage(text = "empty")
        var raw = convertTextToRaw(pair)
        var rawHeader = raw.first
        val textMessageJson = "{\"contactuserid\":0,\"timestamp\":\"12:00\",\"message\":\"test\"}\n"
        var rawText = textMessageJson.toByteArray(Charsets.UTF_8)

        var tmpFile = File(context.filesDir, "protocolTest.txt")
        tmpFile.deleteOnExit()
        // Writing the data into the file so that the method can work with it
        var outStream = tmpFile.outputStream()
        outStream.write(rawText)
        outStream.flush()

        ApollonProtocolHandler.receiveAny(rawHeader)

        // Wait for database to finish writing data into it (100ms should be enough for most)
        Thread.sleep(100)
        var messages = messageDao.getMessages(0L)

        Assert.assertNotNull(messages)
        Assert.assertEquals(0, messages!!.size)
    }

    @Test
    fun testTextTimeout() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val messageDao = ApollonDatabase.getInstance(context).messageDao()
        messageDao.clearMessages()

        ApollonProtocolHandler.initialize(userId, context.applicationContext)

        var pair = createTextMessage(text = "empty")
        var raw = convertTextToRaw(pair)
        var rawHeader = raw.first
        val timeoutJsonText = "{\"ContactUserId\":0,\"Timestamp\":\"12:00\",\"Message\":\"test\""
        val rawWrong = timeoutJsonText.toByteArray(Charsets.UTF_8)

        var tmpFile = File(context.filesDir, "protocolTest.txt")
        tmpFile.deleteOnExit()
        // Writing the data into the file so that the method can work with it
        var outStream = tmpFile.outputStream()
        outStream.write(rawWrong)
        outStream.flush()

        // TODO: Fix this not timeouting
        ApollonProtocolHandler.receiveAny(rawHeader)

        Thread.sleep(1000)
        val messages = messageDao.getMessages(0L)

        Assert.assertNotNull(messages)
        Assert.assertEquals(0, messages!!.size)

        // TODO: Fix reading in all text!
        Assert.fail()
    }

    @Test
    fun testSendingMessage() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val messageDao = ApollonDatabase.getInstance(context).messageDao()
        messageDao.clearMessages()

        ApollonProtocolHandler.initialize(userId, context.applicationContext)

        ApollonProtocolHandler.sendText("test", 12345u)

        Thread.sleep(100)
    }

}