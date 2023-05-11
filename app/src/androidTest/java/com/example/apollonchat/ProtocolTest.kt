package com.example.apollonchat

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.apollonchat.database.ApollonDatabase
import com.example.apollonchat.networking.ApollonProtocolHandler.ApollonProtocolHandler
import com.example.apollonchat.networking.constants.DataType
import com.example.apollonchat.networking.constants.PacketCategories
import com.example.apollonchat.networking.packets.Header
import com.example.apollonchat.networking.packets.Message
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.util.stream.Stream

@RunWith(AndroidJUnit4::class)
class ProtocolTest {

    private lateinit var db : ApollonDatabase

    @Before
    fun initDatabase() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        db = Room.inMemoryDatabaseBuilder(context, ApollonDatabase::class.java).allowMainThreadQueries().build()

    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun testReceiveMessage() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val messageDao = ApollonDatabase.getInstance(context).messageDao()
        messageDao.clearMessages()

        val userId = 1234u
        ApollonProtocolHandler.initialize(userId, context.applicationContext)

        val contactId = 38585298u
        val messageId = 23768943u
        val timestamp = "12:44:00+09:00"
        val text = "Testing the protocol handler for correctness"
        val header = Header(PacketCategories.DATA.cat.toByte(), DataType.TEXT.type.toByte(), contactId, messageId)
        val message = Message(userId, timestamp, text)

        val rawHeader = header.toByteArray()
        val stringMessage = Json.encodeToString(message) + "\n"

        val tmpFile = File(context.filesDir, "protocolTest.txt")
        // Writing the data into the file so that the method can work with it
        val outStream = tmpFile.outputStream()
        outStream.write(stringMessage.toByteArray(Charsets.UTF_8))
        outStream.flush()

        ApollonProtocolHandler.ReceiveAny(rawHeader, tmpFile.inputStream())

        Thread.sleep(1000)
        val messages = messageDao.getMessages(contactId.toLong())

        Assert.assertNotNull(messages)
        Assert.assertEquals(1, messages!!.size)
        Assert.assertEquals(text, messages[0].content)

        outStream.close()
        tmpFile.delete()
    }

}