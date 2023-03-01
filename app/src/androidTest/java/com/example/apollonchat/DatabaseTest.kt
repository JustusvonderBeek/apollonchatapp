package com.example.apollonchat

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.apollonchat.database.ApollonDatabase
import com.example.apollonchat.database.contact.ContactDatabaseDao
import com.example.apollonchat.database.contact.Contact
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
class DatabaseTest {

    private lateinit var userDatabaseDao: ContactDatabaseDao
    private lateinit var db : ApollonDatabase

    @Before
    fun createDb() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        db = Room.inMemoryDatabaseBuilder(context, ApollonDatabase::class.java).allowMainThreadQueries().build()
        userDatabaseDao = db.contactDatabaseDao
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
        val contact = Contact(id, "username123", "emptyPath.png", emptyList())
        userDatabaseDao.insert(contact)
        val queriedUser = userDatabaseDao.getContact(contact.contactId)
        Assert.assertNotNull(queriedUser)
        if (queriedUser != null) {
            Assert.assertEquals(queriedUser.contactId, id)
            Assert.assertEquals(queriedUser.messages.size, 0)
        }
    }

}