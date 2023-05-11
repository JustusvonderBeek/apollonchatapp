package com.example.apollonchat

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.apollonchat.database.ApollonDatabase
import com.example.apollonchat.database.contact.ContactDatabaseDao
import com.example.apollonchat.database.contact.Contact
import com.example.apollonchat.database.message.DisplayMessage
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
class DatabaseTest {

    private lateinit var contactDatabaseDao: ContactDatabaseDao
    private lateinit var db : ApollonDatabase

    @Before
    fun createDb() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        db = Room.inMemoryDatabaseBuilder(context, ApollonDatabase::class.java).allowMainThreadQueries().build()
        contactDatabaseDao = db.contactDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun insertAndGetUser() {
        val id = Random.nextLong()
        val contactName = "username123"
        val imagePath = "emptyPath.png"
        val contact = Contact(id, contactName, imagePath, "last Message")
        contactDatabaseDao.insertContact(contact)
        val queriedUser = contactDatabaseDao.getContact(contact.contactId)
        Assert.assertNotNull(queriedUser)
        if (queriedUser != null) {
            Assert.assertEquals(queriedUser.contactId, id)
            Assert.assertEquals(queriedUser.lastMessage, "last Message")
            Assert.assertEquals(queriedUser.contactName, contactName)
            Assert.assertEquals(queriedUser.contactImagePath, imagePath)
        }
    }

    @Test
    @Throws(Exception::class)
    fun testGetAll() {
        val id = Random.nextLong()
        val contactName = "Username"
        val imagePath = "Path"
        val messages = listOf<String>("Test", "test", "a")
        val contactList = mutableListOf<Contact>()
        for (i in 0..10) {
            val contact = Contact(id + i, contactName, imagePath, "last Message")
            contactList.add(contact)
            contactDatabaseDao.insertContact(contact)
        }
        val out = contactDatabaseDao.getAllContacts()
//        out.observe(this, Observer {res ->
//            res?.let {
//                Assert.assertNotNull(res)
//                Assert.assertNotEquals(res.size, 0)
//            }
//        })
    }

    @Test
    fun testMessageDatabase() {
        val dMessage = DisplayMessage(12345, 54321, false, "Message", "Now")

        val messageDao = db.messageDao()
        messageDao.insertMessage(dMessage)
        var messages = messageDao.getMessages(54321)

        Assert.assertNotNull(messages)
        Assert.assertEquals(1, messages!!.size)
        messageDao.clearMessages()
        messages = messageDao.getMessages(54321)
        Assert.assertNotNull(messages)
        Assert.assertEquals(0, messages!!.size)
    }

}