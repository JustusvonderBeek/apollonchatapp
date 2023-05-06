package com.example.apollonchat

import androidx.test.ext.junit.runners.AndroidJUnit4
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
}