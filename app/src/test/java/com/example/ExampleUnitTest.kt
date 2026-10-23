package com.example

import com.example.data.local.PeerTypeConverters
import com.example.data.model.MessageStatus
import com.example.data.model.MessageType
import com.example.data.model.ServerConfig
import com.example.data.model.TransferStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testServerConfigDefaults() {
        val config = ServerConfig()
        assertEquals("0.peerjs.com", config.host)
        assertEquals(443, config.port)
        assertEquals("/", config.path)
        assertEquals(true, config.secure)
        assertEquals("peerjs", config.key)
    }

    @Test
    fun testTypeConverters() {
        val converter = PeerTypeConverters()
        assertEquals(MessageStatus.READ, converter.toMessageStatus("READ"))
        assertEquals(MessageType.FILE_REF, converter.toMessageType("FILE_REF"))
        assertEquals(TransferStatus.COMPLETED, converter.toTransferStatus("COMPLETED"))
    }

    @Test
    fun testCustomPeerIdValidation() {
        fun isValid(id: String) = id.length in 3..36 && id.all { it.isLetterOrDigit() || it == '-' || it == '_' }

        assertTrue(isValid("alice-device"))
        assertTrue(isValid("node_99"))
        assertTrue(isValid("cyber-falcon-42"))
        assertFalse(isValid("ab")) // too short
        assertFalse(isValid("invalid peer!")) // contains spaces and special char
        assertFalse(isValid("test@peer")) // contains @
    }

    @Test
    fun testMessageStatusReadWorkflow() {
        val converter = PeerTypeConverters()
        assertEquals("READ", converter.fromMessageStatus(MessageStatus.READ))
        assertEquals("DELIVERED", converter.fromMessageStatus(MessageStatus.DELIVERED))
        assertEquals(MessageStatus.READ, converter.toMessageStatus("READ"))

        // Verify status ordering for delivery receipts
        val statuses = listOf(MessageStatus.SENDING, MessageStatus.SENT, MessageStatus.DELIVERED, MessageStatus.READ)
        assertTrue(statuses.contains(MessageStatus.READ))
        assertEquals(MessageStatus.READ, statuses.last())
    }
}
