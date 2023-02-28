package com.example.apollonchat

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.apollonchat.database.ChatUserDatabase
import com.example.apollonchat.database.ChatUserDatabaseDao
import com.example.apollonchat.database.User
import org.hamcrest.MatcherAssert
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
class DatabaseTest {

    private lateinit var userDatabaseDao: ChatUserDatabaseDao
    private lateinit var db : ChatUserDatabase

    @Before
    fun createDb() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        db = Room.inMemoryDatabaseBuilder(context, ChatUserDatabase::class.java).allowMainThreadQueries().build()
        userDatabaseDao = db.chatUserDatabaseDao
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
        val user = User(id, "username123", "emptyPath.png", emptyList())
        userDatabaseDao.insert(user)
        val queriedUser = userDatabaseDao.getUser(user.userId)
        Assert.assertNotNull(queriedUser)
        if (queriedUser != null) {
            Assert.assertEquals(queriedUser.userId, id)
            Assert.assertEquals(queriedUser.messages.size, 0)
        }
    }

}