package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.crypto.CryptoManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import javax.crypto.AEADBadTagException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Peer Tool", appName)
    }

    @Test
    fun testCryptoManagerInitialization() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val crypto = CryptoManager(context)

        assertNotNull(crypto.identityKeyPair.public)
        assertNotNull(crypto.identityKeyPair.private)
        assertTrue(crypto.getLocalPublicKeyBase64().isNotEmpty())
        assertTrue(crypto.getLocalFingerprintHex().isNotEmpty())
    }

    @Test
    fun testEcdhKeyAgreementAndAes256GcmRoundtrip() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val aliceCrypto = CryptoManager(context)
        val bobCrypto = CryptoManager(context)

        val alicePub = aliceCrypto.getLocalPublicKeyBase64()
        val bobPub = bobCrypto.getLocalPublicKeyBase64()

        // Establish symmetric session via ECDH
        val aliceSafetyNumber = aliceCrypto.establishSession("bob_peer", bobPub)
        val bobSafetyNumber = bobCrypto.establishSession("alice_peer", alicePub)

        // Both parties must compute the exact same 60-digit safety number
        assertEquals(aliceSafetyNumber, bobSafetyNumber)
        assertEquals(12, aliceSafetyNumber.split(" ").size) // 12 blocks of 5 digits
        assertTrue(aliceCrypto.hasSession("bob_peer"))
        assertTrue(bobCrypto.hasSession("alice_peer"))

        // Encrypt with AES-256-GCM
        val originalMessage = "Secret decentralized message via PeerJS WebRTC"
        val (iv, ciphertext) = aliceCrypto.encryptString("bob_peer", originalMessage)

        // Decrypt with AES-256-GCM
        val decryptedMessage = bobCrypto.decryptString("alice_peer", iv, ciphertext)
        assertEquals(originalMessage, decryptedMessage)
    }

    @Test
    fun testAes256GcmTamperingDetection() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val alice = CryptoManager(context)
        val bob = CryptoManager(context)

        alice.establishSession("bob_peer", bob.getLocalPublicKeyBase64())
        bob.establishSession("alice_peer", alice.getLocalPublicKeyBase64())

        val (iv, ciphertext) = alice.encryptString("bob_peer", "Authentic payload")

        // Tamper with ciphertext by corrupting characters
        val tamperedCiphertext = if (ciphertext.startsWith("A")) "B" + ciphertext.substring(1) else "A" + ciphertext.substring(1)

        try {
            bob.decryptString("alice_peer", iv, tamperedCiphertext)
            fail("Expected AEADBadTagException or tampering failure but decryption succeeded!")
        } catch (e: Exception) {
            // Expected tampering detection (AEADBadTagException or BadPaddingException)
            assertTrue(e is AEADBadTagException || e.cause is AEADBadTagException || e is javax.crypto.BadPaddingException)
        }
    }

    @Test
    fun testPeerVerificationPersistence() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val crypto = CryptoManager(context)

        crypto.setPeerVerified("peer_target_123", true)
        assertTrue(crypto.isPeerVerified("peer_target_123"))

        crypto.setPeerVerified("peer_target_123", false)
        assertTrue(!crypto.isPeerVerified("peer_target_123"))
    }
}
