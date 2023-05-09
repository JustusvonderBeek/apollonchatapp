package com.example.apollonchat

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.apollonchat.networking.constants.ContactType
import com.example.apollonchat.networking.constants.PacketCategories
import com.example.apollonchat.networking.packets.ContactInfo
import com.example.apollonchat.networking.packets.ContactList
import com.example.apollonchat.networking.packets.ContactOption
import com.example.apollonchat.networking.packets.Create
import com.example.apollonchat.networking.packets.Header
import com.example.apollonchat.networking.packets.Message
import com.example.apollonchat.networking.packets.MessageAck
import com.example.apollonchat.networking.packets.NetworkContact
import com.example.apollonchat.networking.packets.NetworkOption
import com.example.apollonchat.networking.packets.Search
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NetworkPacketTest {

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
    fun testPacketLength() {
        val jsonString = "{\"Text\":\"Das ist ein einfacher Text\"}"
        val jsonHeader = "{\"Category\":1,\"Type\":2,\"MessageID\":1234567890,\"ContactID\":987654321}\n"
        val jsonPacket = jsonHeader.toByteArray(Charsets.UTF_8) + jsonString.toByteArray(Charsets.UTF_8)
        val category = 1
        val type = 2
        val mId = 1234567890
        val cId = 987654321
        val text = "{\"Text\":\"Das ist ein einfacher Text\"}"
        val header = convertIntToByte(category) + convertIntToByte(type) + convertIntToByteArray(mId) + convertIntToByteArray(cId)
        val packet = header + text.toByteArray(Charsets.UTF_8)

        println("Length of json: ${jsonPacket.size} - Length of raw: ${packet.size}")
        println("Json:\n${jsonPacket.toHexString()}")
        println("Raw:\n${packet.toHexString()}")
        Assert.assertTrue(jsonPacket.size > packet.size)
        println("Json Header Overhead: ${jsonHeader.toByteArray(Charsets.UTF_8).size.toFloat() / jsonString.toByteArray(Charsets.UTF_8).size} = ${jsonHeader.toByteArray(Charsets.UTF_8).size.toFloat() / jsonPacket.size}")
        println("Raw Header Overhead: ${header.size.toFloat() / text.toByteArray(Charsets.UTF_8).size} = ${header.size.toFloat() / packet.size}")

        // Testing with way more data and less header overhead
        val longJsonString = "{\"Text\":\"Das ist ein einfacher Text. Das ist ein einfacher Text. Das ist ein einfacher Text. Das ist ein einfacher Text. Das ist ein einfacher Text. Das ist ein einfacher Text. Das ist ein einfacher Text. Das ist ein einfacher Text. Das ist ein einfacher Text. Das ist ein einfacher Text. Das ist ein einfacher Text. Das ist ein einfacher Text. Das ist ein einfacher Text. Das ist ein einfacher Text. Das ist ein einfacher Text. Das ist ein einfacher Text. Das ist ein einfacher Text. Das ist ein einfacher Text. Das ist ein einfacher Text. Das ist ein einfacher Text. Das ist ein einfacher Text. Das ist ein einfacher Text. Das ist ein einfacher Text. Das ist ein einfacher Text. Das ist ein einfacher Text. Das ist ein einfacher Text. Das ist ein einfacher Text. Das ist ein einfacher Text. Das ist ein einfacher Text. Das ist ein einfacher Text. Das ist ein einfacher Text. Das ist ein einfacher Text. Das ist ein einfacher Text. Das ist ein einfacher Text. Das ist ein einfacher Text. Das ist ein einfacher Text. Das ist ein einfacher Text. Das ist ein einfacher Text. Das ist ein einfacher Text. Das ist ein einfacher Text. Das ist ein einfacher Text. Das ist ein einfacher Text. Das ist ein einfacher Text. Das ist ein einfacher Text. Das ist ein einfacher Text. Das ist ein einfacher Text. Das ist ein einfacher Text. Das ist ein einfacher Text. Das ist ein einfacher Text. Das ist ein einfacher Text. Das ist ein einfacher Text. Das ist ein einfacher Text. Das ist ein einfacher Text. Das ist ein einfacher Text. Das ist ein einfacher Text. Das ist ein einfacher Text. Das ist ein einfacher Text. Das ist ein einfacher Text. Das ist ein einfacher Text. Das ist ein einfacher Text. Das ist ein einfacher Text. Das ist ein einfacher Text. Das ist ein einfacher Text. Das ist ein einfacher Text. Das ist ein einfacher Text. Das ist ein einfacher Text. Das ist ein einfacher Text. Das ist ein einfacher Text. Das ist ein einfacher Text. Das ist ein einfacher Text. Das ist ein einfacher Text. Das ist ein einfacher Text. Das ist ein einfacher Text. Das ist ein einfacher Text. Das ist ein einfacher Text. Das ist ein einfacher Text. Das ist ein einfacher Text. Das ist ein einfacher Text. Das ist ein einfacher Text. Das ist ein einfacher Text. Das ist ein einfacher Text. Das ist ein einfacher Text. Das ist ein einfacher Text. Das ist ein einfacher Text. Das ist ein einfacher Text. Das ist ein einfacher Text. Das ist ein einfacher Text. Das ist ein einfacher Text. Das ist ein einfacher Text.\"}"
        val longJsonPacket = jsonHeader.toByteArray(Charsets.UTF_8) + longJsonString.toByteArray(Charsets.UTF_8)

        val longPacket = header + longJsonString.toByteArray(Charsets.UTF_8)
        println("Long Json: ${longJsonPacket.size} - Long Raw: ${longPacket.size}")
        Assert.assertTrue(longJsonPacket.size > longPacket.size)

        println("Long Json Header Overhead: ${jsonHeader.toByteArray(Charsets.UTF_8).size.toFloat() / longJsonString.toByteArray(Charsets.UTF_8).size} = ${jsonHeader.toByteArray(Charsets.UTF_8).size.toFloat() / longJsonPacket.size}")
        println("Long Raw Header Overhead: ${header.size.toFloat() / longJsonString.toByteArray(Charsets.UTF_8).size} = ${header.size.toFloat() / longPacket.size}")
    }

    @Test
    fun testHeader() {
        val userId = 1234u
        val messageId = 4321u
        val header = Header(PacketCategories.CONTACT.cat.toByte(), ContactType.LOGIN.type.toByte(), userId, messageId)
        Assert.assertEquals(1, header.Category.toInt())
        Assert.assertEquals(5, header.Type.toInt())
        Assert.assertEquals(userId, header.UserId)
        Assert.assertEquals(messageId, header.MessageId)

        val raw = header.toByteArray()
        val expectedRaw = byteArrayOf(0x01, 0x05, 0x00, 0x00, 0x04, 0xD2.toByte(), 0x00, 0x00, 0x10, 0xE1.toByte())

        Assert.assertArrayEquals(expectedRaw, raw)
    }

    @Test
    fun testCreate() {
        val name = "Neuer Benutzer"
        val username = Create(name)
        Assert.assertEquals(name, username.Username)

        val jsonString = Json.encodeToString(username)
        val expectedString = "{\"Username\":\"$name\"}"
        Assert.assertEquals(expectedString, jsonString)
    }

    @Test
    fun testSearchUser() {
        val user = "Neuer Benutzer"
        val search = Search(user)
        Assert.assertEquals(user, search.UserIdentifier)

        val jsonString = Json.encodeToString(search)
        val expectedString = "{\"UserIdentifier\":\"$user\"}"
        Assert.assertEquals(expectedString, jsonString)
    }

    @Test
    fun testContactList() {
        val username = "Benutzer"
        val userId = 1234u
        val contact = NetworkContact(userId, username)
        val contacts = listOf(contact, contact)
        val contactList = ContactList(contacts)

        Assert.assertEquals(contacts.size, contactList.Contacts!!.size)
        for (c in contactList.Contacts!!) {
            Assert.assertEquals(contact.UserId, c.UserId)
            Assert.assertEquals(contact.Username, c.Username)
        }

        val jsonString = Json.encodeToString(contactList)
        val expectedString = "{\"Contacts\":[{\"UserId\":1234,\"Username\":\"Benutzer\"},{\"UserId\":1234,\"Username\":\"Benutzer\"}]}"
        Assert.assertEquals(expectedString, jsonString)
    }

    @Test
    fun testContactOption() {
        val contactId = 1234u
        val option = NetworkOption("Add", "4321")
        val options = listOf(option, option)
        val contactOption = ContactOption(contactId, options)

        Assert.assertEquals(options.size, contactOption.Options.size)
        Assert.assertEquals(contactId, contactOption.ContactUserId)

        for (c in contactOption.Options) {
            Assert.assertEquals(option.Type, c.Type)
            Assert.assertEquals(option.Value, c.Value)
        }

        val jsonString = Json.encodeToString(contactOption)
        val expectedString = "{\"ContactUserId\":1234,\"Options\":[{\"Type\":\"Add\",\"Value\":\"4321\"},{\"Type\":\"Add\",\"Value\":\"4321\"}]}"
        Assert.assertEquals(expectedString, jsonString)
    }

    @Test
    fun testMessage() {
        val text = "Das ist eine neue Nachricht"
        val contactId = 1234u
        val timestamp = "12:33:44+09:00"
        val message = Message(contactId, timestamp, text)

        Assert.assertEquals(contactId, message.ContactUserId)
        Assert.assertEquals(timestamp, message.Timestamp)
        Assert.assertEquals(text, message.Message)

        val jsonString = Json.encodeToString(message)
        val expectedString = "{\"ContactUserId\":1234,\"Timestamp\":\"12:33:44+09:00\",\"Message\":\"Das ist eine neue Nachricht\"}"
        Assert.assertEquals(expectedString, jsonString)
    }

    @Test
    fun testTextAck() {
        val contactId = 1234u
        val timestamp = "12:33:44+09:00"
        val ack = MessageAck(contactId, timestamp)

        Assert.assertEquals(contactId, ack.ContactUserId)
        Assert.assertEquals(timestamp, ack.Timestamp)

        val jsonString = Json.encodeToString(ack)
        val expectedString = "{\"ContactUserId\":1234,\"Timestamp\":\"12:33:44+09:00\"}"
        Assert.assertEquals(expectedString, jsonString)
    }

    @Test
    fun testLogin() {
        // Same as only the header! No need for a test
    }

    @Test
    fun testContactInfo() {
        val contactIds = listOf(1234u, 4321u)
        val image = byteArrayOf(0x01, 0x02, 0x03, 0x04)
        val imageSize = image.size.toUInt()
        val imageFormat = "jpeg"
        val contactInfo = ContactInfo(contactIds, imageSize, image, imageFormat)

        Assert.assertEquals(contactIds, contactInfo.ContactIds)
        Assert.assertEquals(imageSize, contactInfo.ImageBytes)
        Assert.assertEquals(imageFormat, contactInfo.ImageFormat)
        Assert.assertArrayEquals(image, contactInfo.Image)
    }

    @Test
    fun testContactAck() {
        // Again, this is only a header, no test required
    }
}